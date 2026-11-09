package io.github.questclassic.app.core

/**
 * Pre-defined multi-pattern buzz *sequences* — chaining 2+
 * [BuzzPattern]s with pauses between them to express composite states
 * the single-pattern alphabet doesn't cover gracefully.
 *
 * Use cases:
 *   - "AI finished answering" followed by "config exported" → a single
 *     composite buzz instead of two overlapping ones.
 *   - A low-battery warning that escalates: OK → BATTERY_LOW → OVERHEAT.
 *   - Boot-chime: OK / OK / UPDATE_READY once a fresh install's
 *     payload is verified.
 *
 * Each sequence carries the pattern list and an inter-pattern pause in
 * milliseconds. Pure data; the actual playback loop lives in
 * `ControllerHaptics.playSequence` so this module stays unit-testable.
 */
data class BuzzSequence(
    val id: String,
    val label: String,
    val patterns: List<BuzzPattern>,
    val gapMs: Long = 150L,
) {
    init {
        require(patterns.isNotEmpty()) { "empty sequence: $id" }
        require(gapMs in 50L..1_000L) { "gap out of sensible range: $gapMs" }
    }

    /** Total approximate duration including gaps and each pattern's own
     *  timings (via BuzzPattern.toTimings). */
    fun approxDurationMs(): Long =
        patterns.sumOf { it.toTimings().sum() } + gapMs * (patterns.size - 1).coerceAtLeast(0)

    companion object {
        val BOOT_CHIME = BuzzSequence(
            id = "boot_chime",
            label = "Successful boot + ready",
            patterns = listOf(BuzzPattern.OK, BuzzPattern.UPDATE_READY),
            gapMs = 200L,
        )

        val BATTERY_ESCALATION = BuzzSequence(
            id = "battery_escalation",
            label = "Battery dropping fast",
            patterns = listOf(BuzzPattern.BATTERY_LOW, BuzzPattern.OVERHEAT),
            gapMs = 120L,
        )

        val AI_HANDED_OFF = BuzzSequence(
            id = "ai_handed_off",
            label = "AI answered + companion took over",
            patterns = listOf(BuzzPattern.AI_ANSWERED, BuzzPattern.COMPANION_PAIRED),
            gapMs = 200L,
        )

        val CONFIG_SHIPPED = BuzzSequence(
            id = "config_shipped",
            label = "Config exported + verified",
            patterns = listOf(BuzzPattern.CONFIG_EXPORTED, BuzzPattern.OK),
            gapMs = 180L,
        )

        val SECURITY_EVENT = BuzzSequence(
            id = "security_event",
            label = "Root detected + attention required",
            patterns = listOf(BuzzPattern.ROOT_DETECTED, BuzzPattern.OTA_CHECK_FAIL),
            gapMs = 180L,
        )

        /** Every registered sequence — demo picker and docs read from here. */
        val ALL: List<BuzzSequence> = listOf(
            BOOT_CHIME, BATTERY_ESCALATION, AI_HANDED_OFF, CONFIG_SHIPPED, SECURITY_EVENT,
        )
    }
}
