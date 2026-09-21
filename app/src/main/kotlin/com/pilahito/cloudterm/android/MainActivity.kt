package com.pilahito.cloudterm.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.ui.CloudTermTheme
import com.pilahito.cloudterm.android.ui.HostsScreen
import com.pilahito.cloudterm.android.ui.SessionScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            CloudTermTheme {
                App(vm)
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val s = vm.session
    if (s == null) HostsScreen(vm) else SessionScreen(vm, s)

    val connecting = vm.connecting
    if (connecting != null && vm.hostKeyRequest == null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Conectando…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(connecting.name)
                }
            },
        )
    }

    vm.hostKeyRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { vm.answerHostKey(false) },
            title = { Text("Servidor desconocido") },
            text = {
                Text(
                    request.message +
                        "\n\nComprueba que la huella coincide con la de tu servidor antes de continuar.",
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.answerHostKey(true) }) { Text("Confiar y conectar") }
            },
            dismissButton = {
                TextButton(onClick = { vm.answerHostKey(false) }) { Text("Cancelar") }
            },
        )
    }

    vm.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.error = null },
            title = { Text("No se pudo conectar") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.error = null }) { Text("Aceptar") } },
        )
    }
}
