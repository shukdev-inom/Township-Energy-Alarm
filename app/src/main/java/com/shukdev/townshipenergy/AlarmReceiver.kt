package com.shukdev.townshipenergy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fired by the system at the exact "energy is full" time. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Store.clearTarget(context)
        Store.setDone(context, true)
        ContextCompat.startForegroundService(context, Intent(context, SpeakService::class.java))
    }
}
