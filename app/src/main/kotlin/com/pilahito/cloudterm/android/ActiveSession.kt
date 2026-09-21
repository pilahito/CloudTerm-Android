package com.pilahito.cloudterm.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pilahito.cloudterm.android.agents.AgentEvent
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

data class Transfer(val label: String, val done: Long, val total: Long)

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
    var agentLog by mutableStateOf<List<AgentEvent>>(emptyList())

    var editorPath by mutableStateOf<String?>(null)
    var editorName by mutableStateOf("")
    var editorText by mutableStateOf("")
    var editorDirty by mutableStateOf(false)
    var editorLoading by mutableStateOf(false)
    var editorOpen by mutableStateOf(false)

    init {
        conn.onShellClosed = { shellClosed = true }
        narrate(PixelAgents.GUARD, "Sesión", "Conectado. No se publican registros del servidor.")
    }

    fun narrate(agent: String, action: String, detail: String) {
        agentLog = (agentLog + AgentEvent(agent, action, detail)).takeLast(80)
    }

    fun startShell(cols: Int, rows: Int, sink: (ByteArray) -> Unit) {
        conn.outputSink = sink
        narrate(PixelAgents.SHELL, "Terminal", "Abriendo PTY ${cols}x${rows}")
        scope.launch(Dispatchers.IO) {
            try {
                conn.openShell(cols, rows)
            } catch (e: Exception) {
                notice = "No se pudo abrir el terminal."
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
                narrate(PixelAgents.TRANSFER, "Listar", "Directorio inicial listo")
            } catch (e: Exception) {
                notice = "No se pudo listar el directorio."
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
                notice = "No se pudo abrir el directorio."
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
                narrate(PixelAgents.TRANSFER, "Carpeta", name)
            } catch (e: Exception) {
                notice = "No se pudo crear la carpeta."
            }
            refresh()
        }
    }

    fun rename(file: RemoteFile, newName: String) {
        scope.launch {
            try {
                conn.rename(file.path, joinPath(parentPath(file.path), newName))
                narrate(PixelAgents.TRANSFER, "Renombrar", file.name)
            } catch (e: Exception) {
                notice = "No se pudo renombrar."
            }
            refresh()
        }
    }

    fun delete(file: RemoteFile) {
        scope.launch {
            try {
                conn.delete(file)
                narrate(PixelAgents.TRANSFER, "Eliminar", file.name)
            } catch (e: Exception) {
                notice = "No se pudo eliminar."
            }
            refresh()
        }
    }

    fun download(file: RemoteFile, destination: Uri) {
        scope.launch {
            val label = "↓ ${file.name}"
            transfer = Transfer(label, 0, file.size)
            narrate(PixelAgents.TRANSFER, "Descarga", "Pixel está moviendo ${file.name}")
            try {
                val out = resolver.openOutputStream(destination)
                    ?: throw IOException("destino")
                out.use { conn.download(file.path, it) { done -> transfer = Transfer(label, done, file.size) } }
                notice = "Descargado: ${file.name}"
                narrate(PixelAgents.TRANSFER, "Listo", file.name)
            } catch (e: Exception) {
                notice = "Error al descargar."
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
            narrate(PixelAgents.TRANSFER, "Subida", "Pixel está enviando $name")
            try {
                val input = resolver.openInputStream(source) ?: throw IOException("origen")
                input.use { conn.upload(it, joinPath(path, name)) { done -> transfer = Transfer(label, done, size) } }
                notice = "Subido: $name"
                narrate(PixelAgents.TRANSFER, "Listo", name)
                refresh()
            } catch (e: Exception) {
                notice = "Error al subir."
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
        narrate(PixelAgents.EDITOR, "Abrir", file.name)
        scope.launch {
            try {
                editorText = conn.readText(file.path)
            } catch (e: Exception) {
                notice = "No se pudo abrir el código."
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
        narrate(PixelAgents.EDITOR, "Guardar", editorName)
        scope.launch {
            try {
                conn.writeText(remote, editorText)
                editorDirty = false
                notice = "Guardado: $editorName"
            } catch (e: Exception) {
                notice = "No se pudo guardar."
            } finally {
                editorLoading = false
            }
        }
    }

    fun close() = conn.close()
}
