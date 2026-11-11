package com.shukdev.townshipenergy

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Short-lived foreground service: speaks the message twice, posts a notification, stops. */
class SpeakService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_STOP = "com.shukdev.townshipenergy.STOP_VOICE"
    }

    private var tts: TextToSpeech? = null
    private var finished = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopNow()
            return START_NOT_STICKY
        }
        Notifier.ensureChannel(this)
        val n = Notifier.build(this, false, withStop = true).build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifier.ID_SERVICE, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(Notifier.ID_SERVICE, n)
        }
        if (tts == null) {
            tts = TextToSpeech(this, this)
            handler.postDelayed({ finish() }, 25_000) // safety net
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        val t = tts
        if (finished) return
        if (status != TextToSpeech.SUCCESS || t == null) {
            finish()
            return
        }
        t.language = Locale.US
        t.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "second") finish()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                finish()
            }
        })
        t.speak(Notifier.MESSAGE, TextToSpeech.QUEUE_FLUSH, null, "first")
        t.speak(Notifier.MESSAGE, TextToSpeech.QUEUE_ADD, null, "second")
    }

    /** Voice finished by itself: leave a normal notification behind. */
    private fun finish() {
        if (finished) return
        finished = true
        handler.post {
            Notifier.postDone(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** User pressed Stop (or opened the app): silence immediately, no extra notification. */
    private fun stopNow() {
        finished = true
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
