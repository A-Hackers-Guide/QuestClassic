package io.github.questclassic.app.core

/**
 * Pure debouncing state machine. Caller passes event arrival
 * timestamps; the debouncer tells each whether to fire the action
 * or skip because a trailing event is pending.
 *
 * Trailing-edge semantics: only the event at the END of a quiet
 * window fires. This matches what the UI wants for 'save draft N
 * ms after the last keystroke'.
 *
 * Pure function around an injectable clock; a coroutine wrapper
 * that calls [accept] + delays in between lives in the caller
 * (not this module, so tests don't need coroutines).
 */
class Debouncer(val quietMillis: Long = 200, private val now: () -> Long = System::currentTimeMillis) {

    private var lastEventMs: Long = Long.MIN_VALUE

    /** Mark an event and return the timestamp when its action should
     *  fire IFF no later event supersedes it. The caller then
     *  schedules a delay and re-calls [shouldFire] at wake. */
    @Synchronized
    fun accept(): Long {
        val t = now()
        lastEventMs = t
        return t + quietMillis
    }

    /** Return true if [eventMs] is still the last event seen —
     *  i.e. nothing newer arrived during the quiet window. */
    @Synchronized
    fun shouldFire(eventMs: Long): Boolean {
        return eventMs == lastEventMs + 0L   // no newer event
    }

    @Synchronized
    fun reset() {
        lastEventMs = Long.MIN_VALUE
    }
}
