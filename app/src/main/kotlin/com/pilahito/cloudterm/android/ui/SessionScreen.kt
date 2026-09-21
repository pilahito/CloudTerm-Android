package com.pilahito.cloudterm.android.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilahito.cloudterm.android.ActiveSession
import com.pilahito.cloudterm.android.AppViewModel
import com.pilahito.cloudterm.android.data.Protocol

@Composable
fun SessionScreen(vm: AppViewModel, session: ActiveSession) {
    val filesFirst = session.host.protocol != Protocol.SSH
    var tab by remember { mutableIntStateOf(if (filesFirst) 1 else 0) }
    var confirmExit by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(session.notice) {
        val message = session.notice
        if (message != null) {
            session.notice = null
            snackbar.showSnackbar(message)
        }
    }

    LaunchedEffect(session.editorOpen) {
        if (session.editorOpen) tab = 2
    }

    BackHandler {
        when {
            tab == 1 && session.path.isNotEmpty() && session.path != "/" -> session.up()
            else -> confirmExit = true
        }
    }

    fun hideKeyboard() {
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    val tabs = listOf("Terminal", "Archivos", "Código", "IA", "Agents")

    Box(Modifier.fillMaxSize().background(CtBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CtSurface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { confirmExit = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar sesión", tint = CtText)
                }
                HexLogo(size = 26.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        session.host.name,
                        color = CtText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Text(
                        session.host.protocol.label,
                        color = CtAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CtAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { vm.checkForUpdate(silent = false) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("Actualizar", color = CtAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CtSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                tabs.forEachIndexed { i, label ->
                    val selected = tab == i
                    val shown = if (i == 2 && session.editorDirty) "$label ·" else label
                    Column(
                        Modifier
                            .clickable {
                                tab = i
                                if (i == 1) hideKeyboard()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            shown,
                            color = if (selected) CtAccent else CtMuted,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .height(2.dp)
                                .width(if (selected) 28.dp else 0.dp)
                                .clip(CircleShape)
                                .background(CtAccent),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(CtAccent.copy(alpha = 0.18f)))

            Box(Modifier.weight(1f).fillMaxWidth().background(CtBg)) {
                if (tab == 0) {
                    TerminalPane(session, onClose = { confirmExit = true }, modifier = Modifier.fillMaxSize())
                }
                if (tab == 1) {
                    FilesPane(
                        session,
                        Modifier
                            .fillMaxSize()
                            .background(CtBg)
                            .pointerInput(Unit) { detectTapGestures { } },
                    )
                }
                if (tab == 2) {
                    EditorPane(session, Modifier.fillMaxSize())
                }
                if (tab == 3) {
                    AiPane(
                        vm = vm,
                        fileName = session.editorName.ifBlank { null },
                        fileBody = session.editorText.ifBlank { null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (tab == 4) {
                    PixelAgentsPane(session, Modifier.fillMaxSize())
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Cerrar sesión", color = CtText) },
            text = { Text("Se cerrará la conexión con ${session.host.name}.", color = CtMuted) },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; vm.disconnect() }) {
                    Text("Cerrar", color = CtPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Seguir", color = CtAccent) }
            },
            containerColor = CtCard,
        )
    }
}
