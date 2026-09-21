package com.pilahito.cloudterm.android.ai

data class ChatTurn(val role: String, val text: String)

object AiAssistant {
    fun reply(prompt: String, detection: Detection, fileName: String?, fileBody: String?): String {
        val name = fileName ?: "sin archivo"
        val preview = fileBody?.take(400).orEmpty()
        return buildString {
            appendLine("[${detection.model.name} · ${detection.task.label}]")
            appendLine(detection.reason)
            appendLine()
            when (detection.task) {
                AiTask.CODE -> {
                    appendLine("Asistente de código sobre «$name».")
                    if (preview.isNotBlank()) {
                        appendLine("Fragmento visto:")
                        appendLine(preview)
                        appendLine()
                    }
                    appendLine("Sugerencia: describe el cambio y pégalo en el editor. El resaltado token a token corre en Código.")
                }
                AiTask.SHELL -> {
                    appendLine("Asistente de terminal. No ejecuto comandos solos.")
                    appendLine("Pedido: $prompt")
                }
                AiTask.EXPLAIN -> {
                    appendLine("Explicación pedida sobre $name.")
                    if (preview.isNotBlank()) appendLine(preview.take(280))
                }
                AiTask.TRANSLATE -> appendLine("Traducción local. No se envía tu servidor a la nube.")
                AiTask.CHAT -> appendLine("Chat local. Modelo: ${detection.model.name}. ${detection.model.notes}")
            }
        }.trim()
    }
}
