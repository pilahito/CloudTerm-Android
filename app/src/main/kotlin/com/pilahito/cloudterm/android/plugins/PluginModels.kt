package com.pilahito.cloudterm.android.plugins

data class CtPlugin(
    val id: String,
    val name: String,
    val publisher: String,
    val version: String,
    val summary: String,
    val kind: PluginKind,
    val enabled: Boolean,
    val source: PluginSource,
    val supports: List<String>,
)

enum class PluginKind { BUNDLED, THEME, SNIPPETS, NEEDS_HOST }

enum class PluginSource { CLOUDTERM, OPENVSX, VSIX }

data class VsixHit(
    val namespace: String,
    val name: String,
    val version: String,
    val display: String,
    val description: String,
    val downloadUrl: String,
) {
    val id: String get() = "$namespace.$name"
}
