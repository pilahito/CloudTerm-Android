package com.pilahito.cloudterm.android

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pilahito.cloudterm.android.data.AuthType
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.data.HostStore
import com.pilahito.cloudterm.android.data.SecretVault
import com.pilahito.cloudterm.android.ssh.SshConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.net.UnknownHostException

class HostKeyRequest(val message: String, val decision: CompletableDeferred<Boolean>)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HostStore(app)
    private val vault = SecretVault(app)

    var hosts by mutableStateOf(store.load())
        private set
    var session by mutableStateOf<ActiveSession?>(null)
        private set
    var connecting by mutableStateOf<Host?>(null)
        private set
    var hostKeyRequest by mutableStateOf<HostKeyRequest?>(null)
        private set
    var error by mutableStateOf<String?>(null)

    fun hasKey(hostId: String): Boolean = vault.has("key:$hostId")

    fun saveHost(host: Host, password: String, keyText: String?, passphrase: String) {
        hosts = if (hosts.any { it.id == host.id }) {
            hosts.map { if (it.id == host.id) host else it }
        } else {
            hosts + host
        }
        store.save(hosts)

        if (host.authType == AuthType.PASSWORD) {
            if (password.isNotEmpty()) vault.put("pw:${host.id}", password)
            vault.remove("key:${host.id}")
            vault.remove("pass:${host.id}")
        } else {
            if (!keyText.isNullOrBlank()) {
                vault.put("key:${host.id}", if (keyText.endsWith("\n")) keyText else keyText + "\n")
            }
            if (passphrase.isNotEmpty()) vault.put("pass:${host.id}", passphrase)
            vault.remove("pw:${host.id}")
        }
    }

    fun deleteHost(host: Host) {
        hosts = hosts.filter { it.id != host.id }
        store.save(hosts)
        vault.remove("pw:${host.id}")
        vault.remove("key:${host.id}")
        vault.remove("pass:${host.id}")
    }

    fun connect(host: Host) {
        if (connecting != null || session != null) return
        connecting = host
        val conn = SshConnection(
            host = host,
            password = vault.get("pw:${host.id}"),
            privateKey = vault.get("key:${host.id}"),
            passphrase = vault.get("pass:${host.id}"),
            knownHosts = File(getApplication<Application>().filesDir, "known_hosts"),
        ) { message ->
            val decision = CompletableDeferred<Boolean>()
            hostKeyRequest = HostKeyRequest(message, decision)
            decision.await()
        }
        viewModelScope.launch {
            try {
                conn.connect()
                session = ActiveSession(
                    host,
                    conn,
                    viewModelScope,
                    getApplication<Application>().contentResolver,
                )
                arrancarServicio(host.name)
            } catch (e: Exception) {
                conn.close()
                error = friendly(e)
            } finally {
                connecting = null
            }
        }
    }

    fun answerHostKey(trust: Boolean) {
        hostKeyRequest?.decision?.complete(trust)
        hostKeyRequest = null
    }

    fun disconnect() {
        session?.close()
        session = null
        pararServicio()
    }

    private fun arrancarServicio(nombre: String) {
        val app = getApplication<Application>()
        val intent = Intent(app, SshSessionService::class.java)
            .putExtra(SshSessionService.EXTRA_HOST, nombre)
        if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent) else app.startService(intent)
    }

    private fun pararServicio() {
        val app = getApplication<Application>()
        app.stopService(Intent(app, SshSessionService::class.java).setAction(SshSessionService.ACTION_STOP))
    }

    private fun friendly(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("HostKey has been changed") ->
                "¡La clave del servidor ha cambiado! Puede ser un ataque de intermediario. " +
                    "Si sabes que el servidor se reinstaló, borra los datos de la app para olvidar la clave anterior."
            msg.contains("Auth fail") || msg.contains("Auth cancel") ->
                "Usuario, contraseña o clave incorrectos."
            e is UnknownHostException || msg.contains("UnknownHost") ->
                "No se encuentra el servidor. Revisa la dirección."
            msg.contains("timeout", ignoreCase = true) ->
                "El servidor no responde (tiempo agotado)."
            msg.contains("Connection refused", ignoreCase = true) ->
                "Conexión rechazada. Revisa el puerto."
            msg.contains("reject HostKey") || msg.contains("HostKey") ->
                "Conexión cancelada: clave del servidor no aceptada."
            else -> msg.ifBlank { e.javaClass.simpleName }
        }
    }

    override fun onCleared() {
        session?.close()
        pararServicio()
    }
}
