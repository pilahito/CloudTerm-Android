package com.pilahito.cloudterm.android.ui

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pilahito.cloudterm.android.ActiveSession
import com.pilahito.cloudterm.android.plugins.PluginKind
import com.pilahito.cloudterm.android.plugins.PluginStore
import org.json.JSONObject

private class EditorBridge(
    private val onReady: () -> Unit,
    private val onChanged: (String) -> Unit,
    private val onSave: (String) -> Unit,
) {
    @JavascriptInterface
    fun ready() = onReady()

    @JavascriptInterface
    fun changed(text: String) = onChanged(text)

    @JavascriptInterface
    fun save(text: String) = onSave(text)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorPane(session: ActiveSession, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val plugins = remember { PluginStore(ctx) }
    var ready = remember { booleanArrayOf(false) }

    val web = remember {
        WebView(ctx).apply {
            setBackgroundColor(0xFF0F1419.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            addJavascriptInterface(
                EditorBridge(
                    onReady = {
                        ready[0] = true
                        post {
                            pushFile(this, session)
                            applyTheme(this, plugins)
                        }
                    },
                    onChanged = { session.onEditorChange(it) },
                    onSave = {
                        session.onEditorChange(it)
                        session.saveEditor()
                    },
                ),
                "Android",
            )
            loadUrl("file:///android_asset/editor.html")
        }
    }

    LaunchedEffect(session.editorPath, session.editorLoading, session.editorText) {
        if (ready[0] && !session.editorLoading) web.post {
            pushFile(web, session)
            applyTheme(web, plugins)
        }
    }

    DisposableEffect(Unit) {
        onDispose { web.destroy() }
    }

    Box(modifier.background(AppBackground)) {
        if (!session.editorOpen) {
            Text(
                "Abre un archivo de código desde Archivos → Editar código.\nPlugins: pestaña Plugins (nativos + temas Open VSX).",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
            if (session.editorLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun pushFile(web: WebView, session: ActiveSession) {
    val name = JSONObject.quote(session.editorName)
    val text = JSONObject.quote(session.editorText)
    val lang = JSONObject.quote(
        session.editorName.substringAfterLast('.', "").uppercase(),
    )
    web.evaluateJavascript("setFile($name, $text, languageOf($name) || $lang)", null)
}

private fun applyTheme(web: WebView, plugins: PluginStore) {
    val theme = plugins.installed().firstOrNull { it.enabled && it.kind == PluginKind.THEME } ?: return
    val js = plugins.themeCss(theme.id) ?: return
    web.evaluateJavascript(js, null)
}
