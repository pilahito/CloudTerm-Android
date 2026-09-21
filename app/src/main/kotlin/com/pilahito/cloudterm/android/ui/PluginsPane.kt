package com.pilahito.cloudterm.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.plugins.AcodeMarket
import com.pilahito.cloudterm.android.plugins.AcodePlugin
import com.pilahito.cloudterm.android.plugins.CtPlugin
import com.pilahito.cloudterm.android.plugins.OpenVsx
import com.pilahito.cloudterm.android.plugins.PluginKind
import com.pilahito.cloudterm.android.plugins.PluginStore
import com.pilahito.cloudterm.android.plugins.VsixHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PluginsPane(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val store = remember { PluginStore(ctx) }
    var plugins by remember { mutableStateOf(store.all()) }
    var market by remember { mutableStateOf("web") }
    var query by remember { mutableStateOf("prettier") }
    var hits by remember { mutableStateOf<List<VsixHit>>(emptyList()) }
    var acodeHits by remember { mutableStateOf<List<AcodePlugin>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() { plugins = store.all() }
    LaunchedEffect(Unit) { refresh() }

    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Plugins", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = market == "web", onClick = { market = "web" }, label = { Text("Web Acode") })
            FilterChip(selected = market == "acode", onClick = { market = "acode" }, label = { Text("API Acode") })
            FilterChip(selected = market == "vsx", onClick = { market = "vsx" }, label = { Text("Open VSX") })
        }
        if (market == "web") {
            AcodeWebPane(Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plugins, key = { it.id }) { p ->
                PluginCard(
                    plugin = p,
                    onToggle = { on -> store.setEnabled(p.id, on); refresh() },
                    onRemove = if (p.kind != PluginKind.BUNDLED) ({ store.remove(p.id); refresh() }) else null,
                )
            }
            item {
                OutlinedTextField(query, { query = it }, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            if (market == "acode") {
                                acodeHits = withContext(Dispatchers.IO) { runCatching { AcodeMarket.search(query) }.getOrDefault(emptyList()) }
                                hits = emptyList()
                            } else {
                                hits = withContext(Dispatchers.IO) { runCatching { OpenVsx.search(query) }.getOrDefault(emptyList()) }
                                acodeHits = emptyList()
                            }
                            busy = false
                        }
                    },
                ) { Text("Buscar") }
            }
            items(acodeHits, key = { it.id }) { hit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(hit.name, style = MaterialTheme.typography.titleSmall)
                        Text("${hit.author} · v${hit.version}", style = MaterialTheme.typography.bodySmall)
                        Button(enabled = !busy && hit.free, onClick = {
                            busy = true
                            scope.launch {
                                status = withContext(Dispatchers.IO) {
                                    runCatching { AcodeMarket.download(hit); "ZIP listo para Acode" }.getOrElse { it.message ?: "Error" }
                                }
                                busy = false
                            }
                        }) { Text(if (hit.free) "Descargar" else "De pago") }
                    }
                }
            }
            items(hits, key = { it.id }) { hit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(hit.display)
                        Button(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                status = withContext(Dispatchers.IO) {
                                    runCatching {
                                        store.installVsix(hit, OpenVsx.download(hit.downloadUrl))
                                        "Instalado ${hit.display}"
                                    }.getOrElse { it.message ?: "Error" }
                                }
                                refresh(); busy = false
                            }
                        }) { Text("Instalar VSIX") }
                    }
                }
            }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = {
                val launch = ctx.packageManager.getLaunchIntentForPackage("com.foxdebug.acodefree")
                    ?: ctx.packageManager.getLaunchIntentForPackage("com.foxdebug.acode")
                if (launch != null) ctx.startActivity(launch)
                else ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.foxdebug.acodefree")))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Abrir Acode") }
    }
}

@Composable
private fun PluginCard(plugin: CtPlugin, onToggle: (Boolean) -> Unit, onRemove: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plugin.name, style = MaterialTheme.typography.titleSmall)
                    Text("${plugin.publisher} · ${plugin.kind}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = plugin.enabled, onCheckedChange = onToggle)
            }
            if (onRemove != null) OutlinedButton(onClick = onRemove) { Text("Quitar") }
        }
    }
}
