package com.pilahito.cloudterm.android.ui

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
    var query by remember { mutableStateOf("theme") }
    var hits by remember { mutableStateOf<List<VsixHit>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() { plugins = store.all() }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Plugins del editor", style = MaterialTheme.typography.titleMedium)
        Text(
            "Los plugins nativos de CloudTerm sí corren aquí. De Open VSX / Visual Studio solo se aplican temas y snippets. " +
                "Pylance, Copilot o Remote-SSH necesitan el host de VS Code (pestaña VS).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plugins, key = { it.id }) { p ->
                PluginCard(
                    plugin = p,
                    onToggle = { on ->
                        store.setEnabled(p.id, on)
                        refresh()
                    },
                    onRemove = if (p.kind != PluginKind.BUNDLED) ({
                        store.remove(p.id)
                        refresh()
                    }) else null,
                )
            }
            item {
                Text("Tienda Open VSX", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    query, { query = it },
                    label = { Text("Buscar (theme, prettier, python…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            status = "Buscando…"
                            scope.launch {
                                hits = withContext(Dispatchers.IO) {
                                    runCatching { OpenVsx.search(query) }.getOrElse {
                                        status = it.message ?: "Error"
                                        emptyList()
                                    }
                                }
                                if (hits.isNotEmpty()) status = "${hits.size} resultados"
                                busy = false
                            }
                        },
                    ) { Text("Buscar") }
                }
            }
            items(hits, key = { it.id }) { hit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(hit.display, style = MaterialTheme.typography.titleSmall)
                        Text("${hit.namespace} · ${hit.version}", style = MaterialTheme.typography.bodySmall)
                        if (hit.description.isNotBlank()) {
                            Text(hit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                status = "Descargando ${hit.display}…"
                                scope.launch {
                                    val msg = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val bytes = OpenVsx.download(hit.downloadUrl)
                                            val installed = store.installVsix(hit, bytes)
                                            "Instalado: ${installed.name} (${installed.kind})"
                                        }.getOrElse { it.message ?: "Error al instalar" }
                                    }
                                    status = msg
                                    refresh()
                                    busy = false
                                }
                            },
                        ) { Text("Instalar VSIX") }
                    }
                }
            }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PluginCard(plugin: CtPlugin, onToggle: (Boolean) -> Unit, onRemove: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plugin.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${plugin.publisher} · ${plugin.kind} · ${plugin.version}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = plugin.enabled, onCheckedChange = onToggle)
            }
            Text(plugin.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                plugin.supports.forEach { FilterChip(selected = true, onClick = {}, label = { Text(it) }) }
            }
            if (onRemove != null) {
                OutlinedButton(onClick = onRemove) { Text("Quitar") }
            }
        }
    }
}
