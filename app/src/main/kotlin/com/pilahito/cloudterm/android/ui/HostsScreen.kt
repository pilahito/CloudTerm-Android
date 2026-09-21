package com.pilahito.cloudterm.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.AppViewModel
import com.pilahito.cloudterm.android.data.AuthType
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.data.Protocol

@Composable
fun HostsScreen(vm: AppViewModel) {
    var editing by remember { mutableStateOf<Host?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Host?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (vm.hosts.isEmpty()) {
            Text(
                "Añade tu primer servidor (SSH, SFTP, FTP o FTPS)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.hosts, key = { it.id }) { host ->
                    Card(
                        onClick = { vm.connect(host) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${host.name}  ·  ${host.protocol.label}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${host.username}@${host.hostname}:${host.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editing = host }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                            IconButton(onClick = { deleting = host }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Mint,
        ) { Icon(Icons.Default.Add, contentDescription = "Añadir servidor") }
    }

    if (creating || editing != null) {
        val initial = editing
        HostDialog(
            initial = initial,
            hasKey = initial != null && vm.hasKey(initial.id),
            onDismiss = { creating = false; editing = null },
            onSave = { host, password, key, passphrase ->
                vm.saveHost(host, password, key, passphrase)
                creating = false
                editing = null
            },
        )
    }

    deleting?.let { host ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar servidor") },
            text = { Text("Se borrará «${host.name}» y sus credenciales guardadas.") },
            confirmButton = { TextButton(onClick = { vm.deleteHost(host); deleting = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HostDialog(
    initial: Host?,
    hasKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (Host, String, String?, String) -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var hostname by remember { mutableStateOf(initial?.hostname ?: "") }
    var protocol by remember { mutableStateOf(initial?.protocol ?: Protocol.SSH) }
    var port by remember { mutableStateOf((initial?.port ?: protocol.defaultPort).toString()) }
    var user by remember { mutableStateOf(initial?.username ?: "") }
    var auth by remember { mutableStateOf(initial?.authType ?: AuthType.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf<String?>(null) }
    val ftpLike = protocol == Protocol.FTP || protocol == Protocol.FTPS
    if (ftpLike && auth != AuthType.PASSWORD) auth = AuthType.PASSWORD
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            keyText = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        }
    }
    val keyOk = auth == AuthType.PASSWORD || keyText != null || hasKey
    val valid = hostname.isNotBlank() && user.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535 && keyOk
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo servidor" else "Editar servidor") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(hostname, { hostname = it }, label = { Text("Dirección") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Text("Protocolo", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Protocol.entries.forEach { p ->
                        FilterChip(selected = protocol == p, onClick = { protocol = p; port = p.defaultPort.toString() }, label = { Text(p.label) })
                    }
                }
                OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Puerto") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(user, { user = it }, label = { Text("Usuario") }, singleLine = true)
                if (!ftpLike) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = auth == AuthType.PASSWORD, onClick = { auth = AuthType.PASSWORD }, label = { Text("Contraseña") })
                        FilterChip(selected = auth == AuthType.KEY, onClick = { auth = AuthType.KEY }, label = { Text("Clave privada") })
                    }
                }
                if (auth == AuthType.PASSWORD || ftpLike) {
                    OutlinedTextField(password, { password = it }, label = { Text(if (initial != null) "Contraseña (vacío = no cambiar)" else "Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                } else {
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text(if (keyText != null || hasKey) "Clave cargada ✓" else "Elegir archivo de clave") }
                    OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val base = initial ?: Host(name = "", hostname = "", username = "")
                onSave(base.copy(name = name.ifBlank { hostname.trim() }, hostname = hostname.trim(), port = port.toInt(), username = user.trim(), authType = if (ftpLike) AuthType.PASSWORD else auth, protocol = protocol), password, keyText, passphrase)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
