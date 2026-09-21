package com.pilahito.cloudterm.android.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.pilahito.cloudterm.android.ActiveSession
import com.pilahito.cloudterm.android.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: AppViewModel, session: ActiveSession) {
    var tab by remember { mutableIntStateOf(0) }
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

    BackHandler {
        if (tab == 1 && session.path.isNotEmpty() && session.path != "/") session.up()
        else confirmExit = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(session.host.name) },
                    navigationIcon = {
                        IconButton(onClick = { confirmExit = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar sesión")
                        }
                    },
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Terminal") })
                    Tab(
                        selected = tab == 1,
                        onClick = {
                            tab = 1
                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(view.windowToken, 0)
                        },
                        text = { Text("Archivos") },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            // El terminal siempre está compuesto: cambiar de pestaña no cierra el shell.
            TerminalPane(session, onClose = { confirmExit = true }, modifier = Modifier.fillMaxSize())
            if (tab == 1) {
                FilesPane(
                    session,
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Se cerrará la conexión con ${session.host.name}.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; vm.disconnect() }) { Text("Cerrar") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir") } },
        )
    }
}
