package com.shukdev.townshipenergy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Uses AlarmManager.setAlarmClock(): exact, survives Doze, works with the screen off
 * and needs NO special "exact alarm" permission.
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
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showPending(c)), firePending(c))
    }

    fun cancel(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePending(c))
        Store.clearTarget(c)
    }
}
