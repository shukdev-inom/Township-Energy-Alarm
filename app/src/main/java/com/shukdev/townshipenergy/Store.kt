package com.shukdev.townshipenergy

import android.content.Context

/** Tiny SharedPreferences wrapper that remembers the timer + last inputs. */
object Store {
    private fun p(c: Context) = c.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)

    fun saveTimer(c: Context, targetMillis: Long, max: Int, minutesPerEnergy: Double, current: Int) {
        p(c).edit()
            .putLong("target", targetMillis)
            .putInt("max", max)
            .putFloat("minutes", minutesPerEnergy.toFloat())
            .putInt("current", current)
            .putBoolean("done", false)
            .apply()
    }

    fun target(c: Context): Long = p(c).getLong("target", 0L)
    fun max(c: Context): Int = p(c).getInt("max", 100)
    fun minutes(c: Context): Double = p(c).getFloat("minutes", 0f).toDouble()
    fun current(c: Context): Int = p(c).getInt("current", -1)

    /** true after the alarm fired (energy is full) until a new timer is started or cancelled. */
    fun isDone(c: Context): Boolean = p(c).getBoolean("done", false)
    fun setDone(c: Context, done: Boolean) {
        p(c).edit().putBoolean("done", done).apply()
    }

    fun clearTarget(c: Context) {
        p(c).edit().putLong("target", 0L).apply()
    }
}
