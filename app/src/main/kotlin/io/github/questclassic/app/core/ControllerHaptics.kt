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

    /**
     * Plays a [BuzzSequence] — each pattern in order with the sequence's
     * inter-pattern gap. Promised in #110.
     *
     * Suspends the caller's coroutine between patterns. Cancel the
     * coroutine to stop mid-sequence; [cancel] will clear any residue
     * on the vibrator.
     */
    suspend fun playSequence(seq: BuzzSequence) {
        if (!vibrator.hasVibrator()) return
        for ((i, pattern) in seq.patterns.withIndex()) {
            playPattern(pattern)
            val own = pattern.toTimings().sum()
            // Wait for the pattern to finish + the inter-pattern gap
            // before firing the next one; never gap after the last.
            val sleep = own + if (i == seq.patterns.lastIndex) 0L else seq.gapMs
            kotlinx.coroutines.delay(sleep)
        }
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

        // --- Tier-2 additions ---
        val TRACKING_LOST = BuzzPattern(".-..", "6DoF tracking lost")
        val AUDIO_FAULT = BuzzPattern("-.--", "audio output failed")
        val UPDATE_READY = BuzzPattern("..-", "update ready to install")
        val GUARDIAN_LOST = BuzzPattern("-...", "guardian boundary cleared")
        val OVERHEAT = BuzzPattern("---", "critical overheating — stop")
        val ROOT_DETECTED = BuzzPattern(".--.", "root signals present")
        val PASSTHROUGH_ON = BuzzPattern(".-.", "passthrough enabled")
        val PASSTHROUGH_OFF = BuzzPattern("..--", "passthrough disabled")

        // --- Tier-3 additions ---
        val AI_WAKE = BuzzPattern(".--", "AI assistant woke / is listening")
        val AI_ANSWERED = BuzzPattern("--.", "AI finished answering")
        val VOICE_RECORDING = BuzzPattern(".-.-", "voice clone sample recording")
        val VOICE_TRAINING_DONE = BuzzPattern("-..--", "voice clone training complete")
        val COMPANION_PAIRED = BuzzPattern("...--", "desktop companion paired")
        val COMPANION_LOST = BuzzPattern("---..", "desktop companion lost")
        val EMULATOR_LAUNCHED = BuzzPattern(".----", "emulator launched")
        val CONFIG_EXPORTED = BuzzPattern(".-...", "config exported")
        val CONFIG_RESTORED = BuzzPattern("-....-", "config restored — re-enter API keys")

        /** Convenience: every known pattern, for the docs page and the demo picker. */
        val ALL: List<BuzzPattern> = listOf(
            OK, BATTERY_LOW, STORAGE_FULL, WIFI_DOWN, OTA_CHECK_FAIL,
            CONTROLLER_MISSING, THERMAL, DISPLAY_FAULT,
            TRACKING_LOST, AUDIO_FAULT, UPDATE_READY, GUARDIAN_LOST,
            OVERHEAT, ROOT_DETECTED, PASSTHROUGH_ON, PASSTHROUGH_OFF,
            AI_WAKE, AI_ANSWERED, VOICE_RECORDING, VOICE_TRAINING_DONE,
            COMPANION_PAIRED, COMPANION_LOST, EMULATOR_LAUNCHED,
            CONFIG_EXPORTED, CONFIG_RESTORED,
        )
    }
}
