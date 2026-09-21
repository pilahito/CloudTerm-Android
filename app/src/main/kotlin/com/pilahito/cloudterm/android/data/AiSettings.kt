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
    var apiUrl: String = ""
        private set
    var apiKey: String = ""
        private set
    var apiModel: String = "gpt-4o-mini"
        private set
    var assignments: Map<AiTask, String> = AiDetector.defaultAssignments()
        private set

    init {
        load()
    }

    fun save(
        ollamaUrl: String = this.ollamaUrl,
        geminiKey: String = this.geminiKey,
        apiUrl: String = this.apiUrl,
        apiKey: String = this.apiKey,
        apiModel: String = this.apiModel,
        assignments: Map<AiTask, String> = this.assignments,
    ) {
        this.ollamaUrl = ollamaUrl.trim()
        this.geminiKey = geminiKey.trim()
        this.apiUrl = apiUrl.trim()
        this.apiKey = apiKey.trim()
        this.apiModel = apiModel.trim().ifBlank { "gpt-4o-mini" }
        this.assignments = assignments
        val o = JSONObject()
            .put("ollamaUrl", this.ollamaUrl)
            .put("geminiKey", this.geminiKey)
            .put("apiUrl", this.apiUrl)
            .put("apiKey", this.apiKey)
            .put("apiModel", this.apiModel)
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
            apiUrl = o.optString("apiUrl", apiUrl)
            apiKey = o.optString("apiKey", apiKey)
            apiModel = o.optString("apiModel", apiModel)
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
