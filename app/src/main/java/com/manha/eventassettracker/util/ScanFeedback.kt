package com.manha.eventassettracker.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Plays a sharp, loud confirmation beep (or a lower error buzz) and vibrates, on every scan. */
class ScanFeedback(context: Context) {
    private val appContext = context.applicationContext
    private var toneGenerator: ToneGenerator? = null

    private fun tone(): ToneGenerator {
        var tg = toneGenerator
        if (tg == null) {
            tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100) // max volume
            toneGenerator = tg
        }
        return tg
    }

    fun success() {
        try {
            // Two short sharp beeps, like a classic handheld barcode scanner.
            tone().startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
        } catch (_: Exception) {
        }
        vibrate(longArrayOf(0, 90, 50, 90))
    }

    fun error() {
        try {
            tone().startTone(ToneGenerator.TONE_CDMA_PIP, 220)
        } catch (_: Exception) {
        }
        vibrate(longArrayOf(0, 250, 90, 250))
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
