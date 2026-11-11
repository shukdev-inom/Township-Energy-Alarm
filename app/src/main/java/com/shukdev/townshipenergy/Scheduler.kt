package com.shukdev.townshipenergy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Uses AlarmManager.setAlarmClock(): exact, survives Doze, works with the screen off.
 * If the phone still refuses exact alarms, falls back to a normal alarm instead of crashing.
 */
object Scheduler {

    private fun firePending(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1, Intent(c, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun showPending(c: Context): PendingIntent =
        PendingIntent.getActivity(
            c, 2, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(c: Context, triggerAtMillis: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPending(c)),
                firePending(c)
            )
        } catch (e: SecurityException) {
            // Exact alarms blocked: use a regular alarm (can be a few minutes late).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, firePending(c))
            Toast.makeText(
                c,
                "Exact alarms are blocked on this phone, so the alert may come a little late.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun cancel(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePending(c))
        Store.clearTarget(c)
    }
}