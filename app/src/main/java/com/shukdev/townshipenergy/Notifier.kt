package com.shukdev.townshipenergy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CHANNEL = "energy_alert"
    const val MESSAGE = "Energy has been refilled. Please continue your expedition."
    const val ID_SERVICE = 1
    const val ID_DONE = 2

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Energy alerts", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    fun build(c: Context, autoCancel: Boolean): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            c, 3, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Energy refilled")
            .setContentText(MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MESSAGE))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(autoCancel)
    }

    /** Normal, dismissible notification that stays after the voice finishes. */
    fun postDone(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(c)
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ID_DONE, build(c, true).build())
    }
}
