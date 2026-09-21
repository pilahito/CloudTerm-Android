package com.pilahito.cloudterm.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.pilahito.cloudterm.android.auth.HostFingerprint
import com.pilahito.cloudterm.android.ui.CloudTermTheme
import com.pilahito.cloudterm.android.ui.HostsScreen
import com.pilahito.cloudterm.android.ui.LockScreen
import com.pilahito.cloudterm.android.ui.SessionScreen

class MainActivity : FragmentActivity() {
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
    var unlocked by rememberSaveable { mutableStateOf(!vm.biometricEnabled) }
    if (vm.biometricEnabled && !unlocked) {
        LockScreen(onUnlocked = { unlocked = true })
        return
    }

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
        val fp = HostFingerprint.extract(request.message)
        AlertDialog(
            onDismissRequest = { vm.answerHostKey(false) },
            title = { Text("Detector de huella SSH") },
            text = {
                Text(
                    buildString {
                        append("Comprueba la huella del servidor antes de confiar.\n\n")
                        if (fp != null) {
                            append("Huella detectada:\n")
                            append(fp)
                            append("\n\n")
                        }
                        append("No se publica ni se sube. Solo se guarda en este teléfono si aceptas.")
                    },
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

    val busy = vm.updateBusy
    if (busy != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Actualización") },
            text = {
                Column {
                    Text(busy)
                    if (vm.updateProgress in 0..100) {
                        LinearProgressIndicator(
                            progress = { vm.updateProgress / 100f },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text("${vm.updateProgress} %", modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
        )
    }

    val info = vm.update
    if (busy == null && info != null && info.newer) {
        AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Hay una versión nueva") },
            text = {
                Text("Instalada: ${vm.installedVersion}\nNueva: ${info.tag}\n\n${info.notes}")
            },
            confirmButton = {
                TextButton(onClick = { vm.downloadAndInstall() }) { Text("Descargar e instalar") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissUpdate() }) { Text("Ahora no") }
            },
        )
    }

    vm.updateNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.updateNotice = null },
            title = { Text("Actualizar") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.updateNotice = null }) { Text("Aceptar") } },
        )
    }
}
