package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
    LaunchedEffect(Unit) {
        delay(1400)
        splash = false
    }
    if (splash) {
        Splash()
        return
    }
    val session = vm.session
    if (session != null && tab != 2) {
        SessionScreen(vm, session)
        return
    }
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(AppBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Mint),
                    contentAlignment = Alignment.Center,
                ) { Text("S", color = AppBackground, fontWeight = FontWeight.Bold) }
                Text("  CloudTerm Editor", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { palette = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Command Palette", tint = Mint)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = Mint)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = AppSurface) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Mint.copy(alpha = 0.2f),
                    selectedIconColor = Mint,
                    selectedTextColor = Mint,
                )
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, colors = colors)
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Servers") }, colors = colors)
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Code, null) }, label = { Text("Editor") }, colors = colors)
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Outlined.AutoAwesome, null) }, label = { Text("Agents") }, colors = colors)
                NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = colors)
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(AppBackground)) {
            when (tab) {
                0 -> HomePane { tab = 2 }
                1 -> HostsScreen(vm)
                2 -> if (session != null) EditorPane(session, Modifier.fillMaxSize()) else LocalEditorPane(Modifier.fillMaxSize())
                3 -> AiPane(vm, null, null, Modifier.fillMaxSize())
                else -> PluginsPane(Modifier.fillMaxSize())
            }
        }
    }
    if (palette) CommandPalette(
        onDismiss = { palette = false },
        onEditor = { palette = false; tab = 2 },
        onServers = { palette = false; tab = 1 },
        onAgents = { palette = false; tab = 3 },
        onSettings = { palette = false; tab = 4 },
    )
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(Mint),
                contentAlignment = Alignment.Center,
            ) { Text("S", color = AppBackground, fontWeight = FontWeight.Bold, fontSize = 36.sp) }
            Spacer(Modifier.height(16.dp))
            Text("CloudTerm", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Editor + terminal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tu equipo de terminales\nestá listo",
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomePane(onEditor: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Editor CloudTerm", color = Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("El editor es nuestro: CodeMirror en la app, Guardar al disco o al servidor. VS Code es opcional en la sesión.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onEditor) { Text("Abrir editor") }
    }
}

@Composable
private fun CommandPalette(
    onDismiss: () -> Unit,
    onEditor: () -> Unit,
    onServers: () -> Unit,
    onAgents: () -> Unit,
    onSettings: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AppSurface) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextField(
                    q, { q = it },
                    placeholder = { Text("Command Palette") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Mint, unfocusedIndicatorColor = Mint.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onEditor, modifier = Modifier.fillMaxWidth()) { Text("Editor CloudTerm") }
                TextButton(onClick = onServers, modifier = Modifier.fillMaxWidth()) { Text("Servers") }
                TextButton(onClick = onAgents, modifier = Modifier.fillMaxWidth()) { Text("Agents") }
                TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Plugins / Settings") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
