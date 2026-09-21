package com.pilahito.cloudterm.android.ui

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocalEditorPane(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val file = remember { File(ctx.filesDir, "scratch.kt") }
    var name by remember { mutableStateOf(file.name) }
    var status by remember { mutableStateOf("Editor CloudTerm · archivo local") }
    var ready = remember { booleanArrayOf(false) }

    val web = remember {
        WebView(ctx).apply {
            setBackgroundColor(0xFF0B1214.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            addJavascriptInterface(
                object {
                    @JavascriptInterface fun ready() {
                        ready[0] = true
                        val text = if (file.exists()) file.readText() else "fun main() {\n    println(\"CloudTerm\")\n}\n"
                        post {
                            val n = JSONObject.quote(name)
                            val t = JSONObject.quote(text)
                            evaluateJavascript("setFile($n, $t, languageOf($n))", null)
                        }
                    }
                    @JavascriptInterface fun changed(text: String) {
                        status = "sin guardar · ${text.length} chars"
                    }
                    @JavascriptInterface fun save(text: String) {
                        file.writeText(text)
                        post { status = "Guardado en ${file.name} (editor CloudTerm)" }
                    }
                },
                "Android",
            )
            loadUrl("file:///android_asset/editor.html")
        }
    }
    DisposableEffect(Unit) { onDispose { web.destroy() } }
    Column(modifier.fillMaxSize().background(AppBackground)) {
        Text(status, color = Mint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        AndroidView(factory = { web }, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}
