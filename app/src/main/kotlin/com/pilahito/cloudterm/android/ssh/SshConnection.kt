package com.pilahito.cloudterm.android.ssh

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpProgressMonitor
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import com.pilahito.cloudterm.android.data.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

data class RemoteFile(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
)

fun joinPath(dir: String, name: String): String =
    if (dir.endsWith("/")) dir + name else "$dir/$name"

fun parentPath(path: String): String {
    val t = path.trimEnd('/')
    val i = t.lastIndexOf('/')
    return if (i <= 0) "/" else t.substring(0, i)
}

/**
 * Una conexión SSH con un canal de shell (terminal) y un canal SFTP (archivos).
 * La clave del servidor se verifica contra un known_hosts propio: la primera vez
 * se pregunta al usuario y, si después cambia, la conexión se rechaza.
 *
 * Importante: muchos OpenSSH desactivan `password` y solo aceptan
 * `keyboard-interactive`. Sin UIKeyboardInteractive JSch falla con Auth fail
 * aunque la contraseña sea correcta. SFTP puede funcionar en el mismo servidor
 * si el hosting expone un daemon distinto (o FTPS en 21/990), de ahí la confusión.
 */
class SshConnection(
    private val host: Host,
    private val password: String?,
    private val privateKey: String?,
    private val passphrase: String?,
    private val knownHosts: File,
    private val confirmHostKey: suspend (String) -> Boolean,
) {
    private var session: Session? = null
    private var shell: Channel? = null
    private var shellOut: OutputStream? = null
    private var sftp: ChannelSftp? = null
    private val sftpLock = Mutex()
    private val writer = Executors.newSingleThreadExecutor()

    @Volatile
    var outputSink: ((ByteArray) -> Unit)? = null

    @Volatile
    var onShellClosed: (() -> Unit)? = null

    suspend fun connect() = withContext(Dispatchers.IO) {
        if (host.port in FTPS_PORTS) {
            throw JSchException(
                "El puerto ${host.port} es de FTP/FTPS, no de SSH. " +
                    "SSH usa el puerto 22 (a veces 2222). FTPS y SSH no son el mismo protocolo: " +
                    "si tu hosting solo da FTPS, el terminal no puede existir.",
            )
        }

        val jsch = JSch()
        if (!knownHosts.exists()) knownHosts.createNewFile()
        jsch.setKnownHosts(knownHosts.absolutePath)

        if (!privateKey.isNullOrBlank()) {
            val pass = passphrase?.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
            jsch.addIdentity("cloudterm", privateKey.toByteArray(Charsets.UTF_8), null, pass)
        }

        val s = jsch.getSession(host.username, host.hostname, host.port)
        if (!password.isNullOrEmpty()) s.setPassword(password)
        s.setConfig("StrictHostKeyChecking", "ask")
        s.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
        s.setConfig("MaxAuthTries", "5")
        s.timeout = 20_000
        s.setUserInfo(InteractiveUserInfo(password, passphrase, confirmHostKey))
        s.setServerAliveInterval(15_000)
        s.setServerAliveCountMax(4)
        try {
            s.connect(20_000)
        } catch (e: JSchException) {
            throw remapConnectError(e)
        }
        session = s
    }

    /* ------------------------------ Terminal ------------------------------ */

    /** Abre el shell una sola vez; si el servidor no da canal `shell`, prueba `exec`. */
    fun openShell(cols: Int, rows: Int) {
        if (shell != null) return
        val s = session ?: throw IllegalStateException("sesión SSH no conectada")

        val input: InputStream
        val ch: Channel = try {
            openPtyShell(s, cols, rows)
        } catch (first: Exception) {
            try {
                openPtyExec(s, cols, rows)
            } catch (second: Exception) {
                throw JSchException(
                    "El servidor aceptó SSH/SFTP pero no un terminal interactivo " +
                        "(canal shell/exec denegado). Muchos hostings solo permiten SFTP. " +
                        "shell=${first.message}; exec=${second.message}",
                )
            }
        }
        input = ch.inputStream
        shellOut = ch.outputStream
        shell = ch

        Thread {
            val buf = ByteArray(8192)
            try {
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    if (n > 0) outputSink?.invoke(buf.copyOf(n))
                }
            } catch (_: IOException) {
            }
            onShellClosed?.invoke()
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun openPtyShell(s: Session, cols: Int, rows: Int): ChannelShell {
        val ch = s.openChannel("shell") as ChannelShell
        ch.setPtyType("xterm-256color")
        ch.setPtySize(cols, rows, 0, 0)
        ch.setEnv("LANG", "en_US.UTF-8")
        ch.setEnv("TERM", "xterm-256color")
        ch.connect(12_000)
        if (!ch.isConnected) throw JSchException("canal shell no conectado")
        return ch
    }

    private fun openPtyExec(s: Session, cols: Int, rows: Int): ChannelExec {
        val ch = s.openChannel("exec") as ChannelExec
        ch.setPty(true)
        ch.setPtyType("xterm-256color", cols, rows, 0, 0)
        ch.setEnv("LANG", "en_US.UTF-8")
        ch.setEnv("TERM", "xterm-256color")
        ch.setCommand("exec bash -l || exec sh -l || exec /bin/sh")
        ch.connect(12_000)
        if (!ch.isConnected) throw JSchException("canal exec no conectado")
        return ch
    }

    fun write(data: ByteArray) {
        writer.execute {
            try {
                shellOut?.write(data)
                shellOut?.flush()
            } catch (_: IOException) {
            }
        }
    }

    fun resize(cols: Int, rows: Int) {
        writer.execute {
            try {
                when (val ch = shell) {
                    is ChannelShell -> ch.setPtySize(cols, rows, 0, 0)
                    is ChannelExec -> ch.setPtySize(cols, rows, 0, 0)
                }
            } catch (_: Exception) {
            }
        }
    }

    /* -------------------------------- SFTP -------------------------------- */

    private fun channel(): ChannelSftp {
        val existing = sftp
        if (existing != null && existing.isConnected) return existing
        val ch = session!!.openChannel("sftp") as ChannelSftp
        ch.connect(10_000)
        sftp = ch
        return ch
    }

    suspend fun home(): String = sftpLock.withLock {
        withContext(Dispatchers.IO) { channel().pwd() }
    }

    suspend fun list(dir: String): List<RemoteFile> = sftpLock.withLock {
        withContext(Dispatchers.IO) {
            val ch = channel()
            ch.ls(dir)
                .filterIsInstance<ChannelSftp.LsEntry>()
                .filter { it.getFilename() != "." && it.getFilename() != ".." }
                .map { e ->
                    val a = e.getAttrs()
                    val p = joinPath(dir, e.getFilename())
                    var isDir = a.isDir()
                    if (!isDir && a.isLink()) {
                        isDir = try {
                            ch.stat(p).isDir()
                        } catch (_: Exception) {
                            false
                        }
                    }
                    RemoteFile(e.getFilename(), p, isDir, a.getSize(), a.getMTime().toLong() * 1000L)
                }
                .sortedWith(compareByDescending<RemoteFile> { it.isDir }.thenBy { it.name.lowercase() })
        }
    }

    suspend fun download(remote: String, out: OutputStream, onBytes: (Long) -> Unit) =
        sftpLock.withLock {
            withContext(Dispatchers.IO) {
                var total = 0L
                channel().get(remote, out, object : SftpProgressMonitor {
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        total += count
                        onBytes(total)
                        return true
                    }

                    override fun end() {}
                })
            }
        }

    suspend fun upload(input: InputStream, remote: String, onBytes: (Long) -> Unit) =
        sftpLock.withLock {
            withContext(Dispatchers.IO) {
                var total = 0L
                channel().put(input, remote, object : SftpProgressMonitor {
                    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                    override fun count(count: Long): Boolean {
                        total += count
                        onBytes(total)
                        return true
                    }

                    override fun end() {}
                }, ChannelSftp.OVERWRITE)
            }
        }

    suspend fun mkdir(path: String) = sftpLock.withLock {
        withContext(Dispatchers.IO) { channel().mkdir(path) }
    }

    suspend fun rename(from: String, to: String) = sftpLock.withLock {
        withContext(Dispatchers.IO) { channel().rename(from, to) }
    }

    suspend fun delete(file: RemoteFile) = sftpLock.withLock {
        withContext(Dispatchers.IO) { deleteRecursive(channel(), file.path, file.isDir) }
    }

    private fun deleteRecursive(ch: ChannelSftp, path: String, isDir: Boolean) {
        if (isDir) {
            for (e in ch.ls(path).filterIsInstance<ChannelSftp.LsEntry>()) {
                val n = e.getFilename()
                if (n == "." || n == "..") continue
                deleteRecursive(ch, joinPath(path, n), e.getAttrs().isDir())
            }
            ch.rmdir(path)
        } else {
            ch.rm(path)
        }
    }

    fun close() {
        Thread {
            runCatching { shell?.disconnect() }
            runCatching { sftp?.disconnect() }
            runCatching { session?.disconnect() }
            writer.shutdown()
        }.start()
    }

    private fun remapConnectError(e: JSchException): JSchException {
        val msg = e.message.orEmpty()
        val lower = msg.lowercase()
        if (lower.contains("auth fail") || lower.contains("auth cancel")) {
            return JSchException(
                "Auth fail: usuario, contraseña o clave incorrectos, o el servidor " +
                    "exige un método que no enviamos. Prueba clave ed25519 o revisa que SSH " +
                    "esté abierto en el puerto ${host.port} (no confundir con FTPS).",
                e,
            )
        }
        if (lower.contains("connection refused") || lower.contains("econnrefused")) {
            return JSchException(
                "Conexión rechazada en ${host.hostname}:${host.port}. " +
                    "Ese puerto no habla SSH. FTPS suele ser 21 o 990; SSH es 22.",
                e,
            )
        }
        if (lower.contains("invalid version") || lower.contains("invalid identification") ||
            lower.contains("protocol error") || lower.contains("session.connect")
        ) {
            return JSchException(
                "El servidor en ${host.hostname}:${host.port} no habla SSH " +
                    "(identificación inválida). Si FTPS te funciona ahí, es otro protocolo; " +
                    "cambia al puerto 22 o activa SSH en el panel del hosting.",
                e,
            )
        }
        return e
    }

    companion object {
        private val FTPS_PORTS = setOf(21, 989, 990)
    }
}

/**
 * UserInfo + keyboard-interactive. OpenSSH moderno suele ofrecer
 * keyboard-interactive y no password; JSch necesita ambas interfaces.
 */
private class InteractiveUserInfo(
    private val password: String?,
    private val passphrase: String?,
    private val confirmHostKey: suspend (String) -> Boolean,
) : UserInfo, UIKeyboardInteractive {
    override fun getPassphrase(): String? = passphrase
    override fun getPassword(): String? = password
    override fun promptPassword(message: String?): Boolean = !password.isNullOrEmpty()
    override fun promptPassphrase(message: String?): Boolean = !passphrase.isNullOrEmpty()
    override fun promptYesNo(message: String?): Boolean =
        runBlocking { confirmHostKey(message.orEmpty()) }

    override fun showMessage(message: String?) {}

    override fun promptKeyboardInteractive(
        destination: String?,
        name: String?,
        instruction: String?,
        prompt: Array<out String>?,
        echo: BooleanArray?,
    ): Array<String>? {
        val pw = password ?: return null
        if (prompt.isNullOrEmpty()) return emptyArray()
        return Array(prompt.size) { pw }
    }
}
