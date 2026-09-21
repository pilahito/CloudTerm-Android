package com.pilahito.cloudterm.android.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        ollama: Boolean,
    ): String {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isBlank()) return "Falta la URL de la API."
        return if (ollama) ollamaChat(base, model, prompt) else openAiChat(base, apiKey, model, prompt)
    }

    private fun openAiChat(base: String, apiKey: String, model: String, prompt: String): String {
        val url = URL("$base/chat/completions")
        val body = JSONObject()
            .put("model", model.ifBlank { "gpt-4o-mini" })
            .put("stream", false)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", prompt)),
            )
        val raw = post(url, body.toString(), apiKey)
        val json = JSONObject(raw)
        if (json.has("error")) {
            return json.optJSONObject("error")?.optString("message") ?: raw.take(300)
        }
        return json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
            .ifBlank { raw.take(400) }
    }

    private fun ollamaChat(base: String, model: String, prompt: String): String {
        val url = URL("$base/api/chat")
        val body = JSONObject()
            .put("model", model.ifBlank { "llama3.2:1b" })
            .put("stream", false)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", prompt)),
            )
        val raw = post(url, body.toString(), null)
        val json = JSONObject(raw)
        return json.optJSONObject("message")?.optString("content").orEmpty().ifBlank { raw.take(400) }
    }

    private fun post(url: URL, body: String, bearer: String?): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }
}
