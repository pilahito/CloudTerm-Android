package com.pilahito.cloudterm.android.plugins

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AcodePlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val price: Int,
    val downloads: Int,
) {
    val free: Boolean get() = price <= 0
    val downloadUrl: String
        get() = "https://acode.app/api/plugin/download/$id?device=cloudterm&package=com.foxdebug.acodefree&version=1"
}

object AcodeMarket {
    private const val BASE = "https://acode.app/api"

    fun search(query: String): List<AcodePlugin> {
        val q = query.trim()
        val path = if (q.isBlank()) {
            "$BASE/plugins?explore=random&page=1&limit=20&supported_editor=cm"
        } else {
            "$BASE/plugins?name=${URLEncoder.encode(q, "UTF-8")}&supported_editor=cm"
        }
        val raw = get(URL(path))
        val arr = when {
            raw.trim().startsWith("[") -> JSONArray(raw)
            else -> JSONObject(raw).optJSONArray("plugins") ?: JSONArray()
        }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val author = o.optJSONObject("author")?.optString("name").orEmpty()
                .ifBlank { o.optString("author") }
            AcodePlugin(
                id = o.optString("id"),
                name = o.optString("name").ifBlank { o.optString("id") },
                version = o.optString("version"),
                author = author,
                description = o.optString("description").ifBlank { o.optString("readme") }.take(180),
                price = o.optInt("price", 0),
                downloads = o.optInt("downloads", 0),
            )
        }.filter { it.id.isNotBlank() }
    }

    fun download(plugin: AcodePlugin): ByteArray {
        if (!plugin.free) error("Plugin de pago. Cómpralo dentro de Acode.")
        return OpenVsx.download(plugin.downloadUrl, maxBytes = 12_000_000)
    }

    private fun get(url: URL): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "CloudTerm-Android")
            instanceFollowRedirects = true
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
