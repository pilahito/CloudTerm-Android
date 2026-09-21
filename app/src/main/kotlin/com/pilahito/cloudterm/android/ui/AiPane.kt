package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilahito.cloudterm.android.AppViewModel
import com.pilahito.cloudterm.android.ai.AiAssistant
import com.pilahito.cloudterm.android.ai.AiDetector
import com.pilahito.cloudterm.android.ai.AiTask
import com.pilahito.cloudterm.android.ai.ChatTurn
import com.pilahito.cloudterm.android.ai.Detection
import com.pilahito.cloudterm.android.ai.ModelCatalog

@Composable
fun AiPane(vm: AppViewModel, fileName: String?, fileBody: String?, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var detection by remember { mutableStateOf<Detection?>(null) }
    val turns = remember { mutableStateListOf<ChatTurn>() }

    Column(
        modifier
            .background(CtBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Asistente IA", color = CtText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            "El detector elige tarea y modelo. No se suben logs SSH.",
            color = CtMuted,
            fontSize = 13.sp,
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Pregunta o instrucción") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CtAccent,
                unfocusedBorderColor = CtAccentDim,
                focusedLabelColor = CtAccent,
                unfocusedLabelColor = CtMuted,
                focusedTextColor = CtText,
                unfocusedTextColor = CtText,
                cursorColor = CtAccent,
            ),
        )
        TextButton(
            onClick = {
                val ctx = buildString {
                    if (!fileName.isNullOrBlank()) append("Archivo: $fileName\n")
                    if (!fileBody.isNullOrBlank()) append(fileBody.take(2000))
                    if (isNotEmpty()) append("\n\n")
                    append(prompt)
                }
                val d = AiDetector.detect(ctx, fileName, vm.aiAssignments)
                detection = d
                turns.add(ChatTurn("tú", prompt.ifBlank { fileName ?: "" }))
                turns.add(ChatTurn("ia", AiAssistant.reply(prompt, d, fileName, fileBody)))
            },
            enabled = prompt.isNotBlank() || !fileName.isNullOrBlank(),
        ) { Text("Detectar y preguntar", color = CtAccent) }

        detection?.let { d ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CtCard)
                    .border(1.dp, CtAccent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Tarea: ${d.task.label} · ${d.model.name}", color = CtText, fontWeight = FontWeight.Medium)
                Text(d.reason, color = CtMuted, fontSize = 13.sp)
            }
        }
        turns.takeLast(6).forEach { turn ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CtCard)
                    .padding(10.dp),
            ) {
                Text(if (turn.role == "tú") "Tú" else "Asistente", color = CtAccent, fontSize = 12.sp)
                Text(turn.text, color = CtText, fontSize = 13.sp)
            }
        }
        Text("Asignación por tipo", color = CtText, fontWeight = FontWeight.Medium)
        AiTask.entries.forEach { task ->
            Text(task.label, color = CtMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModelCatalog.all.forEach { model ->
                    FilterChip(
                        selected = vm.aiAssignments[task] == model.id,
                        onClick = { vm.assignModel(task, model.id) },
                        label = { Text(model.name.split(" ").first()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CtAccent.copy(alpha = 0.22f),
                            selectedLabelColor = CtAccent,
                            containerColor = CtCard,
                            labelColor = CtMuted,
                        ),
                    )
                }
            }
        }
    }
}
