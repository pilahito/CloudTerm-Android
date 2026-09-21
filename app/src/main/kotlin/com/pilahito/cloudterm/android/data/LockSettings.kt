package com.pilahito.cloudterm.android.data

import android.content.Context

class LockSettings(context: Context) {
    private val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("bio", false)
        set(value) { prefs.edit().putBoolean("bio", value).apply() }
}
