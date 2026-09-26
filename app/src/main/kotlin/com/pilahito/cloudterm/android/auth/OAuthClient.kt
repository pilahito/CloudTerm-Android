package com.pilahito.cloudterm.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.pilahito.cloudterm.android.data.CloudAccount
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Inicio de sesión en el teléfono.
 *
 * Google usa el retorno a `http://127.0.0.1:<puerto>`, sin ruta: una aplicación
 * de escritorio rechaza `/callback`. GitHub, si no hay secreto, enseña un código
 * en github.com/login/device. Con secreto, vuelve a `/callback`.
 */
class OAuthClient(private val context: Context) {
    @Volatile private var stopped = false
    private var server: ServerSocket? = null

    fun cancel() {
        stopped = true
        runCatching { server?.close() }
    }

    fun signInGoogle(clientId: String): CloudAccount {
        stopped = false
        val verifier = verifier()
        val state = verifier()
        val code = loopback(state, path = "/") { port ->
            val redirect = "http://127.0.0.1:$port"
            open(
                "https://accounts.google.com/o/oauth2/v2/auth?" + form(
                    mapOf(
                        "client_id" to clientId,
                        "redirect_uri" to redirect,
                        "response_type" to "code",
                        "scope" to "openid email profile https://www.googleapis.com/auth/drive.appdata",
                        "code_challenge" to challenge(verifier),
                        "code_challenge_method" to "S256",
                        "state" to state,
                        "access_type" to "offline",
                        "prompt" to "consent",
                    ),
                ),
            )
            redirect
        }
        val token = postForm(
            "https://oauth2.googleapis.com/token",
            mapOf(
                "client_id" to clientId,
                "code" to code,
                "code_verifier" to verifier,
                "grant_type" to "authorization_code",
                "redirect_uri" to lastRedirect,
            ),
        )
        val access = token.getString("access_token")
        val profile = getJson("https://openidconnect.googleapis.com/v1/userinfo", access)
        return CloudAccount(
            provider = "google",
            name = profile.optString("name"),
            email = profile.optString("email"),
        ).also { deliveredToken = access }
    }

    fun signInGithub(
        clientId: String,
        clientSecret: String?,
        onUserCode: (String) -> Unit,
    ): CloudAccount {
        stopped = false
        val id = clientId.ifBlank { GithubApp.CLIENT_ID }
        val secret = clientSecret?.takeIf { it.isNotBlank() }
            ?: if (id == GithubApp.CLIENT_ID) GithubApp.CLIENT_SECRET else null
        val access = if (!secret.isNullOrBlank()) {
            githubBrowser(id, secret)
        } else {
            githubDevice(id, onUserCode)
        }
        val profile = getJson(
            "https://api.github.com/user",
            access,
            mapOf("Accept" to "application/vnd.github+json", "User-Agent" to "CloudTerm"),
        )
        val login = profile.optString("login")
        val name = profile.optString("name").ifBlank { login }
        var email = profile.optString("email")
        if (email.isBlank()) email = githubEmail(access)
        return CloudAccount(provider = "github", name = name, email = email).also { deliveredToken = access }
    }

    var deliveredToken: String = ""
        private set

    private var lastRedirect: String = ""

    private fun githubBrowser(clientId: String, secret: String): String {
        val verifier = verifier()
        val state = verifier()
        val builtin = clientId == GithubApp.CLIENT_ID
        val code = loopback(state, path = "/callback", preferredPort = if (builtin) GithubApp.CALLBACK_PORT else 0) { port ->
            val redirect = "http://127.0.0.1:$port/callback"
            val fields = mutableMapOf(
                "client_id" to clientId,
                "redirect_uri" to redirect,
                "state" to state,
                "code_challenge" to challenge(verifier),
                "code_challenge_method" to "S256",
            )
            if (!builtin) fields["scope"] = GITHUB_SCOPES
            open("https://github.com/login/oauth/authorize?" + form(fields))
            redirect
        }
        val token = postForm(
            "https://github.com/login/oauth/access_token",
            mapOf(
                "client_id" to clientId,
                "client_secret" to secret,
                "code" to code,
                "redirect_uri" to lastRedirect,
                "grant_type" to "authorization_code",
                "code_verifier" to verifier,
            ),
            mapOf("Accept" to "application/json"),
        )
        return token.getString("access_token")
    }

    private fun githubDevice(clientId: String, onUserCode: (String) -> Unit): String {
        val started = try {
            postForm(
                "https://github.com/login/device/code",
                mapOf("client_id" to clientId, "scope" to GITHUB_SCOPES),
                mapOf("Accept" to "application/json"),
            )
        } catch (err: IllegalStateException) {
            val text = err.message.orEmpty()
            if (text.contains("device", ignoreCase = true)) {
                throw IllegalStateException(
                    "$text Activa «Enable Device Flow» en la OAuth App, o pega el secreto de cliente.",
                )
            }
            throw err
        }
        if (started.has("error")) {
            val detail = started.optString("error_description")
            throw IllegalStateException(
                "GitHub rechazó el código de dispositivo: ${started.getString("error")} $detail. " +
                    "Activa «Enable Device Flow» en la OAuth App, o pega el secreto de cliente.",
            )
        }
        val userCode = started.getString("user_code")
        val verify = started.getString("verification_uri")
        val device = started.getString("device_code")
        val interval = started.optLong("interval", 5L).coerceAtLeast(1L)
        onUserCode(userCode)
        open(Uri.parse(verify).buildUpon().appendQueryParameter("user_code", userCode).build().toString())

        val deadline = System.currentTimeMillis() + started.optLong("expires_in", 900L) * 1000L
        var wait = interval
        while (System.currentTimeMillis() < deadline) {
            if (stopped) throw IllegalStateException("Inicio de sesión cancelado.")
            try {
                Thread.sleep(wait * 1000L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Inicio de sesión cancelado.")
            }
            val poll = postForm(
                "https://github.com/login/oauth/access_token",
                mapOf(
                    "client_id" to clientId,
                    "device_code" to device,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                ),
                mapOf("Accept" to "application/json"),
            )
            if (poll.has("access_token")) return poll.getString("access_token")
            when (poll.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> wait += 5
                "expired_token" -> throw IllegalStateException("El código de GitHub caducó. Vuelve a intentarlo.")
                "access_denied" -> throw IllegalStateException("Cancelaste la autorización en GitHub.")
                else -> throw IllegalStateException(
                    "GitHub rechazó el canje: ${poll.optString("error")} ${poll.optString("error_description")}",
                )
            }
        }
        throw IllegalStateException("El código de GitHub caducó. Vuelve a intentarlo.")
    }

    private fun githubEmail(access: String): String {
        val conn = openGet(
            "https://api.github.com/user/emails",
            access,
            mapOf("Accept" to "application/vnd.github+json", "User-Agent" to "CloudTerm"),
        )
        val text = read(conn)
        if (conn.responseCode !in 200..299) return ""
        val list = org.json.JSONArray(text)
        var fallback = ""
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            if (!item.optBoolean("verified")) continue
            val email = item.optString("email")
            if (item.optBoolean("primary")) return email
            if (fallback.isBlank()) fallback = email
        }
        return fallback
    }

    /**
     * Escucha en 127.0.0.1, abre el navegador y devuelve el código.
     * [prepare] recibe el puerto y devuelve el `redirect_uri` usado.
     */
    private fun loopback(
        expectedState: String,
        path: String,
        preferredPort: Int = 0,
        prepare: (Int) -> String,
    ): String {
        val address = java.net.InetAddress.getByName("127.0.0.1")
        val listener = if (preferredPort > 0) {
            try {
                ServerSocket(preferredPort, 1, address)
            } catch (_: java.io.IOException) {
                ServerSocket(0, 1, address)
            }
        } else {
            ServerSocket(0, 1, address)
        }
        server = listener
        listener.soTimeout = 180_000
        try {
            lastRedirect = prepare(listener.localPort)
            val socket = listener.accept()
            socket.soTimeout = 10_000
            socket.use { client ->
                val request = client.getInputStream().bufferedReader().readLine().orEmpty()
                val target = request.split(" ").getOrNull(1).orEmpty()
                val query = target.substringAfter("?", "")
                val params = query.split("&").mapNotNull { part ->
                    val key = part.substringBefore("=")
                    if (key.isEmpty()) null else key to Uri.decode(part.substringAfter("=", ""))
                }.toMap()
                val ok = params["state"] == expectedState && params["code"] != null && params["error"] == null
                val page = if (ok) {
                    "<h1>Cuenta vinculada</h1><p>Ya puedes volver a CloudTerm.</p>"
                } else {
                    "<h1>No se pudo iniciar sesión</h1><p>${params["error"] ?: "respuesta incompleta"}</p>"
                }
                val body = "<!doctype html><html><body style=\"font-family:sans-serif\">$page</body></html>"
                val bytes = body.toByteArray()
                client.getOutputStream().write(
                    "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray() + bytes,
                )
                client.getOutputStream().flush()
                params["error"]?.let { throw IllegalStateException("El proveedor rechazó el acceso: $it") }
                if (params["state"] != expectedState) {
                    throw IllegalStateException("La respuesta no corresponde a esta petición.")
                }
                if (!target.substringBefore("?").let { it == path || (path == "/" && (it == "/" || it.isEmpty())) }) {
                    throw IllegalStateException("La vuelta no llegó a $path.")
                }
                return params["code"] ?: throw IllegalStateException("La respuesta no traía código.")
            }
        } catch (err: java.net.SocketException) {
            if (stopped) throw IllegalStateException("Inicio de sesión cancelado.")
            throw err
        } finally {
            listener.close()
            server = null
        }
    }

    private fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun postForm(url: String, fields: Map<String, String>, headers: Map<String, String> = emptyMap()): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 20_000
            readTimeout = 20_000
        }
        conn.outputStream.use { it.write(form(fields).toByteArray()) }
        val text = read(conn)
        val json = runCatching { JSONObject(text) }.getOrElse {
            throw IllegalStateException("Respuesta ilegible (${conn.responseCode}): $text")
        }
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("El proveedor rechazó el canje (${conn.responseCode}): $text")
        }
        return json
    }

    private fun getJson(url: String, access: String, headers: Map<String, String> = emptyMap()): JSONObject {
        val conn = openGet(url, access, headers)
        val text = read(conn)
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("No se pudo leer la cuenta (${conn.responseCode}).")
        }
        return JSONObject(text)
    }

    private fun openGet(url: String, access: String, headers: Map<String, String>): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $access")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 20_000
            readTimeout = 20_000
        }
    }

    private fun read(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.let { BufferedReader(InputStreamReader(it)).readText() }.orEmpty()
    }

    private companion object {
        const val GITHUB_SCOPES = "read:user user:email gist"

        fun verifier(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        fun challenge(verifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        fun form(fields: Map<String, String>): String =
            fields.entries.joinToString("&") { (k, v) ->
                URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
            }
    }
}
