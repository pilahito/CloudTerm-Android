package com.pilahito.cloudterm.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pilahito.cloudterm.android.data.Host

private const val VSCODROID = "com.vscodroid"
private const val PLAY = "https://play.google.com/store/apps/details?id=com.vscodroid"
private const val GITHUB = "https://github.com/rmyndharis/VSCodroid"
private const val OPENVSX = "https://open-vsx.org"

@Composable
fun VsCodroidPane(host: Host, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val installed = rememberInstalled(VSCODROID)

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("VS Code en el teléfono", style = MaterialTheme.typography.titleMedium)
        Text(
            "VSCodroid es un VS Code de verdad (Code-OSS + terminal + extensiones Open VSX). " +
                "No cabe dentro de CloudTerm (~1 GB y Android 13 ARM64). CloudTerm abre esa app " +
                "y sigue siendo el cliente SSH/FTP.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Servidor actual: ${host.username}@${host.hostname}:${host.port} (${host.protocol.label})",
            style = MaterialTheme.typography.bodySmall,
        )
        if (installed) {
            Button(
                onClick = {
                    val launch = ctx.packageManager.getLaunchIntentForPackage(VSCODROID)
                    if (launch != null) ctx.startActivity(launch)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Abrir VSCodroid") }
        } else {
            Button(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Instalar VSCodroid (Play)") }
            OutlinedButton(
                onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB))) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("GitHub / APK") }
        }
        OutlinedButton(
            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OPENVSX))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Extensiones Open VSX") }
        Text(
            "Plugins: en VSCodroid, vista Extensiones. Las que son JavaScript funcionan; " +
                "las que traen binario de escritorio Linux no. El terminal de VSCodroid es bash local, " +
                "no el SSH de CloudTerm. Para el mismo servidor usa la extensión Remote SSH dentro de VSCodroid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun rememberInstalled(pkg: String): Boolean {
    val ctx = LocalContext.current
    return try {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Exception) {
        false
    }
}
