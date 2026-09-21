package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilahito.cloudterm.android.ActiveSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PixelAgentsPane(session: ActiveSession, modifier: Modifier = Modifier) {
    Column(
        modifier.background(CtBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Pixel Agents", color = CtText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            "Cada transferencia y acción del servidor se narra aquí, al estilo Windows.",
            color = CtMuted,
            fontSize = 13.sp,
        )
        val t = session.transfer
        if (t != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CtCard)
                    .border(1.dp, CtAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(t.label, color = CtText)
                if (t.total > 0) {
                    LinearProgressIndicator(
                        progress = { (t.done.toFloat() / t.total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = CtAccent,
                        trackColor = CtAccentDim.copy(alpha = 0.3f),
                    )
                    Text("${t.done} / ${t.total} bytes", color = CtMuted, fontSize = 12.sp)
                } else {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth(),
                        color = CtAccent,
                        trackColor = CtAccentDim.copy(alpha = 0.3f),
                    )
                }
            }
        }
        if (session.agentLog.isEmpty() && t == null) {
            Text("Sin actividad todavía. Sube o descarga un archivo para ver a Pixel en acción.", color = CtMuted)
        }
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(session.agentLog.reversed()) { ev ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CtCard)
                        .padding(12.dp),
                ) {
                    Text("${ev.agent} · ${ev.action}", color = CtText, fontWeight = FontWeight.Medium)
                    Text(ev.detail, color = CtMuted, fontSize = 13.sp)
                    Text(
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ev.at)),
                        color = CtAccentDim,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
