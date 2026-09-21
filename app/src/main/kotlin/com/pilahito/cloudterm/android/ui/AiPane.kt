package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.AppViewModel
import com.pilahito.cloudterm.android.ai.AiDetector
import com.pilahito.cloudterm.android.ai.AiTask
import com.pilahito.cloudterm.android.ai.Detection
import com.pilahito.cloudterm.android.ai.ModelCatalog

@Composable
fun AiPane(vm: AppViewModel, fileName: String?, fileBody: String?, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var detection by remember { mutableStateOf<Detection?>(null) }

    Column(
        modifier.padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Detector de IA", style = MaterialTheme.typography.titleMedium)
        Text(
            "Escribe lo que necesitas. CloudTerm detecta la tarea y le asigna un modelo.",
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
                detection = AiDetector.detect(ctx, fileName, vm.aiAssignments)
            },
            enabled = prompt.isNotBlank() || !fileName.isNullOrBlank(),
        ) { Text("Detectar y asignar") }

        detection?.let { d ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tarea: ${d.task.label}", style = MaterialTheme.typography.titleSmall)
                    Text("Modelo: ${d.model.name}")
                    Text("Backend: ${d.model.backend.label}")
                    Text(d.reason, style = MaterialTheme.typography.bodySmall)
                    Text(d.model.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
