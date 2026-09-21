package com.pilahito.cloudterm.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Lista de servidores en un JSON dentro del directorio privado de la app. Sin credenciales. */
class HostStore(context: Context) {
    private val file = File(context.filesDir, "hosts.json")

    fun load(): List<Host> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Host(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    hostname = o.getString("hostname"),
                    port = o.optInt("port", 22),
                    username = o.getString("username"),
                    authType = runCatching { AuthType.valueOf(o.optString("auth", "PASSWORD")) }
                        .getOrDefault(AuthType.PASSWORD),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(hosts: List<Host>) {
        val arr = JSONArray()
        hosts.forEach { h ->
            arr.put(
                JSONObject()
                    .put("id", h.id)
                    .put("name", h.name)
                    .put("hostname", h.hostname)
                    .put("port", h.port)
                    .put("username", h.username)
                    .put("auth", h.authType.name)
            )
        }
        file.writeText(arr.toString())
    }
}
