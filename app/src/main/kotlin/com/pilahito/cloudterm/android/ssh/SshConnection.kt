package com.pilahito.cloudterm.android.ssh

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpProgressMonitor
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
    private var shell: ChannelShell? = null
    private var shellOut: OutputStream? = null
    private var sftp: ChannelSftp? = null
    private val sftpLock = Mutex()
    private val writer = Executors.newSingleThreadExecutor()

    @Volatile
    var outputSink: ((ByteArray) -> Unit)? = null

    @Volatile
    var onShellClosed: (() -> Unit)? = null

    suspend fun connect() = withContext(Dispatchers.IO) {
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
        s.timeout = 20_000
        s.setUserInfo(object : UserInfo {
            override fun getPassphrase(): String? = passphrase
            override fun getPassword(): String? = password
            override fun promptPassword(message: String?): Boolean = !password.isNullOrEmpty()
            override fun promptPassphrase(message: String?): Boolean = !passphrase.isNullOrEmpty()
            override fun promptYesNo(message: String?): Boolean =
                runBlocking { confirmHostKey(message.orEmpty()) }

            override fun showMessage(message: String?) {}
        })
        s.setServerAliveInterval(30_000)
        s.connect(15_000)
        session = s
    }

    /* ------------------------------ Terminal ------------------------------ */

    /** Abre el shell una sola vez; llamadas posteriores no hacen nada. */
    fun openShell(cols: Int, rows: Int) {
        if (shell != null) return
        val s = session ?: return
        val ch = s.openChannel("shell") as ChannelShell
        ch.setPtyType("xterm-256color")
        ch.setPtySize(cols, rows, 0, 0)
        val input = ch.inputStream
        shellOut = ch.outputStream
        ch.connect(10_000)
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
                shell?.setPtySize(cols, rows, 0, 0)
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
}
