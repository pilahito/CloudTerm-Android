package com.pilahito.cloudterm.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pilahito.cloudterm.android.agents.PixelAgents
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.net.RemoteFs
import com.pilahito.cloudterm.android.ssh.RemoteFile
import com.pilahito.cloudterm.android.ssh.joinPath
import com.pilahito.cloudterm.android.ssh.parentPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

data class Transfer(
    val label: String,
    val done: Long,
    val total: Long,
    val host: String = "",
    val fileName: String = "",
    val action: String = "",
    val agent: String = PixelAgents.COPISTA,
) {
    val agentLine: String
        get() = PixelAgents.line(agent, action.ifBlank { label }, fileName, host)
}

class ActiveSession(
    val host: Host,
    val conn: RemoteFs,
    private val scope: CoroutineScope,
    private val resolver: ContentResolver,
) {
    var path by mutableStateOf("")
    var entries by mutableStateOf<List<RemoteFile>>(emptyList())
    var loading by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    var transfer by mutableStateOf<Transfer?>(null)
    var shellClosed by mutableStateOf(false)

    var editorPath by mutableStateOf<String?>(null)
    var editorName by mutableStateOf("")
    var editorText by mutableStateOf("")
    var editorDirty by mutableStateOf(false)
    var editorLoading by mutableStateOf(false)
    var editorOpen by mutableStateOf(false)

    private val hostTag: String
        get() = "${host.username}@${host.hostname}:${host.port}"

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
            fun snap(done: Long) = Transfer(
                label = label,
                done = done,
                total = file.size,
                host = hostTag,
                fileName = file.name,
                action = "Descargando archivo",
                agent = PixelAgents.forDownload(),
            )
            transfer = snap(0)
            try {
                val out = resolver.openOutputStream(destination)
                    ?: throw IOException("no se pudo escribir el destino")
                out.use { conn.download(file.path, it) { done -> transfer = snap(done) } }
                notice = PixelAgents.line(PixelAgents.COPISTA, "descargó", file.name, hostTag)
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
            fun snap(done: Long) = Transfer(
                label = label,
                done = done,
                total = size,
                host = hostTag,
                fileName = name,
                action = "Copiando archivo al servidor",
                agent = PixelAgents.forUpload(),
            )
            transfer = snap(0)
            try {
                val input = resolver.openInputStream(source)
                    ?: throw IOException("no se pudo leer el archivo")
                input.use { conn.upload(it, joinPath(path, name)) { done -> transfer = snap(done) } }
                notice = PixelAgents.line(PixelAgents.COPISTA, "subió", name, hostTag)
                refresh()
            } catch (e: Exception) {
                notice = "Error al subir: ${e.message}"
            } finally {
                transfer = null
            }
        }
    }

    fun openEditor(file: RemoteFile) {
        editorOpen = true
        editorPath = file.path
        editorName = file.name
        editorLoading = true
        editorDirty = false
        scope.launch {
            try {
                editorText = conn.readText(file.path)
            } catch (e: Exception) {
                notice = "No se pudo abrir el código: ${e.message}"
                editorOpen = false
            } finally {
                editorLoading = false
            }
        }
    }

    fun onEditorChange(text: String) {
        editorText = text
        editorDirty = true
    }

    fun saveEditor() {
        val remote = editorPath ?: return
        editorLoading = true
        scope.launch {
            try {
                conn.writeText(remote, editorText)
                editorDirty = false
                notice = PixelAgents.line(PixelAgents.EDITOR, "guardó", editorName, hostTag)
            } catch (e: Exception) {
                notice = "No se pudo guardar: ${e.message}"
            } finally {
                editorLoading = false
            }
        }
    }

    fun close() = conn.close()
}
