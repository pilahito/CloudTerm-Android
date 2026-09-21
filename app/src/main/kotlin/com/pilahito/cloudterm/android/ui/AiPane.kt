package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        modifier.padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Asistente IA", style = MaterialTheme.typography.titleMedium)
        Text(
            "El detector elige tarea y modelo. No se suben logs SSH.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Pregunta o instrucción") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        OutlinedButton(
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
        ) { Text("Detectar y preguntar") }

        detection?.let { d ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tarea: ${d.task.label} · ${d.model.name}", style = MaterialTheme.typography.titleSmall)
                    Text(d.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        turns.takeLast(6).forEach { turn ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(if (turn.role == "tú") "Tú" else "Asistente", style = MaterialTheme.typography.labelMedium)
                    Text(turn.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Asignación por tipo", style = MaterialTheme.typography.titleSmall)
        AiTask.entries.forEach { task ->
            Text(task.label, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModelCatalog.all.forEach { model ->
                    FilterChip(
                        selected = vm.aiAssignments[task] == model.id,
                        onClick = { vm.assignModel(task, model.id) },
                        label = { Text(model.name.split(" ").first()) },
                    )
                }
            }
        }
    }
}
