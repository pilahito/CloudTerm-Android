package com.pilahito.cloudterm.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pilahito.cloudterm.android.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun AppShell(vm: AppViewModel) {
    var splash by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(2) }
    var palette by remember { mutableStateOf(false) }
    var work by remember { mutableStateOf("term") }
    LaunchedEffect(Unit) {
        delay(1200)
        splash = false
    }
    val session = vm.session
    LaunchedEffect(session?.editorOpen) {
        if (session?.editorOpen == true) {
            tab = 2
            work = "code"
        }
    }
    if (splash) {
        Splash()
        return
    }
    BackHandler(enabled = session != null) { vm.disconnect() }
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(Modifier.background(AppBackground)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Mint),
                        contentAlignment = Alignment.Center,
                    ) { Text("S", color = AppBackground, fontWeight = FontWeight.Bold) }
                    Text("  CloudTerm", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { palette = true }) {
                        Icon(Icons.Default.Search, null, tint = Mint)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, null, tint = Mint)
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SessionChip(
                        title = session?.host?.name ?: "editor",
                        subtitle = if (session != null) session.host.protocol.label else "local",
                        selected = true,
                        onClose = { if (session != null) vm.disconnect() },
                    )
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Mint).clickable { tab = 1 },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Add, null, tint = AppBackground) }
                }
            }
        },
        bottomBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(AppSurface).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("UTF-8", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        if (session != null) "Connected · ${session.host.name}" else "Ready",
                        color = Mint,
                        fontSize = 11.sp,
                    )
                }
                NavigationBar(containerColor = AppSurface) {
                    val colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Mint.copy(alpha = 0.25f),
                        selectedIconColor = Mint,
                        selectedTextColor = Mint,
                    )
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, colors = colors)
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Servers") }, colors = colors)
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Terminal, null) }, label = { Text("Terminal") }, colors = colors)
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Outlined.AutoAwesome, null) }, label = { Text("Agents") }, colors = colors)
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = colors)
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(AppBackground)) {
            when (tab) {
                0 -> HomePane(
                    connected = session != null,
                    onTerminal = { tab = 2 },
                    onServers = { tab = 1 },
                )
                1 -> if (session != null) FilesPane(session, Modifier.fillMaxSize()) else HostsScreen(vm)
                2 -> TerminalWorkspace(vm, work) { work = it }
                3 -> AiPane(vm, session?.editorName?.ifBlank { null }, session?.editorText?.ifBlank { null }, Modifier.fillMaxSize())
                else -> PluginsPane(Modifier.fillMaxSize())
            }
            session?.transfer?.let { TransferHud(it, Modifier.align(Alignment.BottomCenter)) }
        }
    }
    if (palette) {
        CommandPalette(
            onDismiss = { palette = false },
            onTerminal = { palette = false; tab = 2; work = "term" },
            onEditor = { palette = false; tab = 2; work = "code" },
            onServers = { palette = false; tab = 1 },
            onAgents = { palette = false; tab = 3 },
            onSettings = { palette = false; tab = 4 },
        )
    }
}

@Composable
private fun TerminalWorkspace(vm: AppViewModel, work: String, setWork: (String) -> Unit) {
    val session = vm.session
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Terminal",
                color = if (work == "term") Mint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { setWork("term") }.padding(8.dp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (session?.editorDirty == true) "Editor·" else "Editor",
                color = if (work == "code") Mint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { setWork("code") }.padding(8.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(Modifier.fillMaxSize()) {
            if (work == "code") {
                if (session != null) EditorPane(session, Modifier.fillMaxSize())
                else LocalEditorPane(Modifier.fillMaxSize())
            } else {
                if (session != null) TerminalPane(session, onClose = { vm.disconnect() }, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Conecta un host en Servers para la terminal.\nEl Editor sí abre sin servidor.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SessionChip(title: String, subtitle: String, selected: Boolean, onClose: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) Mint.copy(alpha = 0.15f) else AppSurface).padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, color = if (selected) Mint else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(Mint), contentAlignment = Alignment.Center) {
                Text("S", color = AppBackground, fontWeight = FontWeight.Bold, fontSize = 36.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("CloudTerm", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Tu equipo de terminales\nestá listo", color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomePane(connected: Boolean, onTerminal: () -> Unit, onServers: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Tu equipo de terminales está listo", color = Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(if (connected) "Sesión activa. Terminal o Editor arriba." else "Misma barra que el vídeo: Home, Servers, Terminal, Agents, Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onTerminal) { Text("Terminal / Editor") }
        TextButton(onClick = onServers) { Text("Servers") }
    }
}

@Composable
private fun CommandPalette(
    onDismiss: () -> Unit,
    onTerminal: () -> Unit,
    onEditor: () -> Unit,
    onServers: () -> Unit,
    onAgents: () -> Unit,
    onSettings: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AppSurface) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(
                    q, { q = it },
                    placeholder = { Text("Command Palette") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Mint, unfocusedIndicatorColor = Mint.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onEditor, modifier = Modifier.fillMaxWidth()) { Text("Editor CloudTerm") }
                TextButton(onClick = onTerminal, modifier = Modifier.fillMaxWidth()) { Text("Terminal") }
                TextButton(onClick = onServers, modifier = Modifier.fillMaxWidth()) { Text("Servers") }
                TextButton(onClick = onAgents, modifier = Modifier.fillMaxWidth()) { Text("Agents") }
                TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
