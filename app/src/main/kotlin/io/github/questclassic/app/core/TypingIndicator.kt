package io.github.questclassic.app.core

/**
 * Pure state machine for a 'user is typing' indicator. Caller fires
 * [onKeystroke] on every text change; [isTyping] queries whether the
 * indicator should be visible given a quiet window of [quietMillis].
 *
 * Useful for the companion chat panel (future) to show 'your Quest
 * is typing…' to a desktop user, without sending a packet per
 * keystroke.
 *
 * Pure function + injectable clock → deterministic tests.
 */
class TypingIndicator(
    val quietMillis: Long = 1200,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var lastKeystrokeMs: Long = Long.MIN_VALUE

    @Synchronized
    fun onKeystroke() {
        lastKeystrokeMs = now()
    }

    @Synchronized
    fun isTyping(): Boolean {
        val t = now()
        return t - lastKeystrokeMs < quietMillis
    }

    @Synchronized
    fun reset() {
        lastKeystrokeMs = Long.MIN_VALUE
    }
}
