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
    var market by remember { mutableStateOf("acode") }
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
        Text(
            "Acode: catálogo oficial acode.app. Los plugins de Acode usan acode.require y se ejecutan en Acode, no en CloudTerm. " +
                "Open VSX: temas/snippets en este editor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = market == "acode", onClick = { market = "acode" }, label = { Text("Acode") })
            FilterChip(selected = market == "vsx", onClick = { market = "vsx" }, label = { Text("Open VSX") })
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
                Text(if (market == "acode") "Marketplace Acode" else "Open VSX", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    query, { query = it },
                    label = { Text(if (market == "acode") "Buscar en Acode" else "Buscar VSIX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        status = "Buscando…"
                        scope.launch {
                            if (market == "acode") {
                                acodeHits = withContext(Dispatchers.IO) {
                                    runCatching { AcodeMarket.search(query) }.getOrElse {
                                        status = it.message ?: "Error Acode"
                                        emptyList()
                                    }
                                }
                                hits = emptyList()
                                if (acodeHits.isNotEmpty()) status = "${acodeHits.size} plugins Acode"
                            } else {
                                hits = withContext(Dispatchers.IO) {
                                    runCatching { OpenVsx.search(query) }.getOrElse {
                                        status = it.message ?: "Error VSX"
                                        emptyList()
                                    }
                                }
                                acodeHits = emptyList()
                                if (hits.isNotEmpty()) status = "${hits.size} VSIX"
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
                        Text(
                            "${hit.author} · v${hit.version}" + if (hit.free) " · gratis" else " · de pago",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (hit.description.isNotBlank()) {
                            Text(hit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !busy && hit.free,
                                onClick = {
                                    busy = true
                                    status = "Descargando ${hit.name}…"
                                    scope.launch {
                                        status = withContext(Dispatchers.IO) {
                                            runCatching {
                                                AcodeMarket.download(hit)
                                                "ZIP de ${hit.name} listo. Ábrelo en Acode: Ajustes → Plugins → + → Remoto/Local."
                                            }.getOrElse { it.message ?: "Error" }
                                        }
                                        busy = false
                                    }
                                },
                            ) { Text(if (hit.free) "Descargar para Acode" else "De pago") }
                            OutlinedButton(
                                onClick = {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://acode.app/plugin/${hit.id}")),
                                    )
                                },
                            ) { Text("Ficha") }
                        }
                    }
                }
            }
            items(hits, key = { it.id }) { hit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(hit.display, style = MaterialTheme.typography.titleSmall)
                        Text("${hit.namespace} · ${hit.version}", style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    status = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val installed = store.installVsix(hit, OpenVsx.download(hit.downloadUrl))
                                            "Instalado: ${installed.name}"
                                        }.getOrElse { it.message ?: "Error" }
                                    }
                                    refresh()
                                    busy = false
                                }
                            },
                        ) { Text("Instalar VSIX aquí") }
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
        ) { Text("Abrir / instalar Acode") }
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
            Text(plugin.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRemove != null) OutlinedButton(onClick = onRemove) { Text("Quitar") }
        }
    }
}
