package com.pilahito.cloudterm.android.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pilahito.cloudterm.android.plugins.OpenVsx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AcodeWebPane(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("acode.app — misma tienda, sin servidor nuestro") }
    val scope = rememberCoroutineScope()
    val destDir = remember { File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "acode-downloads").apply { mkdirs() } }

    val web = remember {
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress / 100f
                }
            }
            setDownloadListener(DownloadListener { url, _, contentDisposition, mime, _ ->
                val name = URLUtil.guessFileName(url, contentDisposition, mime).ifBlank { "plugin.zip" }
                status = "Descargando $name…"
                scope.launch {
                    status = withContext(Dispatchers.IO) {
                        runCatching {
                            val bytes = OpenVsx.download(url, maxBytes = 16_000_000)
                            File(destDir, name).writeBytes(bytes)
                            "Guardado: ${File(destDir, name).absolutePath}\nÁbrelo en Acode → Plugins → + → Local."
                        }.getOrElse { it.message ?: "Error de descarga" }
                    }
                }
            })
            loadUrl("https://acode.app")
        }
    }

    Column(modifier.fillMaxSize()) {
        if (progress in 0f..0.99f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(status, modifier = Modifier.padding(8.dp))
        AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
    }
}
