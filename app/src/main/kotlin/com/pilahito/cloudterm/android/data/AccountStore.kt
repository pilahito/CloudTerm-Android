package com.pilahito.cloudterm.android.data

import android.content.Context
import org.json.JSONObject

data class CloudAccount(
    val provider: String,
    val name: String,
    val email: String,
)

/**
 * El identificador de cliente no es secreto y va en preferencias.
 * Los tokens y el secreto de GitHub van al [SecretVault].
 */
class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val vault = SecretVault(context)

    var googleClientId: String
        get() = prefs.getString(GOOGLE_ID, "").orEmpty()
        set(value) = prefs.edit().putString(GOOGLE_ID, value.trim()).apply()

    var githubClientId: String
        get() = prefs.getString(GITHUB_ID, "").orEmpty()
        set(value) = prefs.edit().putString(GITHUB_ID, value.trim()).apply()

    fun githubSecret(): String? = vault.get(GITHUB_SECRET)?.takeIf { it.isNotBlank() }

    fun saveGithubSecret(value: String) {
        if (value.isBlank()) vault.remove(GITHUB_SECRET) else vault.put(GITHUB_SECRET, value.trim())
    }

    fun account(): CloudAccount? {
        val raw = prefs.getString(ACCOUNT, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            CloudAccount(
                provider = json.getString("provider"),
                name = json.optString("name"),
                email = json.optString("email"),
            )
        }.getOrNull()
    }

    fun saveAccount(account: CloudAccount, accessToken: String) {
        vault.put("token:${account.provider}", accessToken)
        prefs.edit().putString(
            ACCOUNT,
            JSONObject()
                .put("provider", account.provider)
                .put("name", account.name)
                .put("email", account.email)
                .toString(),
        ).apply()
    }

    fun signOut() {
        account()?.let { vault.remove("token:${it.provider}") }
        prefs.edit().remove(ACCOUNT).apply()
    }

    private companion object {
        const val GOOGLE_ID = "googleClientId"
        const val GITHUB_ID = "githubClientId"
        const val GITHUB_SECRET = "auth:github:client_secret"
        const val ACCOUNT = "account"
    }
}
