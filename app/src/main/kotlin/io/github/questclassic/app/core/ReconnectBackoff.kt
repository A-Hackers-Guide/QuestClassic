package io.github.questclassic.app.core

/**
 * Exponential backoff with jitter + ceiling, for the companion
 * WebSocket reconnect loop and any other 'try again later' callers.
 *
 * - First retry fires almost immediately ([baseMillis]).
 * - Each subsequent retry doubles until the ceiling ([maxMillis]).
 * - ±[jitterPercent]% random jitter so a thousand headsets don't
 *   reconnect to the same companion in lockstep.
 *
 * Pure function around a Double random source so tests can pin it.
 */
class ReconnectBackoff(
    val baseMillis: Long = 500,
    val maxMillis: Long = 30_000,
    val jitterPercent: Int = 20,
    private val random: () -> Double = Math::random,
) {
    init {
        require(baseMillis in 1..maxMillis) { "base must be within 1..max" }
        require(jitterPercent in 0..50) { "jitter must be 0..50" }
    }

    private var attempt = 0

    /** Record a failed attempt and return the delay before the next try. */
    @Synchronized
    fun nextDelayMs(): Long {
        val raw = baseMillis.toDouble() * (1L shl attempt.coerceAtMost(30))
        val capped = raw.coerceAtMost(maxMillis.toDouble())
        val jitterRange = capped * jitterPercent / 100.0
        val delta = (random() - 0.5) * 2 * jitterRange
        val withJitter = (capped + delta).coerceAtLeast(0.0)
        attempt++
        return withJitter.toLong()
    }

    /** Reset after a successful connect. */
    @Synchronized
    fun reset() {
        attempt = 0
    }

    @Synchronized
    fun attemptCount(): Int = attempt
}
