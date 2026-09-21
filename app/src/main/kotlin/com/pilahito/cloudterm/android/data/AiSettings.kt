package com.pilahito.cloudterm.android.data

import android.content.Context
import com.pilahito.cloudterm.android.ai.AiDetector
import com.pilahito.cloudterm.android.ai.AiTask
import org.json.JSONObject
import java.io.File

class AiSettings(context: Context) {
    private val file = File(context.filesDir, "ai_assignments.json")

    var ollamaUrl: String = "http://127.0.0.1:11434"
        private set
    var geminiKey: String = ""
        private set
    var assignments: Map<AiTask, String> = AiDetector.defaultAssignments()
        private set

    init {
        load()
    }

    fun save(
        ollamaUrl: String = this.ollamaUrl,
        geminiKey: String = this.geminiKey,
        assignments: Map<AiTask, String> = this.assignments,
    ) {
        this.ollamaUrl = ollamaUrl.trim()
        this.geminiKey = geminiKey.trim()
        this.assignments = assignments
        val o = JSONObject()
            .put("ollamaUrl", this.ollamaUrl)
            .put("geminiKey", this.geminiKey)
        val map = JSONObject()
        assignments.forEach { (task, modelId) -> map.put(task.name, modelId) }
        o.put("assignments", map)
        file.writeText(o.toString())
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val o = JSONObject(file.readText())
            ollamaUrl = o.optString("ollamaUrl", ollamaUrl)
            geminiKey = o.optString("geminiKey", geminiKey)
            val map = o.optJSONObject("assignments") ?: return
            val next = AiDetector.defaultAssignments().toMutableMap()
            AiTask.entries.forEach { task ->
                val id = map.optString(task.name, "")
                if (id.isNotBlank()) next[task] = id
            }
            assignments = next
        }
    }
}
