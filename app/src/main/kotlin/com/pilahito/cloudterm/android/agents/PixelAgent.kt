package com.pilahito.cloudterm.android.agents

data class AgentEvent(
    val agent: String,
    val action: String,
    val detail: String,
    val at: Long = System.currentTimeMillis(),
)

object PixelAgents {
    const val TRANSFER = "Pixel"
    const val SHELL = "Shell"
    const val EDITOR = "Código"
    const val GUARD = "Guardia"
    const val SCOUT = "Huella"
}
