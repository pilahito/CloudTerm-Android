package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.ActiveSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PixelAgentsPane(session: ActiveSession, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Pixel Agents", style = MaterialTheme.typography.titleMedium)
        Text(
            "Cada transferencia y acción del servidor se narra aquí, al estilo Windows.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val t = session.transfer
        if (t != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t.label)
                    if (t.total > 0) {
                        LinearProgressIndicator(
                            progress = { (t.done.toFloat() / t.total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${t.done} / ${t.total} bytes", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (session.agentLog.isEmpty() && t == null) {
            Text("Sin actividad todavía. Sube o descarga un archivo para ver a Pixel en acción.")
        }
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(session.agentLog.reversed()) { ev ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${ev.agent} · ${ev.action}", style = MaterialTheme.typography.titleSmall)
                        Text(ev.detail, style = MaterialTheme.typography.bodySmall)
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ev.at)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
