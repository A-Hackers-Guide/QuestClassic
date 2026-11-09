package io.github.questclassic.app.core

/**
 * Collects startup-phase timings so the Diagnostics panel can show
 * "which step took the most time" when a user reports a slow boot.
 *
 * Usage:
 *   val bt = BootTiming()
 *   bt.mark("appcontainer.created")
 *   bt.mark("compose.first-frame")
 *   bt.mark("ai.config.loaded")
 *   …
 *   bt.toJson() → persistable snapshot
 *
 * Each mark captures the elapsed wall-clock time since the first mark
 * (so the list reads as monotonically increasing deltas from t=0).
 *
 * Pure function + injectable clock → unit-testable with no real time.
 */
class BootTiming(private val now: () -> Long = System::currentTimeMillis) {

    data class Mark(val label: String, val elapsedSinceStartMs: Long)

    private val marks = mutableListOf<Mark>()
    @Volatile private var startMs: Long = -1L

    /** Record a phase boundary. The first call establishes t=0. */
    @Synchronized
    fun mark(label: String) {
        val t = now()
        if (startMs < 0) startMs = t
        marks += Mark(label, t - startMs)
    }

    @Synchronized
    fun snapshot(): List<Mark> = marks.toList()

    @Synchronized
    fun reset() {
        marks.clear()
        startMs = -1L
    }

    /** JSON-ish serialization for the diagnostics bundle. Minimal
     *  to avoid pulling kotlinx-serialization into this module. */
    @Synchronized
    fun toJson(): String {
        val body = marks.joinToString(",\n") { m ->
            "  {\"label\": ${escape(m.label)}, \"ms\": ${m.elapsedSinceStartMs}}"
        }
        return "{\n  \"marks\": [\n$body\n  ]\n}"
    }

    /** Total elapsed since the first mark; -1 when nothing marked yet. */
    @Synchronized
    fun totalMs(): Long = marks.lastOrNull()?.elapsedSinceStartMs ?: -1L

    private fun escape(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
