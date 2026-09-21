package com.pilahito.cloudterm.android.ai

enum class AiTask {
    CODE,
    SHELL,
    EXPLAIN,
    CHAT,
    TRANSLATE,
    ;

    val label: String
        get() = when (this) {
            CODE -> "Código"
            SHELL -> "Terminal"
            EXPLAIN -> "Explicar"
            CHAT -> "Chat"
            TRANSLATE -> "Traducir"
        }
}

data class AiModel(
    val id: String,
    val name: String,
    val backend: AiBackend,
    val goodFor: Set<AiTask>,
    val notes: String,
)

enum class AiBackend {
    OLLAMA,
    GEMINI,
    EDGE_GALLERY,
    ;

    val label: String
        get() = when (this) {
            OLLAMA -> "Ollama"
            GEMINI -> "Gemini"
            EDGE_GALLERY -> "AI Edge (local)"
        }
}

data class Detection(
    val task: AiTask,
    val score: Int,
    val model: AiModel,
    val reason: String,
)

object ModelCatalog {
    val all: List<AiModel> = listOf(
        AiModel(
            id = "qwen-coder",
            name = "Qwen2.5-Coder",
            backend = AiBackend.OLLAMA,
            goodFor = setOf(AiTask.CODE),
            notes = "Ollama local. Mejor para editar y generar código.",
        ),
        AiModel(
            id = "llama-chat",
            name = "Llama 3.x",
            backend = AiBackend.OLLAMA,
            goodFor = setOf(AiTask.CHAT, AiTask.EXPLAIN),
            notes = "Ollama local. Chat y explicaciones.",
        ),
        AiModel(
            id = "gemini-flash",
            name = "Gemini Flash",
            backend = AiBackend.GEMINI,
            goodFor = setOf(AiTask.CODE, AiTask.EXPLAIN, AiTask.CHAT, AiTask.TRANSLATE, AiTask.SHELL),
            notes = "Nube. Sirve para todo si hay API key.",
        ),
        AiModel(
            id = "gemma-edge",
            name = "Gemma (AI Edge Gallery)",
            backend = AiBackend.EDGE_GALLERY,
            goodFor = setOf(AiTask.CHAT, AiTask.EXPLAIN),
            notes = "En el teléfono vía la app de Google. Débil en Snapdragon 660.",
        ),
    )

    fun byId(id: String): AiModel =
        all.firstOrNull { it.id == id } ?: all.first { it.id == "gemini-flash" }
}

/**
 * Clasifica el texto (y el archivo abierto) y elige el modelo asignado a esa tarea.
 * No llama a ningún LLM: es un detector local por palabras clave.
 */
object AiDetector {
    private val codeHints = listOf(
        "fun ", "class ", "def ", "import ", "package ", "#!/",
        "error:", "exception", "stacktrace", "nullpointer",
        "refactor", "arregla", "corrige", "implementa", "código",
        "codigo", "function", "const ", "let ", "var ", "<?php",
    )
    private val shellHints = listOf(
        "ssh ", "systemctl", "journalctl", "chmod", "chown", "apt ",
        "yum ", "dnf ", "docker ", "kubectl", "nginx", "comando",
        "terminal", "bash", "pipe", "grep ", "journal",
    )
    private val explainHints = listOf(
        "explica", "qué hace", "que hace", "por qué", "porque",
        "what does", "explain", "resume", "resumen",
    )
    private val translateHints = listOf(
        "traduce", "traducir", "translate", "al español", "to english",
    )

    fun detect(
        prompt: String,
        fileName: String? = null,
        assignments: Map<AiTask, String>,
    ): Detection {
        val text = prompt.lowercase()
        val scores = mutableMapOf(
            AiTask.CODE to 0,
            AiTask.SHELL to 0,
            AiTask.EXPLAIN to 0,
            AiTask.CHAT to 1,
            AiTask.TRANSLATE to 0,
        )
        fun bump(task: AiTask, words: List<String>, weight: Int = 2) {
            if (words.any { text.contains(it) }) scores[task] = scores.getValue(task) + weight
        }
        bump(AiTask.CODE, codeHints, 3)
        bump(AiTask.SHELL, shellHints, 3)
        bump(AiTask.EXPLAIN, explainHints, 3)
        bump(AiTask.TRANSLATE, translateHints, 4)

        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (ext in setOf("kt", "kts", "java", "js", "ts", "py", "go", "rs", "php", "c", "cpp")) {
            scores[AiTask.CODE] = scores.getValue(AiTask.CODE) + 4
        }
        if (ext in setOf("sh", "bash", "zsh")) {
            scores[AiTask.SHELL] = scores.getValue(AiTask.SHELL) + 3
        }

        val task = scores.maxBy { it.value }.key
        val modelId = assignments[task] ?: defaultModel(task)
        val model = ModelCatalog.byId(modelId)
        val reason = buildString {
            append("Tarea: ${task.label} (puntos ${scores[task]}). ")
            append("Modelo: ${model.name} · ${model.backend.label}.")
        }
        return Detection(task, scores.getValue(task), model, reason)
    }

    fun defaultModel(task: AiTask): String = when (task) {
        AiTask.CODE -> "qwen-coder"
        AiTask.SHELL -> "gemini-flash"
        AiTask.EXPLAIN -> "gemini-flash"
        AiTask.CHAT -> "gemma-edge"
        AiTask.TRANSLATE -> "gemini-flash"
    }

    fun defaultAssignments(): Map<AiTask, String> =
        AiTask.entries.associateWith { defaultModel(it) }
}
