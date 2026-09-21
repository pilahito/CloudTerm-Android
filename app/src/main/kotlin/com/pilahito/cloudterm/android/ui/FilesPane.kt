package com.pilahito.cloudterm.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.ActiveSession
import com.pilahito.cloudterm.android.net.isEditableCode
import com.pilahito.cloudterm.android.ssh.RemoteFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesPane(session: ActiveSession, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<RemoteFile?>(null) }
    var renaming by remember { mutableStateOf<RemoteFile?>(null) }
    var deleting by remember { mutableStateOf<RemoteFile?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<RemoteFile?>(null) }

    val download = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val file = pendingDownload
        if (uri != null && file != null) session.download(file, uri)
        pendingDownload = null
    }
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) session.upload(uri)
    }

    LaunchedEffect(Unit) {
        if (session.path.isEmpty()) session.loadInitial()
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { session.up() }, enabled = session.path.isNotEmpty() && session.path != "/") {
                Text("↑ Subir")
            }
            Text(
                session.path,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { session.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }

        val t = session.transfer
        if (t != null) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(t.label, style = MaterialTheme.typography.bodySmall)
                if (t.total > 0) {
                    LinearProgressIndicator(
                        progress = { (t.done.toFloat() / t.total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        } else if (session.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        LazyColumn(Modifier.weight(1f)) {
            items(session.entries, key = { it.path }) { file ->
                FileRow(
                    file = file,
                    onClick = {
                        when {
                            file.isDir -> session.open(file.path)
                            isEditableCode(file.name) -> session.openEditor(file)
                            else -> selected = file
                        }
                    },
                    onMenu = { selected = file },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { upload.launch(arrayOf("*/*")) }) { Text("Subir archivo") }
            OutlinedButton(onClick = { newFolder = true }) { Text("Nueva carpeta") }
        }
    }

    selected?.let { file ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    if (!file.isDir && isEditableCode(file.name)) {
                        TextButton(onClick = {
                            session.openEditor(file)
                            selected = null
                        }) { Text("Editar código") }
                    }
                    if (!file.isDir) {
                        TextButton(onClick = {
                            pendingDownload = file
                            selected = null
                            download.launch(file.name)
                        }) { Text("Descargar") }
                    }
                    TextButton(onClick = { renaming = file; selected = null }) { Text("Renombrar") }
                    TextButton(onClick = { deleting = file; selected = null }) { Text("Eliminar") }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } },
        )
    }

    renaming?.let { file ->
        NameDialog(
            title = "Renombrar",
            initial = file.name,
            onDismiss = { renaming = null },
            onOk = { session.rename(file, it); renaming = null },
        )
    }

    if (newFolder) {
        NameDialog(
            title = "Nueva carpeta",
            initial = "",
            onDismiss = { newFolder = false },
            onOk = { session.mkdir(it); newFolder = false },
        )
    }

    deleting?.let { file ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar") },
            text = {
                Text(
                    if (file.isDir) "Se borrará «${file.name}» y todo su contenido. No se puede deshacer."
                    else "Se borrará «${file.name}». No se puede deshacer."
                )
            },
            confirmButton = { TextButton(onClick = { session.delete(file); deleting = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FileRow(file: RemoteFile, onClick: () -> Unit, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (file.isDir) "📁" else if (isEditableCode(file.name)) "📝" else "📄")
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = buildString {
                if (!file.isDir) append(humanSize(file.size)).append(" · ")
                append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.modified)))
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = text.isNotBlank() && !text.contains('/'), onClick = { onOk(text.trim()) }) {
                Text("Aceptar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return String.format(Locale.US, "%.1f %s", v, units[i])
}
