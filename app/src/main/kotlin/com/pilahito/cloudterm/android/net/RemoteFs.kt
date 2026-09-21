package com.pilahito.cloudterm.android.net

import com.pilahito.cloudterm.android.ssh.RemoteFile
import java.io.InputStream
import java.io.OutputStream

/** Capa común de archivos (SFTP, FTP, FTPS) y, si existe, terminal SSH. */
interface RemoteFs {
    val hasTerminal: Boolean

    var outputSink: ((ByteArray) -> Unit)?
    var onShellClosed: (() -> Unit)?

    suspend fun connect()
    fun openShell(cols: Int, rows: Int)
    fun write(data: ByteArray)
    fun resize(cols: Int, rows: Int)

    suspend fun home(): String
    suspend fun list(dir: String): List<RemoteFile>
    suspend fun download(remote: String, out: OutputStream, onBytes: (Long) -> Unit)
    suspend fun upload(input: InputStream, remote: String, onBytes: (Long) -> Unit)
    suspend fun mkdir(path: String)
    suspend fun rename(from: String, to: String)
    suspend fun delete(file: RemoteFile)
    suspend fun readText(path: String, maxBytes: Long = 1_048_576L): String
    suspend fun writeText(path: String, text: String)
    fun close()
}

fun isEditableCode(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in EDITABLE_EXT
}

val EDITABLE_EXT = setOf(
    "kt", "kts", "java", "js", "mjs", "cjs", "ts", "tsx", "jsx",
    "py", "rb", "go", "rs", "c", "h", "cpp", "hpp", "cc", "cs",
    "php", "html", "htm", "css", "scss", "json", "xml", "svg",
    "md", "txt", "sh", "bash", "zsh", "yml", "yaml", "toml",
    "gradle", "properties", "sql", "vue", "svelte", "ini", "conf",
    "env", "gitignore", "dockerfile", "makefile",
)
