package com.pilahito.cloudterm.android.plugins

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

class PluginStore(private val context: Context) {
    private val root = File(context.filesDir, "plugins").apply { mkdirs() }
    private val enabledFile = File(root, "enabled.json")

    fun bundled(): List<CtPlugin> = listOf(
        CtPlugin(
            id = "cloudterm.format",
            name = "Formatear documento",
            publisher = "CloudTerm",
            version = "1.0",
            summary = "Indenta el archivo abierto (plugin nativo).",
            kind = PluginKind.BUNDLED,
            enabled = isEnabled("cloudterm.format", default = true),
            source = PluginSource.CLOUDTERM,
            supports = listOf("format"),
        ),
        CtPlugin(
            id = "cloudterm.comment",
            name = "Comentar líneas",
            publisher = "CloudTerm",
            version = "1.0",
            summary = "Antepone // a cada línea (plugin nativo).",
            kind = PluginKind.BUNDLED,
            enabled = isEnabled("cloudterm.comment", default = true),
            source = PluginSource.CLOUDTERM,
            supports = listOf("comment"),
        ),
        CtPlugin(
            id = "cloudterm.snippets",
            name = "Snippets básicos",
            publisher = "CloudTerm",
            version = "1.0",
            summary = "Inserta plantillas fn / for / html5.",
            kind = PluginKind.BUNDLED,
            enabled = isEnabled("cloudterm.snippets", default = true),
            source = PluginSource.CLOUDTERM,
            supports = listOf("snippets"),
        ),
    )

    fun installed(): List<CtPlugin> {
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val meta = File(dir, "meta.json")
            if (!meta.exists()) return@mapNotNull null
            runCatching {
                val o = JSONObject(meta.readText())
                val id = o.getString("id")
                CtPlugin(
                    id = id,
                    name = o.optString("name", id),
                    publisher = o.optString("publisher", "Open VSX"),
                    version = o.optString("version", ""),
                    summary = o.optString("summary", ""),
                    kind = PluginKind.valueOf(o.optString("kind", PluginKind.NEEDS_HOST.name)),
                    enabled = isEnabled(id, default = true),
                    source = PluginSource.OPENVSX,
                    supports = jsonStrings(o.optJSONArray("supports")),
                )
            }.getOrNull()
        }
    }

    fun all(): List<CtPlugin> = bundled() + installed()

    fun setEnabled(id: String, on: Boolean) {
        val o = if (enabledFile.exists()) JSONObject(enabledFile.readText()) else JSONObject()
        o.put(id, on)
        enabledFile.writeText(o.toString())
    }

    fun isEnabled(id: String, default: Boolean = true): Boolean {
        if (!enabledFile.exists()) return default
        val o = JSONObject(enabledFile.readText())
        return if (o.has(id)) o.optBoolean(id) else default
    }

    fun remove(id: String) {
        File(root, safe(id)).deleteRecursively()
    }

    fun installVsix(hit: VsixHit, bytes: ByteArray): CtPlugin {
        val files = unzip(bytes)
        val manifestPath = files.keys.firstOrNull { it.endsWith("package.json") && it.contains("extension") }
            ?: files.keys.firstOrNull { it.endsWith("package.json") }
            ?: error("El VSIX no trae package.json")
        val pkg = JSONObject(files.getValue(manifestPath).toString(Charsets.UTF_8))
        val contributes = pkg.optJSONObject("contributes") ?: JSONObject()
        val hasTheme = (contributes.optJSONArray("themes")?.length() ?: 0) > 0
        val hasSnippets = (contributes.optJSONArray("snippets")?.length() ?: 0) > 0
        val hasMain = pkg.optString("main").isNotBlank() || pkg.optString("browser").isNotBlank()
        val kind = when {
            hasTheme -> PluginKind.THEME
            hasSnippets -> PluginKind.SNIPPETS
            else -> PluginKind.NEEDS_HOST
        }
        val supports = buildList {
            if (hasTheme) add("theme")
            if (hasSnippets) add("snippets")
            if (hasMain) add("vscode-host")
        }
        val dir = File(root, safe(hit.id)).apply {
            deleteRecursively()
            mkdirs()
        }
        File(dir, "meta.json").writeText(
            JSONObject()
                .put("id", hit.id)
                .put("name", hit.display)
                .put("publisher", hit.namespace)
                .put("version", hit.version)
                .put("summary", when (kind) {
                    PluginKind.THEME -> "Tema extraído del VSIX."
                    PluginKind.SNIPPETS -> "Snippets extraídos del VSIX."
                    else -> "Requiere el host de VS Code (pestaña VS / code-server). No se ejecuta JS aquí."
                })
                .put("kind", kind.name)
                .put("supports", JSONArray(supports))
                .toString(),
        )
        File(dir, "package.json").writeText(pkg.toString())
        if (hasTheme) {
            val themes = contributes.getJSONArray("themes")
            val rel = themes.getJSONObject(0).optString("path")
            val themeFile = files.entries.firstOrNull { it.key.replace('\\', '/').endsWith(rel.removePrefix("./")) }
            if (themeFile != null) File(dir, "theme.json").writeBytes(themeFile.value)
        }
        return installed().first { it.id == hit.id }
    }

    fun themeCss(id: String): String? {
        val f = File(File(root, safe(id)), "theme.json")
        if (!f.exists()) return null
        val colors = JSONObject(f.readText()).optJSONObject("colors") ?: return null
        val bg = colors.optString("editor.background", "#0f1419")
        val fg = colors.optString("editor.foreground", "#e6edf3")
        return "document.body.style.background='$bg';var el=document.querySelector('.CodeMirror');if(el){el.style.background='$bg';el.style.color='$fg';}"
    }

    fun enabledActions(): List<String> =
        all().filter { it.enabled }.flatMap { it.supports }.distinct()

    private fun isEnabled(id: String): Boolean = isEnabled(id, true)

    private fun jsonStrings(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }

    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.isDirectory) continue
                if (e.size > 2_000_000) continue
                out[e.name.replace('\\', '/')] = zip.readBytes()
            }
        }
        return out
    }
}
