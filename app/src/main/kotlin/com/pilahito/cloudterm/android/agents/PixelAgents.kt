package com.pilahito.cloudterm.android.agents

/**
 * Equipo de agentes que narran lo que está pasando en cada servidor
 * o transferencia (el cartel del vídeo / el diálogo de copiar de Windows).
 */
object PixelAgents {
    const val COPISTA = "Pixel Copista"
    const val CONECTOR = "Pixel Conector"
    const val EDITOR = "Pixel Editor"
    const val GUARDIAN = "Pixel Guardián"

    fun forUpload() = COPISTA
    fun forDownload() = COPISTA
    fun forConnect() = CONECTOR
    fun forEdit() = EDITOR

    fun line(agent: String, action: String, file: String, host: String): String {
        val who = if (host.isBlank()) "" else " · $host"
        return if (file.isBlank()) "$agent: $action$who" else "$agent: $action «$file»$who"
    }
}
