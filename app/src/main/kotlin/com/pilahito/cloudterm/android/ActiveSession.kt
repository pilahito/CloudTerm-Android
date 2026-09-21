package com.pilahito.cloudterm.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.ssh.RemoteFile
import com.pilahito.cloudterm.android.ssh.SshConnection
import com.pilahito.cloudterm.android.ssh.joinPath
import com.pilahito.cloudterm.android.ssh.parentPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

data class Transfer(val label: String, val done: Long, val total: Long)

/** Sesión abierta: la conexión y el estado del explorador de archivos. */
class ActiveSession(
    val host: Host,
    val conn: SshConnection,
    private val scope: CoroutineScope,
    private val resolver: ContentResolver,
) {
    var path by mutableStateOf("")
    var entries by mutableStateOf<List<RemoteFile>>(emptyList())
    var loading by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    var transfer by mutableStateOf<Transfer?>(null)
    var shellClosed by mutableStateOf(false)

    init {
        conn.onShellClosed = { shellClosed = true }
    }

    fun startShell(cols: Int, rows: Int, sink: (ByteArray) -> Unit) {
        conn.outputSink = sink
        scope.launch(Dispatchers.IO) {
            try {
                conn.openShell(cols, rows)
            } catch (e: Exception) {
                notice = "No se pudo abrir el terminal: ${e.message}"
            }
        }
    }

    fun loadInitial() {
        scope.launch {
            loading = true
            try {
                val home = conn.home()
                entries = conn.list(home)
                path = home
            } catch (e: Exception) {
                notice = "No se pudo listar el directorio: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun open(target: String) {
        scope.launch {
            loading = true
            try {
                entries = conn.list(target)
                path = target
            } catch (e: Exception) {
                notice = "No se pudo abrir $target: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun up() = open(parentPath(path))

    fun refresh() = open(path)

    fun mkdir(name: String) {
        scope.launch {
            try {
                conn.mkdir(joinPath(path, name))
            } catch (e: Exception) {
                notice = "No se pudo crear la carpeta: ${e.message}"
            }
            refresh()
        }
    }

    fun rename(file: RemoteFile, newName: String) {
        scope.launch {
            try {
                conn.rename(file.path, joinPath(parentPath(file.path), newName))
            } catch (e: Exception) {
                notice = "No se pudo renombrar: ${e.message}"
            }
            refresh()
        }
    }

    fun delete(file: RemoteFile) {
        scope.launch {
            try {
                conn.delete(file)
            } catch (e: Exception) {
                notice = "No se pudo eliminar: ${e.message}"
            }
            refresh()
        }
    }

    fun download(file: RemoteFile, destination: Uri) {
        scope.launch {
            val label = "↓ ${file.name}"
            transfer = Transfer(label, 0, file.size)
            try {
                val out = resolver.openOutputStream(destination)
                    ?: throw IOException("no se pudo escribir el destino")
                out.use { conn.download(file.path, it) { done -> transfer = Transfer(label, done, file.size) } }
                notice = "Descargado: ${file.name}"
            } catch (e: Exception) {
                notice = "Error al descargar: ${e.message}"
            } finally {
                transfer = null
            }
        }
    }

    fun upload(source: Uri) {
        scope.launch {
            var name = "archivo"
            var size = -1L
            resolver.query(source, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni) ?: name
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
            val label = "↑ $name"
            transfer = Transfer(label, 0, size)
            try {
                val input = resolver.openInputStream(source)
                    ?: throw IOException("no se pudo leer el archivo")
                input.use { conn.upload(it, joinPath(path, name)) { done -> transfer = Transfer(label, done, size) } }
                notice = "Subido: $name"
                refresh()
            } catch (e: Exception) {
                notice = "Error al subir: ${e.message}"
            } finally {
                transfer = null
            }
        }
    }

    fun close() = conn.close()
}
