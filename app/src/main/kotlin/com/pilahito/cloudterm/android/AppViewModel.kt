package com.pilahito.cloudterm.android

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pilahito.cloudterm.android.ai.AiTask
import com.pilahito.cloudterm.android.data.AiSettings
import com.pilahito.cloudterm.android.data.AuthType
import com.pilahito.cloudterm.android.data.Host
import com.pilahito.cloudterm.android.data.HostStore
import com.pilahito.cloudterm.android.data.LockSettings
import com.pilahito.cloudterm.android.data.Protocol
import com.pilahito.cloudterm.android.data.SecretVault
import com.pilahito.cloudterm.android.ftp.FtpConnection
import com.pilahito.cloudterm.android.net.RemoteFs
import com.pilahito.cloudterm.android.ssh.SshConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.net.UnknownHostException

class HostKeyRequest(val message: String, val decision: CompletableDeferred<Boolean>)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HostStore(app)
    private val vault = SecretVault(app)
    private val aiStore = AiSettings(app)
    private val lock = LockSettings(app)

    var hosts by mutableStateOf(store.load())
        private set
    var session by mutableStateOf<ActiveSession?>(null)
        private set
    var connecting by mutableStateOf<Host?>(null)
        private set
    var hostKeyRequest by mutableStateOf<HostKeyRequest?>(null)
        private set
    var error by mutableStateOf<String?>(null)
    var aiAssignments by mutableStateOf(aiStore.assignments)
        private set
    var biometricEnabled by mutableStateOf(lock.biometricEnabled)
        private set

    fun setBiometric(enabled: Boolean) {
        lock.biometricEnabled = enabled
        biometricEnabled = enabled
    }

    fun hasKey(hostId: String): Boolean = vault.has("key:$hostId")
    fun hasTotp(hostId: String): Boolean = vault.has("totp:$hostId")

    fun assignModel(task: AiTask, modelId: String) {
        val next = aiAssignments.toMutableMap()
        next[task] = modelId
        aiAssignments = next
        aiStore.save(assignments = next)
    }

    fun saveHost(host: Host, password: String, keyText: String?, passphrase: String, totpSecret: String) {
        hosts = if (hosts.any { it.id == host.id }) {
            hosts.map { if (it.id == host.id) host else it }
        } else {
            hosts + host
        }
        store.save(hosts)

        if (host.authType == AuthType.PASSWORD || host.protocol == Protocol.FTP || host.protocol == Protocol.FTPS) {
            if (password.isNotEmpty()) vault.put("pw:${host.id}", password)
            if (host.protocol == Protocol.FTP || host.protocol == Protocol.FTPS) {
                vault.remove("key:${host.id}")
                vault.remove("pass:${host.id}")
            }
        } else {
            if (!keyText.isNullOrBlank()) {
                vault.put("key:${host.id}", if (keyText.endsWith("\n")) keyText else keyText + "\n")
            }
            if (passphrase.isNotEmpty()) vault.put("pass:${host.id}", passphrase)
            vault.remove("pw:${host.id}")
        }
        if (totpSecret.isNotBlank()) vault.put("totp:${host.id}", totpSecret.replace(" ", ""))
        if (!host.totpEnabled) vault.remove("totp:${host.id}")
    }

    fun deleteHost(host: Host) {
        hosts = hosts.filter { it.id != host.id }
        store.save(hosts)
        vault.remove("pw:${host.id}")
        vault.remove("key:${host.id}")
        vault.remove("pass:${host.id}")
        vault.remove("totp:${host.id}")
    }

    fun connect(host: Host) {
        if (connecting != null || session != null) return
        connecting = host
        val conn: RemoteFs = when (host.protocol) {
            Protocol.FTP, Protocol.FTPS -> FtpConnection(host, vault.get("pw:${host.id}"))
            Protocol.SSH, Protocol.SFTP -> SshConnection(
                host = host,
                password = vault.get("pw:${host.id}"),
                privateKey = vault.get("key:${host.id}"),
                passphrase = vault.get("pass:${host.id}"),
                knownHosts = File(getApplication<Application>().filesDir, "known_hosts"),
                totpSecret = vault.get("totp:${host.id}"),
            ) { message ->
                val decision = CompletableDeferred<Boolean>()
                hostKeyRequest = HostKeyRequest(message, decision)
                decision.await()
            }
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
                error = friendly(e, host)
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

    private fun friendly(e: Exception, host: Host? = null): String {
        val msg = e.message.orEmpty()
        val port = host?.port ?: 22
        return when {
            host?.protocol == Protocol.FTP || host?.protocol == Protocol.FTPS ->
                msg.ifBlank { "No se pudo conectar por ${host.protocol.label} al puerto $port." }
            port in setOf(21, 989, 990) || msg.contains("FTP/FTPS") ->
                "Ese puerto es FTPS/FTP. Cambia el protocolo del servidor a FTP o FTPS."
            msg.contains("HostKey has been changed") ->
                "La huella del servidor ha cambiado. Si lo reinstalaste, borra los datos de la app."
            msg.contains("Auth fail") || msg.contains("Auth cancel") ->
                "Usuario, contraseña, clave o 2FA incorrectos."
            e is UnknownHostException || msg.contains("UnknownHost") ->
                "No se encuentra el servidor. Revisa la dirección."
            msg.contains("timeout", ignoreCase = true) ->
                "El servidor no responde (tiempo agotado)."
            msg.contains("Connection refused", ignoreCase = true) || msg.contains("rechazada") ->
                "Conexión rechazada en el puerto $port."
            msg.contains("identificación inválida") || msg.contains("invalid identification") ->
                "Ese puerto no habla el protocolo SSH."
            msg.contains("reject HostKey") || msg.contains("HostKey") ->
                "Conexión cancelada: huella del servidor no aceptada."
            else -> msg.ifBlank { e.javaClass.simpleName }
        }
    }

    override fun onCleared() {
        session?.close()
        pararServicio()
    }
}
