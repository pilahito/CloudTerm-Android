package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.Transfer
import java.util.Locale

/** Cartel tipo Windows: «Copiando archivo… nombre · X de Y». */
@Composable
fun TransferHud(transfer: Transfer, modifier: Modifier = Modifier) {
    val pct = if (transfer.total > 0) {
        ((transfer.done.toFloat() / transfer.total) * 100f).coerceIn(0f, 100f)
    } else {
        null
    }
    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(transfer.action.ifBlank { "Transfiriendo…" }, style = MaterialTheme.typography.titleSmall)
            if (transfer.fileName.isNotBlank()) {
                Text(transfer.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                transfer.agentLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (pct != null) {
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(humanSize(transfer.done) + " de " + humanSize(transfer.total), style = MaterialTheme.typography.bodySmall)
                    Text(String.format(Locale.US, "%.0f %%", pct), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(humanSize(transfer.done) + " transferidos", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun humanSize(bytes: Long): String {
    if (bytes < 0) return "…"
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
