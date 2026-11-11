package com.shukdev.townshipenergy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped on reboot, so re-create the pending one. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val target = Store.target(context)
        if (target <= 0L) return
        if (target > System.currentTimeMillis()) {
            Scheduler.schedule(context, target)
        } else {
            // Energy filled while the phone was off.
            Store.clearTarget(context)
            Store.setDone(context, true)
            Notifier.postDone(context)
        }
    }
}
