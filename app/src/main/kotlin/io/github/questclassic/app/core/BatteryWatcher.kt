package io.github.questclassic.app.core

/**
 * Pure state machine for battery-level notice thresholds.
 *
 * Reads a percent (0..100) and decides whether to emit a notice:
 *   - > 25%            → no notice.
 *   - crossing 25%     → INFO 'battery_low' (one-shot).
 *   - crossing 10%     → WARN 'battery_critical' (one-shot).
 *   - rising back above 30% → reset so a subsequent fall can fire again.
 *
 * One-shot means the watcher only emits when the threshold is first
 * crossed; staying under doesn't spam. Rising above the reset floor
 * re-arms so a legit charge-discharge cycle gets two notices, not two
 * thousand.
 *
 * Pure function; test with a sequence of readings.
 */
class BatteryWatcher {

    private var low = false
    private var critical = false

    enum class Emit { NONE, LOW, CRITICAL }

    fun accept(percent: Int): Emit {
        val p = percent.coerceIn(0, 100)
        return when {
            p <= 10 && !critical -> {
                critical = true
                low = true
                Emit.CRITICAL
            }
            p in 11..25 && !low -> {
                low = true
                Emit.LOW
            }
            p >= 30 -> {
                low = false
                critical = false
                Emit.NONE
            }
            else -> Emit.NONE
        }
    }

    /** Reset all state (e.g. on container restart). */
    fun reset() {
        low = false
        critical = false
    }
}
