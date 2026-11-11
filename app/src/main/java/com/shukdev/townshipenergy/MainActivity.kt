package com.shukdev.townshipenergy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {

    private lateinit var etCurrent: EditText
    private lateinit var etMax: EditText
    private lateinit var etMinutes: EditText
    private lateinit var ring: EnergyRingView
    private lateinit var tvLabel: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvRingEnergy: TextView
    private lateinit var tvFullAt: TextView
    private lateinit var tvPct: TextView
    private lateinit var tvHudEnergy: TextView
    private lateinit var xpBar: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    private var testTts: TextToSpeech? = null

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Allow notifications so the alert can show up.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCurrent = findViewById(R.id.etCurrent)
        etMax = findViewById(R.id.etMax)
        etMinutes = findViewById(R.id.etMinutes)
        ring = findViewById(R.id.ring)
        tvLabel = findViewById(R.id.tvLabel)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvRingEnergy = findViewById(R.id.tvRingEnergy)
        tvFullAt = findViewById(R.id.tvFullAt)
        tvPct = findViewById(R.id.tvPct)
        tvHudEnergy = findViewById(R.id.tvHudEnergy)
        xpBar = findViewById(R.id.xpBar)

        // Restore last settings
        etMax.setText(Store.max(this).toString())
        val m = Store.minutes(this)
        if (m > 0) etMinutes.setText(trim(m))
        liveEnergy()?.let { etCurrent.setText(it.toString()) }

        // Steppers
        findViewById<View>(R.id.btnCurMinus).setOnClickListener { bump(etCurrent, -1.0, 0.0, false) }
        findViewById<View>(R.id.btnCurPlus).setOnClickListener { bump(etCurrent, 1.0, 0.0, false) }
        findViewById<View>(R.id.btnMaxMinus).setOnClickListener { bump(etMax, -5.0, 1.0, false) }
        findViewById<View>(R.id.btnMaxPlus).setOnClickListener { bump(etMax, 5.0, 1.0, false) }
        findViewById<View>(R.id.btnMinMinus).setOnClickListener { bump(etMinutes, -0.5, 0.5, true) }
        findViewById<View>(R.id.btnMinPlus).setOnClickListener { bump(etMinutes, 0.5, 0.5, true) }

        // Quick "I spent energy" chips (and the + in the top pill adds 5)
        findViewById<View>(R.id.chip5).setOnClickListener { adjustEnergy(-5) }
        findViewById<View>(R.id.chip10).setOnClickListener { adjustEnergy(-10) }
        findViewById<View>(R.id.chip15).setOnClickListener { adjustEnergy(-15) }
        findViewById<View>(R.id.chip20).setOnClickListener { adjustEnergy(-20) }
        findViewById<View>(R.id.hudPlus).setOnClickListener { adjustEnergy(5) }

        findViewById<View>(R.id.btnStart).setOnClickListener { startFromFields() }
        findViewById<View>(R.id.btnCancel).setOnClickListener {
            Scheduler.cancel(this)
            Store.setDone(this, false)
            Toast.makeText(this, "Timer cancelled", Toast.LENGTH_SHORT).show()
            refresh()
        }
        findViewById<View>(R.id.btnTest).setOnClickListener { testVoice() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() } // timer keeps running

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        refresh()
    }

    // ---------- inputs ----------

    private fun parseMax(): Int =
        etMax.text.toString().trim().toIntOrNull()?.takeIf { it > 0 } ?: 100

    private fun parseMinutes(): Double? =
        etMinutes.text.toString().trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    private fun bump(et: EditText, delta: Double, min: Double, decimal: Boolean) {
        val cur = et.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val v = (cur + delta).coerceAtLeast(min)
        et.setText(if (decimal) trim(v) else v.toInt().toString())
        et.error = null
    }

    private fun startFromFields() {
        val current = etCurrent.text.toString().trim().toIntOrNull()
        if (current == null || current < 0) {
            etCurrent.error = "Enter your current energy"
            return
        }
        val minutes = parseMinutes()
        if (minutes == null) {
            etMinutes.error = "Enter minutes per 1 energy"
            return
        }
        beginTimer(current, parseMax(), minutes)
    }

    /** Add/subtract energy relative to the live estimate, then restart the timer. */
    private fun adjustEnergy(delta: Int) {
        val base = liveEnergy()
        if (base == null) {
            Toast.makeText(this, "Enter your current energy first", Toast.LENGTH_SHORT).show()
            return
        }
        val minutes = parseMinutes() ?: Store.minutes(this).takeIf { it > 0 }
        if (minutes == null) {
            etMinutes.error = "Enter minutes per 1 energy"
            return
        }
        val max = parseMax()
        val newEnergy = (base + delta).coerceIn(0, max)
        etCurrent.setText(newEnergy.toString())
        beginTimer(newEnergy, max, minutes)
    }

    private fun beginTimer(current: Int, max: Int, minutes: Double) {
        hideKeyboard()
        if (current >= max) {
            Scheduler.cancel(this)
            Store.saveTimer(this, 0L, max, minutes, max)
            Store.setDone(this, true)
            Toast.makeText(this, "Energy is already full!", Toast.LENGTH_SHORT).show()
            refresh()
            return
        }
        val delayMs = ((max - current) * minutes * 60_000.0).toLong()
        val target = System.currentTimeMillis() + delayMs
        Store.saveTimer(this, target, max, minutes, current)
        Scheduler.schedule(this, target)
        Toast.makeText(this, "Timer set. You'll hear the alert when energy is full.", Toast.LENGTH_LONG).show()
        refresh()
    }

    // ---------- live estimate ----------

    private fun estimated(remainingMs: Long): Int {
        val max = Store.max(this)
        val perEnergyMs = Store.minutes(this) * 60_000.0
        if (perEnergyMs <= 0) return 0
        return (max - ceil(remainingMs / perEnergyMs).toInt()).coerceIn(0, max)
    }

    /** Best guess of the energy right now (null if unknown). */
    private fun liveEnergy(): Int? {
        val target = Store.target(this)
        val now = System.currentTimeMillis()
        if (target > now) return estimated(target - now)
        if (Store.isDone(this)) return Store.max(this)
        return etCurrentOrNull()
    }

    private fun etCurrentOrNull(): Int? =
        if (::etCurrent.isInitialized) etCurrent.text.toString().trim().toIntOrNull() else null

    // ---------- UI refresh ----------

    private fun refresh() {
        val now = System.currentTimeMillis()
        val target = Store.target(this)
        val max = Store.max(this)

        when {
            target > now -> {
                val remaining = target - now
                val est = estimated(remaining)
                val s = remaining / 1000
                tvLabel.text = "Full in"
                tvCountdown.text = String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
                tvRingEnergy.text = "~$est / $max"
                ring.progress = est.toFloat() / max
                tvFullAt.text = "Full at " +
                    SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(target))
                setHud(est, max)
            }
            Store.isDone(this) -> {
                tvLabel.text = "Energy is"
                tvCountdown.text = "FULL"
                tvRingEnergy.text = "$max / $max"
                ring.progress = 1f
                tvFullAt.text = "Ready for your expedition"
                setHud(max, max)
            }
            else -> {
                tvLabel.text = "No timer"
                tvCountdown.text = "--:--:--"
                tvRingEnergy.text = "-- / $max"
                ring.progress = 0f
                tvFullAt.text = "Set your energy and tap Start"
                tvHudEnergy.text = "--/$max"
                tvPct.text = "0%"
                xpBar.progress = 0
            }
        }
    }

    private fun setHud(energy: Int, max: Int) {
        val pct = if (max > 0) energy * 100 / max else 0
        tvHudEnergy.text = "$energy/$max"
        tvPct.text = "$pct%"
        xpBar.progress = pct
    }

    // ---------- misc ----------

    private fun testVoice() {
        testTts?.shutdown()
        testTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                testTts?.language = Locale.US
                testTts?.speak(Notifier.MESSAGE, TextToSpeech.QUEUE_FLUSH, null, "test")
            } else {
                Toast.makeText(this, "Text-to-speech isn't available on this phone", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let { v ->
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
        }
    }

    private fun trim(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    override fun onResume() {
        super.onResume()
        // Opening the app silences a voice alert that is still speaking.
        stopService(android.content.Intent(this, SpeakService::class.java))
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        testTts?.shutdown()
        super.onDestroy()
    }
}
