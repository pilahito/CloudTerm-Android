package com.pilahito.cloudterm.android.plugins

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object OpenVsx {
    fun search(query: String): List<VsixHit> {
        val q = URLEncoder.encode(query.trim().ifBlank { "theme" }, "UTF-8")
        val url = URL("https://open-vsx.org/api/-/search?query=$q&size=12&sortBy=relevance")
        val raw = get(url)
        val arr = JSONObject(raw).optJSONArray("extensions") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val files = e.optJSONObject("files") ?: JSONObject()
                add(
                    VsixHit(
                        namespace = e.optString("namespace"),
                        name = e.optString("name"),
                        version = e.optString("version"),
                        display = e.optString("displayName").ifBlank { e.optString("name") },
                        description = e.optString("description").take(160),
                        downloadUrl = files.optString("download"),
                    ),
                )
            }
        }.filter { it.downloadUrl.isNotBlank() }
    }

    fun download(url: String, maxBytes: Int = 8_000_000): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "CloudTerm-Android")
        }
        conn.inputStream.use { input ->
            val buf = java.io.ByteArrayOutputStream()
            val tmp = ByteArray(16_384)
            var n: Int
            var total = 0
            while (input.read(tmp).also { n = it } != -1) {
                total += n
                if (total > maxBytes) error("El VSIX pesa demasiado para este editor (máx 8 MB).")
                buf.write(tmp, 0, n)
            }
            return buf.toByteArray()
        }
    }

    private fun get(url: URL): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "CloudTerm-Android")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
