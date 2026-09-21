package com.pilahito.cloudterm.android.ftp

import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.data.Protocol
import com.pilahito.cloudterm.android.net.RemoteFs
import com.pilahito.cloudterm.android.ssh.RemoteFile
import com.pilahito.cloudterm.android.ssh.joinPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * FTP (21, texto plano) y FTPS (990 implícito o 21 explícito).
 * No hay terminal: FTP no transporta un shell.
 */
class FtpConnection(
    private val host: Host,
    private val password: String?,
) : RemoteFs {
    private val lock = Mutex()
    private var client: FTPClient? = null

    override val hasTerminal: Boolean = false

    @Volatile
    override var outputSink: ((ByteArray) -> Unit)? = null

    @Volatile
    override var onShellClosed: (() -> Unit)? = null

    override suspend fun connect() = withContext(Dispatchers.IO) {
        val implicit = host.protocol == Protocol.FTPS && host.port != 21
        val c: FTPClient = if (host.protocol == Protocol.FTPS) {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf(TrustAll), SecureRandom())
            FTPSClient(implicit).apply {
                setTrustManager(TrustAll)
                enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            }
        } else {
            FTPClient()
        }
        c.connectTimeout = 20_000
        c.defaultTimeout = 20_000
        c.setDataTimeout(Duration.ofMillis(30_000))
        c.controlEncoding = "UTF-8"
        c.connect(host.hostname, host.port)
        val hello = c.replyCode
        if (!FTPReply.isPositiveCompletion(hello)) {
            c.disconnect()
            throw IllegalStateException("FTP rechazó la conexión (${c.replyString})")
        }
        val ok = c.login(host.username, password.orEmpty())
        if (!ok) {
            val msg = c.replyString?.trim().orEmpty()
            c.disconnect()
            throw IllegalStateException("Login FTP fallido: $msg")
        }
        c.setFileType(FTP.BINARY_FILE_TYPE)
        c.enterLocalPassiveMode()
        if (c is FTPSClient) {
            runCatching { c.execPBSZ(0) }
            runCatching { c.execPROT("P") }
        }
        client = c
    }

    override fun openShell(cols: Int, rows: Int) {
        val text = "\r\nFTP/FTPS no tiene terminal.\r\nUsa la pestaña Archivos o Código.\r\n"
        outputSink?.invoke(text.toByteArray(Charsets.UTF_8))
        onShellClosed?.invoke()
    }

    override fun write(data: ByteArray) {}
    override fun resize(cols: Int, rows: Int) {}

    private fun live(): FTPClient =
        client ?: throw IllegalStateException("FTP no conectado")

    override suspend fun home(): String = lock.withLock {
        withContext(Dispatchers.IO) {
            val pwd = live().printWorkingDirectory()
            if (pwd.isNullOrBlank()) "/" else pwd
        }
    }

    override suspend fun list(dir: String): List<RemoteFile> = lock.withLock {
        withContext(Dispatchers.IO) {
            val c = live()
            c.changeWorkingDirectory(dir)
            val files = c.listFiles() ?: emptyArray()
            files.filter { it.name != "." && it.name != ".." }
                .map { f ->
                    val modified = f.timestamp?.time?.time ?: 0L
                    RemoteFile(
                        name = f.name,
                        path = joinPath(dir, f.name),
                        isDir = f.isDirectory,
                        size = f.size,
                        modified = modified,
                    )
                }
                .sortedWith(compareByDescending<RemoteFile> { it.isDir }.thenBy { it.name.lowercase() })
        }
    }

    override suspend fun download(remote: String, out: OutputStream, onBytes: (Long) -> Unit) =
        lock.withLock {
            withContext(Dispatchers.IO) {
                val wrapped = ProgressOutputStream(out, onBytes)
                val ok = live().retrieveFile(remote, wrapped)
                if (!ok) throw IllegalStateException("No se pudo descargar: ${live().replyString}")
            }
        }

    override suspend fun upload(input: InputStream, remote: String, onBytes: (Long) -> Unit) =
        lock.withLock {
            withContext(Dispatchers.IO) {
                val wrapped = ProgressInputStream(input, onBytes)
                val ok = live().storeFile(remote, wrapped)
                if (!ok) throw IllegalStateException("No se pudo subir: ${live().replyString}")
            }
        }

    override suspend fun mkdir(path: String) = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!live().makeDirectory(path)) {
                throw IllegalStateException("No se pudo crear carpeta: ${live().replyString}")
            }
        }
    }

    override suspend fun rename(from: String, to: String) = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!live().rename(from, to)) {
                throw IllegalStateException("No se pudo renombrar: ${live().replyString}")
            }
        }
    }

    override suspend fun delete(file: RemoteFile) = lock.withLock {
        withContext(Dispatchers.IO) { deleteRecursive(live(), file.path, file.isDir) }
    }

    private fun deleteRecursive(c: FTPClient, path: String, isDir: Boolean) {
        if (isDir) {
            val files = c.listFiles(path) ?: emptyArray()
            for (f in files) {
                if (f.name == "." || f.name == "..") continue
                deleteRecursive(c, joinPath(path, f.name), f.isDirectory)
            }
            if (!c.removeDirectory(path)) {
                throw IllegalStateException("No se pudo borrar carpeta: ${c.replyString}")
            }
        } else {
            if (!c.deleteFile(path)) {
                throw IllegalStateException("No se pudo borrar: ${c.replyString}")
            }
        }
    }

    override suspend fun readText(path: String, maxBytes: Long): String = lock.withLock {
        withContext(Dispatchers.IO) {
            val buf = ByteArrayOutputStream()
            val ok = live().retrieveFile(path, object : OutputStream() {
                override fun write(b: Int) {
                    if (buf.size() >= maxBytes) throw IllegalStateException("El archivo supera ${maxBytes} bytes")
                    buf.write(b)
                }
                override fun write(b: ByteArray, off: Int, len: Int) {
                    if (buf.size() + len > maxBytes) throw IllegalStateException("El archivo supera ${maxBytes} bytes")
                    buf.write(b, off, len)
                }
            })
            if (!ok) throw IllegalStateException("No se pudo leer: ${live().replyString}")
            buf.toString(Charsets.UTF_8.name())
        }
    }

    override suspend fun writeText(path: String, text: String) = lock.withLock {
        withContext(Dispatchers.IO) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val ok = live().storeFile(path, ByteArrayInputStream(bytes))
            if (!ok) throw IllegalStateException("No se pudo guardar: ${live().replyString}")
        }
    }

    override fun close() {
        Thread {
            runCatching { client?.logout() }
            runCatching { client?.disconnect() }
            client = null
        }.start()
    }

    private object TrustAll : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}

private class ProgressOutputStream(
    private val inner: OutputStream,
    private val onBytes: (Long) -> Unit,
) : OutputStream() {
    private var total = 0L
    override fun write(b: Int) {
        inner.write(b)
        total += 1
        onBytes(total)
    }
    override fun write(b: ByteArray, off: Int, len: Int) {
        inner.write(b, off, len)
        total += len
        onBytes(total)
    }
    override fun flush() = inner.flush()
    override fun close() = inner.close()
}

private class ProgressInputStream(
    private val inner: InputStream,
    private val onBytes: (Long) -> Unit,
) : InputStream() {
    private var total = 0L
    override fun read(): Int {
        val n = inner.read()
        if (n >= 0) {
            total += 1
            onBytes(total)
        }
        return n
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = inner.read(b, off, len)
        if (n > 0) {
            total += n
            onBytes(total)
        }
        return n
    }
    override fun close() = inner.close()
}
