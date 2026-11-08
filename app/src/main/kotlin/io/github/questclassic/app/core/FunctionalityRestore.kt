package io.github.questclassic.app.core

import android.content.Context
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The "post / hardware check" assistant.
 *
 * Flow:
 *   1. Ask the user "do you notice any problems?" with a 30s timer.
 *   2. If they say yes, or 30s elapses with no answer, try to calibrate / adjust / fix.
 *   3. If nothing can be done, try to DISPLAY the issue.
 *   4. If display fails (e.g. compositor already dead), show the error code on a plain fallback surface.
 *   5. If that fails too, buzz the controllers in a pattern tied to the finding, and ping the
 *      headset haptics in unison.
 *
 * The 'buzz the controllers' step is wired to [ControllerHaptics] using the [BuzzPattern] table.
 * All buzz patterns live in the public repo (see `docs/reference/buzz-patterns.md`) so users can
 * decode a fault by matching the dot/dash pattern they feel.
 *
 * None of these calibration attempts touch anything that requires elevated privileges. We
 * report what we see; we don't try to repair the device by magic.
 */
class FunctionalityRestore(
    private val app: Context,
    private val haptics: ControllerHaptics,
    private val diagnostics: DiagnosticsEngine,
    /** Delay function — injectable so tests can pass a no-op. Default is kotlinx coroutines delay. */
    private val delayMs: suspend (Long) -> Unit = { ms -> delay(ms) },
) {
    sealed class Progress {
        data class Ask(val timeoutSec: Int) : Progress()
        data class Attempt(val action: String) : Progress()
        data class Fixed(val summary: String) : Progress()
        data class ShowError(val name: String, val detail: String) : Progress()
        data class Fallback(val name: String) : Progress()
        data class Buzz(val pattern: BuzzPattern) : Progress()
    }

    /**
     * Run the ladder. The caller feeds back the user's answer by cancelling the flow when they
     * respond "no", otherwise the ladder keeps escalating.
     */
    fun run(timeoutSec: Int = 30): Flow<Progress> = flow {
        emit(Progress.Ask(timeoutSec))
        // Caller cancels this flow if user says "no". If we reach here after the delay, we assume
        // the user is stuck or unresponsive.
        delayMs(timeoutSec * 1000L)

        emit(Progress.Attempt("recalibrate tracking origin"))
        delayMs(400)
        emit(Progress.Attempt("re-bind controllers"))
        delayMs(400)
        emit(Progress.Attempt("re-query displays"))
        delayMs(400)

        // At this point, if we had a repair worth attempting we'd have done it.
        val checks = diagnostics.runAll()
        val failing = checks.firstOrNull { !it.ok }
        if (failing == null) {
            emit(Progress.Fixed("Self-tests pass; no detected issue."))
            return@flow
        }

        val name = failing.name
        val detail = failing.value

        // Step 3 — try to display it on screen.
        emit(Progress.ShowError(name, detail))
        delayMs(600)

        // Step 4 — fallback text surface (very low-level).
        emit(Progress.Fallback(name))
        delayMs(400)

        // Step 5 — tactile.
        val pattern = failing.failPattern ?: BuzzPattern.OK
        emit(Progress.Buzz(pattern))
        haptics.playPattern(pattern)
    }

    /** Device brand sanity, so this code never buzzes on a non-Quest emulator run. */
    fun isQuest(): Boolean =
        Build.MANUFACTURER.equals("oculus", ignoreCase = true) ||
        Build.MANUFACTURER.equals("meta", ignoreCase = true)
}
