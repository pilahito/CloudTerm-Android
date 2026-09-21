package com.pilahito.cloudterm.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Mantiene la sesión SSH viva cuando la app pasa a segundo plano.
 * Sin esto Android mata el proceso y se pierde la terminal.
 */
class SshSessionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        crearCanal()
        val notification = notificacion(intent?.getStringExtra(EXTRA_HOST) ?: "CloudTerm")
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        return START_STICKY
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Sesión SSH", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mantiene CloudTerm conectado en segundo plano"
                setShowBadge(false)
            },
        )
    }

    private fun notificacion(host: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("CloudTerm")
            .setContentText("Conectado a $host")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.pilahito.cloudterm.android.STOP_SESSION"
        const val EXTRA_HOST = "host"
        private const val CHANNEL = "ssh_session"
        private const val NOTIF_ID = 7
    }
}
