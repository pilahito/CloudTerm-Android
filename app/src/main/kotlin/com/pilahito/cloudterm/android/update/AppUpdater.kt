package com.pilahito.cloudterm.android.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val newer: Boolean,
)

object AppUpdater {
    private const val API = "https://api.github.com/repos/pilahito/CloudTerm-Android/releases/latest"
    private const val UA = "CloudTerm-Android"

    fun currentVersion(app: Application): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, 0).versionName
            }
        }.getOrNull().orEmpty().ifBlank { "0" }
    }

    fun check(current: String): UpdateInfo {
        val body = getText(API)
        val json = JSONObject(body)
        val tag = json.optString("tag_name").removePrefix("v")
        val title = json.optString("name").ifBlank { "CloudTerm $tag" }
        val notes = json.optString("body").take(800)
        val assets = json.optJSONArray("assets") ?: throw IllegalStateException("Sin APK en el release")
        var apk: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apk = a.optString("browser_download_url")
                break
            }
        }
        if (apk.isNullOrBlank()) throw IllegalStateException("Ese release no trae APK")
        return UpdateInfo(tag = tag, title = title, notes = notes, apkUrl = apk, newer = isNewer(tag, current))
    }

    fun download(app: Application, url: String, onProgress: (Int) -> Unit): File {
        val dir = File(app.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "CloudTerm-update.apk")
        if (dest.exists()) dest.delete()
        open(url).use { conn ->
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        }
        if (dest.length() < 1024) throw IllegalStateException("La descarga salió vacía")
        return dest
    }

    fun install(app: Application, apk: File) {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        app.startActivity(intent)
    }

    fun needsInstallPermission(app: Application): Boolean {
        return Build.VERSION.SDK_INT >= 26 && !app.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermission(app: Application) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${app.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }

    private fun getText(url: String): String {
        open(url).use { conn ->
            return conn.inputStream.bufferedReader().use { it.readText() }
        }
    }

    private fun open(start: String): HttpURLConnection {
        var current = start
        repeat(8) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", UA)
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next.isNullOrBlank()) throw IllegalStateException("Redirección sin destino")
                current = if (next.startsWith("http")) next else URL(URL(current), next).toString()
                return@repeat
            }
            if (code !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                conn.disconnect()
                throw IllegalStateException("GitHub $code ${err?.take(120) ?: ""}".trim())
            }
            return conn
        }
        throw IllegalStateException("Demasiadas redirecciones al bajar el APK")
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").split('.', '-', '_').mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
}
