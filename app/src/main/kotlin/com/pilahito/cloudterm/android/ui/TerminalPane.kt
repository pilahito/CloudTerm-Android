package com.pilahito.cloudterm.android.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pilahito.cloudterm.android.ActiveSession

/** Puente JavaScript ⇄ Kotlin. Sus métodos se llaman desde un hilo de fondo del WebView. */
class TerminalBridge(
    private val context: Context,
    private val onReady: (Int, Int) -> Unit,
    private val onInput: (String) -> Unit,
    private val onResize: (Int, Int) -> Unit,
) {
    @JavascriptInterface
    fun ready(cols: Int, rows: Int) = onReady(cols, rows)

    @JavascriptInterface
    fun input(data: String) = onInput(data)

    @JavascriptInterface
    fun resize(cols: Int, rows: Int) = onResize(cols, rows)

    @JavascriptInterface
    fun copy(text: String) {
        if (text.isBlank()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text))
    }
}

private fun sendInput(session: ActiveSession, ctrl: MutableState<Boolean>, data: String) {
    var d = data
    if (ctrl.value && d.length == 1) {
        val c = d[0].uppercaseChar()
        if (c in '@'..'_') d = (c.code - 64).toChar().toString()
        ctrl.value = false
    }
    session.conn.write(d.toByteArray(Charsets.UTF_8))
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalPane(session: ActiveSession, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ctrl = remember { mutableStateOf(false) }

    val web = remember {
        val view = WebView(ctx)
        val bridge = TerminalBridge(
            context = ctx,
            onReady = { cols, rows ->
                session.startShell(cols, rows) { bytes ->
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    view.post { view.evaluateJavascript("writeB64('$b64')", null) }
                }
            },
            onInput = { data -> sendInput(session, ctrl, data) },
            onResize = { cols, rows -> session.conn.resize(cols, rows) },
        )
        view.apply {
            setBackgroundColor(0xFF1E2327.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(false)
            settings.mediaPlaybackRequiresUserGesture = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFocusable = true
            isFocusableInTouchMode = true
            addJavascriptInterface(bridge, "Android")
            loadUrl("file:///android_asset/terminal.html")
        }
    }

    DisposableEffect(Unit) {
        onDispose { web.destroy() }
    }

    Column(modifier) {
        AndroidView(factory = { web }, modifier = Modifier.weight(1f).fillMaxWidth())

        if (session.shellClosed) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("La sesión terminó", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onClose) { Text("Cerrar") }
            }
        }

        ExtraKeys(
            ctrlActive = ctrl.value,
            onCtrl = { ctrl.value = !ctrl.value },
            send = { session.conn.write(it.toByteArray(Charsets.UTF_8)) },
            paste = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(ctx).toString()
                    if (text.isNotEmpty()) session.conn.write(text.toByteArray(Charsets.UTF_8))
                }
            },
            copy = {
                web.evaluateJavascript("copySelection()", null)
            },
        )
    }
}

@Composable
private fun ExtraKeys(
    ctrlActive: Boolean,
    onCtrl: () -> Unit,
    send: (String) -> Unit,
    paste: () -> Unit,
    copy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Key("Esc") { send("\u001b") }
        Key("Tab") { send("\t") }
        Key("Ctrl", active = ctrlActive, onClick = onCtrl)
        Key("↑") { send("\u001b[A") }
        Key("↓") { send("\u001b[B") }
        Key("←") { send("\u001b[D") }
        Key("→") { send("\u001b[C") }
        Key("|") { send("|") }
        Key("~") { send("~") }
        Key("/") { send("/") }
        Key("-") { send("-") }
        Key("Pegar", onClick = paste)
        Key("Copiar", onClick = copy)
    }
}

@Composable
private fun Key(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else AppKey)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
            fontSize = 14.sp,
        )
    }
}
