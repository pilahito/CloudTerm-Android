package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
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
    var tab by remember { mutableIntStateOf(0) }
    var palette by remember { mutableStateOf(false) }
    var workbench by remember { mutableStateOf("term") }
    LaunchedEffect(Unit) {
        delay(1500)
        splash = false
    }
    if (splash) {
        Splash()
        return
    }
    val session = vm.session
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(Modifier.background(AppBackground)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Mint, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("S", color = Mint, fontWeight = FontWeight.Bold) }
                    Text("  CloudTerm", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { palette = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Command Palette", tint = Mint)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = Mint)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkbenchChip(
                            title = session?.host?.name ?: "Tu equipo de t…",
                            subtitle = if (session != null) session.host.protocol.label else "local",
                            selected = workbench == "term",
                            onClick = { workbench = "term"; tab = 2 },
                            onClose = if (session != null) ({ vm.disconnect() }) else null,
                        )
                        if (session?.editorOpen == true) {
                            WorkbenchChip(
                                title = session.editorName.ifBlank { "código" },
                                subtitle = session.host.name,
                                selected = workbench == "code",
                                onClick = { workbench = "code"; tab = 2 },
                                onClose = { session.editorOpen = false; workbench = "term" },
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { tab = 1 },
                        modifier = Modifier.size(36.dp),
                        containerColor = Mint,
                    ) { Icon(Icons.Default.Add, contentDescription = "New Host", modifier = Modifier.size(18.dp), tint = AppBackground) }
                }
            }
        },
        bottomBar = {
            Column(Modifier.background(AppSurface)) {
                NavigationBar(containerColor = AppSurface, tonalElevation = 0.dp) {
                    val colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Mint.copy(alpha = 0.18f),
                        selectedIconColor = Mint,
                        selectedTextColor = Mint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, colors = colors)
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Servers") }, colors = colors)
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Terminal, null) }, label = { Text("Terminal") }, colors = colors)
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Agents") }, colors = colors)
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = colors)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("UTF-8", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        when {
                            session != null -> "Connected"
                            vm.connecting != null -> "Connecting"
                            else -> "Ready"
                        },
                        color = Mint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    if (session != null) Text(session.host.protocol.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(AppBackground)) {
            when (tab) {
                0 -> HomePane(onOpen = { tab = 2 }, onPalette = { palette = true })
                1 -> if (session != null) FilesPane(session, Modifier.fillMaxSize()) else HostsScreen(vm)
                2 -> TerminalWorkbench(vm, workbench)
                3 -> AiPane(vm, session?.editorName?.ifBlank { null }, session?.editorText?.ifBlank { null }, Modifier.fillMaxSize())
                else -> SettingsPane(vm)
            }
            session?.transfer?.let { TransferHud(it, Modifier.align(Alignment.BottomCenter)) }
        }
    }
    if (palette) {
        CommandPalette(
            onDismiss = { palette = false },
            onHome = { palette = false; tab = 0 },
            onServers = { palette = false; tab = 1 },
            onTerminal = { palette = false; tab = 2 },
            onAgents = { palette = false; tab = 3 },
            onSettings = { palette = false; tab = 4 },
        )
    }
}

@Composable
private fun SettingsPane(vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("Privacidad", color = Mint, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
        Text(
            "Los hosts se guardan sólo en este teléfono. No hay registro Solaris en GitHub.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 13.sp,
        )
        Button(
            onClick = { vm.wipeAllPrivate() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE24B4A)),
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        ) { Text("Borrar servidores, claves y known_hosts") }
        vm.wipedNotice?.let { Text(it, color = Mint, modifier = Modifier.padding(horizontal = 12.dp), fontSize = 13.sp) }
        PluginsPane(Modifier.weight(1f))
    }
}

@Composable
private fun TerminalWorkbench(vm: AppViewModel, workbench: String) {
    val session = vm.session
    if (session == null) {
        LocalEditorPane(Modifier.fillMaxSize())
        return
    }
    Row(Modifier.fillMaxSize()) {
        FilesPane(session, Modifier.fillMaxHeight().width(148.dp).background(AppSurface))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (workbench == "code" || session.editorOpen) {
                EditorPane(session, Modifier.fillMaxSize())
            } else {
                TerminalPane(session, onClose = { vm.disconnect() }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun WorkbenchChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)?,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppSurface else AppBackground)
            .then(if (selected) Modifier.border(1.dp, Mint, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, color = if (selected) Mint else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Mint.copy(alpha = 0.8f), fontSize = 10.sp)
        }
        if (onClose != null) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp).size(14.dp).clickable(onClick = onClose),
            )
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(24.dp)).border(1.5.dp, Mint, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Mint, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("S", color = Mint, fontWeight = FontWeight.Bold) }
                    Text("  CloudTerm", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Tu equipo de terminales\nestá listo",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text("Command Palette", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Ctrl + K", color = Mint, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomePane(onOpen: () -> Unit, onPalette: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Tu equipo de terminales\nestá listo", color = Color.White, textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Lupa o Ctrl+K · Command Palette", color = Mint, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onPalette) { Text("Abrir Command Palette") }
        TextButton(onClick = onOpen) { Text("Abrir Terminal / Editor") }
    }
}

@Composable
private fun CommandPalette(
    onDismiss: () -> Unit,
    onHome: () -> Unit,
    onServers: () -> Unit,
    onTerminal: () -> Unit,
    onAgents: () -> Unit,
    onSettings: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = AppSurface) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(
                    q, { q = it },
                    placeholder = { Text("Command Palette") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Mint,
                        unfocusedIndicatorColor = Mint.copy(alpha = 0.35f),
                        focusedContainerColor = AppKey,
                        unfocusedContainerColor = AppKey,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home") }
                TextButton(onClick = onServers, modifier = Modifier.fillMaxWidth()) { Text("Servers / New Host") }
                TextButton(onClick = onTerminal, modifier = Modifier.fillMaxWidth()) { Text("Terminal / Editor CloudTerm") }
                TextButton(onClick = onAgents, modifier = Modifier.fillMaxWidth()) { Text("Agents") }
                TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings / borrar datos") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
