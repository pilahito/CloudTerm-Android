package com.pilahito.cloudterm.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilahito.cloudterm.android.AppViewModel
import com.pilahito.cloudterm.android.data.AuthType
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.data.Protocol

@Composable
fun HostsScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Host?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Host?>(null) }
    var palette by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CtBg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A151C)) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CtAccent,
                    selectedTextColor = CtAccent,
                    unselectedIconColor = CtMuted,
                    unselectedTextColor = CtMuted,
                    indicatorColor = Color.Transparent,
                )
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Home, null) }, label = { Text("Home") }, colors = colors)
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.Phone, null) }, label = { Text("Servers") }, colors = colors)
                NavigationBarItem(tab == 2, { tab = 2; if (vm.hosts.isNotEmpty()) vm.connect(vm.hosts.first()) }, { Icon(Icons.Filled.Search, null) }, label = { Text("Terminal") }, colors = colors)
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Filled.Person, null) }, label = { Text("Agents") }, colors = colors)
                NavigationBarItem(tab == 4, { tab = 4 }, { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") }, colors = colors)
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(CtBg)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HexLogo(30.dp)
                Spacer(Modifier.width(10.dp))
                Text("CloudTerm", color = CtText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { palette = true }) { Icon(Icons.Filled.Search, null, tint = CtMuted) }
                IconButton(onClick = { }) { Icon(Icons.Filled.Notifications, null, tint = CtMuted) }
            }
            when (tab) {
                0 -> HomeHero(hosts = vm.hosts, onPalette = { palette = true }, onAdd = { creating = true }, onOpen = { vm.connect(it) })
                1 -> ServersPane(hosts = vm.hosts, onOpen = { vm.connect(it) }, onEdit = { editing = it }, onDelete = { deleting = it }, onAdd = { creating = true })
                3 -> AgentsLobby(hosts = vm.hosts)
                4 -> SettingsPane(vm)
                else -> HomeHero(vm.hosts, { palette = true }, { creating = true }, { vm.connect(it) })
            }
        }
    }

    if (palette) {
        CommandPalette(
            hosts = vm.hosts, query = query, onQuery = { query = it },
            onClose = { palette = false; query = "" },
            onHost = { palette = false; query = ""; vm.connect(it) },
            onAdd = { palette = false; creating = true },
        )
    }
    if (creating || editing != null) {
        val initial = editing
        HostDialog(
            initial = initial,
            hasKey = initial != null && vm.hasKey(initial.id),
            hasTotp = initial != null && vm.hasTotp(initial.id),
            onDismiss = { creating = false; editing = null },
            onSave = { host, password, key, passphrase, totp ->
                vm.saveHost(host, password, key, passphrase, totp)
                creating = false; editing = null
            },
        )
    }
    deleting?.let { host ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar servidor") },
            text = { Text("Se borrara «${host.name}» y sus credenciales guardadas.") },
            confirmButton = { TextButton(onClick = { vm.deleteHost(host); deleting = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HomeHero(hosts: List<Host>, onPalette: () -> Unit, onAdd: () -> Unit, onOpen: (Host) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.clip(RoundedCornerShape(24.dp)).border(1.5.dp, CtAccent, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HexLogo(22.dp)
            Spacer(Modifier.width(8.dp))
            Text("CloudTerm", color = CtAccent, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(18.dp))
        Text("Tu equipo de\nterminales\nesta listo", color = CtText, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp)
        Spacer(Modifier.height(12.dp))
        OfficeScene(Modifier.fillMaxWidth().height(240.dp))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onPalette).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            KeyCap("Ctrl", CtPink)
            Text("  +  ", color = CtMuted)
            KeyCap("K", CtAccent)
            Spacer(Modifier.width(10.dp))
            Text("Para empezar a usar\nlos atajos de teclado", color = CtMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        if (hosts.isEmpty()) {
            TextButton(onClick = onAdd) { Text("Anadir primer servidor", color = CtAccent) }
        } else {
            hosts.take(3).forEach { h ->
                Text(h.name, color = CtAccent, modifier = Modifier.fillMaxWidth().clickable { onOpen(h) }.padding(vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KeyCap(label: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.18f)).border(1.dp, color, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun OfficeScene(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val origin = Offset(w * 0.50f, h * 0.72f)
        fun iso(x: Float, y: Float, z: Float): Offset {
            return Offset(origin.x + (x - y) * 22f, origin.y + (x + y) * 12f - z * 16f)
        }
        for (gx in -6..8) {
            for (gy in -6..8) {
                val a = iso(gx.toFloat(), gy.toFloat(), 0f)
                val b = iso(gx + 1f, gy.toFloat(), 0f)
                val c = iso(gx + 1f, gy + 1f, 0f)
                val d = iso(gx.toFloat(), gy + 1f, 0f)
                val p = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }
                drawPath(p, CtAccent.copy(alpha = if ((gx + gy) % 2 == 0) 0.10f else 0.05f))
            }
        }
        fun desk(x: Float, y: Float) {
            val top = Path().apply {
                val p1 = iso(x, y, 1.1f); val p2 = iso(x + 2.2f, y, 1.1f)
                val p3 = iso(x + 2.2f, y + 1.4f, 1.1f); val p4 = iso(x, y + 1.4f, 1.1f)
                moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
            }
            drawPath(top, Color(0xFFE8F4F8))
            val mon = iso(x + 0.7f, y + 0.4f, 2.4f)
            drawRect(CtAccent.copy(alpha = 0.55f), topLeft = Offset(mon.x - 10f, mon.y - 18f), size = androidx.compose.ui.geometry.Size(28f, 22f))
        }
        desk(-1.2f, -0.4f); desk(1.4f, -0.6f); desk(-0.2f, 1.6f); desk(2.2f, 1.4f)
    }
}

@Composable
private fun ServersPane(hosts: List<Host>, onOpen: (Host) -> Unit, onEdit: (Host) -> Unit, onDelete: (Host) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Remote hosts", color = CtText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, null, tint = CtAccent) }
        }
        Text("Scan existing servers", color = CtMuted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        if (hosts.isEmpty()) {
            Text("Aun no hay servidores. Pulsa + para anadir SSH, SFTP, FTP o FTPS.", color = CtMuted)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hosts, key = { it.id }) { host ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CtCard).clickable { onOpen(host) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(host.name, color = CtText, fontWeight = FontWeight.SemiBold)
                            Text("${host.protocol.label} · ${host.username}@${host.hostname}", color = CtMuted, fontSize = 12.sp)
                        }
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(8.dp)).background(CtOnline))
                        IconButton(onClick = { onEdit(host) }) { Icon(Icons.Filled.Edit, null, tint = CtMuted) }
                        IconButton(onClick = { onDelete(host) }) { Icon(Icons.Filled.Delete, null, tint = CtMuted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentsLobby(hosts: List<Host>) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Pixel Agents", color = CtText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("60fps", color = CtAccent, modifier = Modifier.padding(vertical = 6.dp))
        OfficeScene(Modifier.fillMaxWidth().height(260.dp))
        Spacer(Modifier.height(12.dp))
        hosts.take(3).forEach { Text("${it.name}  ·  Latencia —", color = CtMuted, fontSize = 13.sp, modifier = Modifier.padding(4.dp)) }
        if (hosts.isEmpty()) Text("Conecta un servidor para ver agentes en la oficina.", color = CtMuted)
    }
}

@Composable
private fun SettingsPane(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", color = CtText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        TextButton(onClick = { vm.checkForUpdate(silent = false) }) { Text("Actualizar app", color = CtAccent) }
        TextButton(onClick = { vm.setBiometric(!vm.biometricEnabled) }) {
            Text(if (vm.biometricEnabled) "Huella ON" else "Huella OFF", color = CtAccent)
        }
        Text("Tema mint · hex logo · atajos Ctrl+K", color = CtMuted, fontSize = 13.sp)
    }
}

@Composable
private fun CommandPalette(hosts: List<Host>, query: String, onQuery: (String) -> Unit, onClose: () -> Unit, onHost: (Host) -> Unit, onAdd: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Command Palette", color = CtAccent) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, onQuery, placeholder = { Text("Search") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CtAccent))
                val filtered = hosts.filter { it.name.contains(query, true) || it.hostname.contains(query, true) }
                filtered.take(6).forEach { h -> Text(h.name, color = CtText, modifier = Modifier.fillMaxWidth().clickable { onHost(h) }.padding(8.dp)) }
                Text("Anadir servidor", color = CtAccent, modifier = Modifier.clickable(onClick = onAdd).padding(8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
    )
}

@Composable
private fun HostDialog(initial: Host?, hasKey: Boolean, hasTotp: Boolean, onDismiss: () -> Unit, onSave: (Host, String, String?, String, String) -> Unit) {
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
    var totpOn by remember { mutableStateOf(initial?.totpEnabled ?: false) }
    var totpSecret by remember { mutableStateOf("") }
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
                OutlinedTextField(hostname, { hostname = it }, label = { Text("Direccion") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
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
                        FilterChip(selected = auth == AuthType.PASSWORD, onClick = { auth = AuthType.PASSWORD }, label = { Text("Contrasena") })
                        FilterChip(selected = auth == AuthType.KEY, onClick = { auth = AuthType.KEY }, label = { Text("Clave privada") })
                    }
                }
                if (auth == AuthType.PASSWORD || ftpLike) {
                    OutlinedTextField(password, { password = it }, label = { Text(if (initial != null) "Contrasena (vacio = no cambiar)" else "Contrasena") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                } else {
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text(if (keyText != null || hasKey) "Clave cargada" else "Elegir archivo de clave") }
                    OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                }
                FilterChip(selected = totpOn, onClick = { totpOn = !totpOn }, label = { Text("2FA TOTP") })
                if (totpOn) {
                    OutlinedTextField(totpSecret, { totpSecret = it }, label = { Text(if (hasTotp) "Secreto TOTP (vacio = no cambiar)" else "Secreto TOTP base32") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val base = initial ?: Host(name = "", hostname = "", username = "")
                val host = base.copy(name = name.ifBlank { hostname.trim() }, hostname = hostname.trim(), port = port.toInt(), username = user.trim(), authType = if (ftpLike) AuthType.PASSWORD else auth, protocol = protocol, totpEnabled = totpOn)
                onSave(host, password, keyText, passphrase, totpSecret)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
