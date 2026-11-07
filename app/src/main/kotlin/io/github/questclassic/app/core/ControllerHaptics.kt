package io.github.questclassic.app.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Controller + headset haptics driver used for diagnostic buzz patterns.
 *
 * Each error code has its own buzz pattern. The pattern reads like short International Morse —
 * dot (80ms), dash (240ms), gap (120ms) — prefixed with a long burst so you know a code is about
 * to be transmitted. When the shell UI can't render the error (display dead, etc.) the headset
 * and controllers still get these patterns, and the user can match them against
 * `docs/diagnostics_patterns.md` in this repo.
 */
class ControllerHaptics(context: Context) {
    private val vibrator: Vibrator = run {
        val svcName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Context.VIBRATOR_MANAGER_SERVICE else Context.VIBRATOR_SERVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(svcName) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(svcName) as Vibrator
        }
    }

    fun playPattern(pattern: BuzzPattern) {
        if (!vibrator.hasVibrator()) return
        val timings = pattern.toTimings()
        val amps = IntArray(timings.size) { if (it % 2 == 0) 0 else 255 }
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(timings, amps, -1)
        } else {
            @Suppress("DEPRECATION")
            return vibrator.vibrate(timings, -1)
        }
        vibrator.vibrate(effect)
    }

    fun cancel() {
        runCatching { vibrator.cancel() }
    }
}

/** A buzz pattern built from short "dot" (.) and long "dash" (-) pulses. */
data class BuzzPattern(val codeLetters: String, val label: String) {
    /** First element is initial gap (0ms). Then alternating gap/pulse timings. */
    fun toTimings(): LongArray {
        val dot = 80L
        val dash = 240L
        val gap = 120L
        val longGap = 400L
        val startBurst = 600L

        val out = mutableListOf<Long>()
        out += 0L                // initial gap
        out += startBurst        // long burst to flag "pattern incoming"
        out += longGap           // pause
        for ((i, c) in codeLetters.withIndex()) {
            when (c) {
                '.' -> out += dot
                '-' -> out += dash
                ' ' -> { out[out.size - 1] = out.last() + longGap; continue }
                else -> out += dot
            }
            out += if (i == codeLetters.length - 1) longGap else gap
        }
        return out.toLongArray()
    }

    companion object {
        /** Codes mirror `docs/diagnostics_patterns.md` for user lookup. */
        val OK = BuzzPattern("...", "self-test passed")
        val BATTERY_LOW = BuzzPattern("-.", "battery low")
        val STORAGE_FULL = BuzzPattern("-..", "storage critical")
        val WIFI_DOWN = BuzzPattern("-.-.", "no network")
        val OTA_CHECK_FAIL = BuzzPattern("-.-", "OTA check failed")
        val CONTROLLER_MISSING = BuzzPattern("-..-", "controller tracking lost")
        val THERMAL = BuzzPattern("--", "thermal throttling")
        val DISPLAY_FAULT = BuzzPattern("...-", "display fault")
    }
}
