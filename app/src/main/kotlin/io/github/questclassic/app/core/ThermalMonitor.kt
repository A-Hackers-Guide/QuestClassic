package io.github.questclassic.app.core

/**
 * Pure state machine for thermal-state notice thresholds.
 *
 * Android's PowerManager gives a 0..5 thermal status; this module turns
 * a stream of that into discrete notices, mirroring [BatteryWatcher]:
 *
 *   - 0 NONE / 1 LIGHT               → no notice.
 *   - crossing 2 MODERATE            → INFO 'thermal_warm' (one-shot).
 *   - crossing 3 SEVERE              → WARN 'thermal_hot' (one-shot).
 *   - crossing 4 CRITICAL / 5 EMERGENCY / 6 SHUTDOWN → ERROR 'thermal_critical'.
 *   - dropping below 2 again re-arms so a legit cycle gets a notice
 *     the second time.
 *
 * The Android PowerManager.ThermalStatusListener wire-up lives in a
 * feature-specific Service; this is the pure state the service feeds.
 */
class ThermalMonitor {

    private var warm = false
    private var hot = false
    private var critical = false

    enum class Emit { NONE, WARM, HOT, CRITICAL }

    fun accept(thermalStatus: Int): Emit {
        val s = thermalStatus.coerceIn(0, 6)
        return when {
            s >= 4 && !critical -> {
                critical = true; hot = true; warm = true
                Emit.CRITICAL
            }
            s == 3 && !hot -> {
                hot = true; warm = true
                Emit.HOT
            }
            s == 2 && !warm -> {
                warm = true
                Emit.WARM
            }
            s <= 1 -> {
                warm = false; hot = false; critical = false
                Emit.NONE
            }
            else -> Emit.NONE
        }
    }

    fun reset() {
        warm = false; hot = false; critical = false
    }
}
