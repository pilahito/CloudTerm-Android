package com.pilahito.cloudterm.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.pilahito.cloudterm.android.data.Host

private const val VSCODROID = "com.vscodroid"
private const val PLAY = "https://play.google.com/store/apps/details?id=com.vscodroid"
private const val GITHUB = "https://github.com/rmyndharis/VSCodroid"
private const val PREF = "cloudterm_ide"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VsCodroidPane(host: Host, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREF, 0) }
    var url by remember { mutableStateOf(prefs.getString("url", "") ?: "") }
    var loaded by remember { mutableStateOf(prefs.getString("url", "").orEmpty().isNotBlank()) }
    val installed = remember {
        try {
            ctx.packageManager.getPackageInfo(VSCODROID, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
    val vscodroidOk = Build.VERSION.SDK_INT >= 33 && Build.SUPPORTED_ABIS.any { it.contains("64") }

    val web = remember {
        WebView(ctx).apply {
            setBackgroundColor(0xFF1E1E1E.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            if (Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            overScrollMode = View.OVER_SCROLL_NEVER
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
    }
    DisposableEffect(Unit) { onDispose { /* keep webview for tab switches */ } }

    Column(modifier.fillMaxSize()) {
        if (!loaded) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("VS que sí funciona aquí", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Opción A — code-server o vscode.dev en este WebView (plugins JS, el que cabe en este teléfono).\n" +
                        "Opción B — VSCodroid nativo: Android 13+ y ARM64, ~1 GB. En un Snapdragon 660 suele no instalarse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Servidor CloudTerm: ${host.username}@${host.hostname}:${host.port}",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL de code-server / vscode.dev") },
                    placeholder = { Text("http://192.168.1.10:8080  o  https://vscode.dev") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val u = url.trim().ifBlank { "https://vscode.dev" }
                        url = u
                        prefs.edit().putString("url", u).apply()
                        loaded = true
                        web.loadUrl(u)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Abrir workbench aquí") }

                if (installed) {
                    Button(
                        onClick = {
                            ctx.packageManager.getLaunchIntentForPackage(VSCODROID)?.let { ctx.startActivity(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Abrir VSCodroid instalado") }
                } else {
                    OutlinedButton(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(if (vscodroidOk) PLAY else GITHUB))) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (vscodroidOk) "Instalar VSCodroid" else "VSCodroid (puede no ir en este móvil)")
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { loaded = false }) { Text("Cambiar URL") }
                OutlinedButton(onClick = { if (web.canGoBack()) web.goBack() }) { Text("Atrás") }
            }
            AndroidView(factory = {
                if (web.url.isNullOrBlank()) web.loadUrl(url.ifBlank { "https://vscode.dev" })
                web
            }, modifier = Modifier.fillMaxSize())
        }
    }
}
