package io.github.questclassic.app.ai

/**
 * Maps the UserPrefs.IntPref.AI_TEMPERATURE_INT (0..100) into each
 * backend's native temperature float. Providers differ:
 *
 *   - Claude: 0.0..1.0
 *   - OpenAI: 0.0..2.0
 *   - Gemini: 0.0..2.0 (practical ceiling ~1.0)
 *   - Ollama: 0.0..2.0 but most models expect 0.0..1.0
 *
 * A slider labelled '50' should mean roughly 'half as spicy' per
 * backend, not an opaque internal number. This module maps the
 * normalised 0..100 int into whatever the backend actually accepts.
 *
 * Pure function; unit-testable.
 */
object TemperatureMapper {

    /**
     * @param percent 0..100 (clamped).
     * @param backendId one of 'claude' / 'openai' / 'gemini' / 'ollama'.
     *   Unknown backend uses the Claude-compatible 0.0..1.0 range.
     */
    fun map(percent: Int, backendId: String): Double {
        val clamped = percent.coerceIn(0, 100).toDouble()
        val range = ranges[backendId.lowercase()] ?: ranges.getValue("claude")
        // Linear interpolate percent / 100 across the range.
        return range.first + (range.second - range.first) * (clamped / 100.0)
    }

    /** Inverse: provider-native float → 0..100 int. */
    fun unmap(value: Double, backendId: String): Int {
        val range = ranges[backendId.lowercase()] ?: ranges.getValue("claude")
        val span = range.second - range.first
        if (span <= 0.0) return 0
        val pct = ((value - range.first) / span * 100.0).toInt().coerceIn(0, 100)
        return pct
    }

    /** Documented provider ranges for the dropdown's tooltip. */
    val ranges: Map<String, Pair<Double, Double>> = mapOf(
        "claude" to (0.0 to 1.0),
        "openai" to (0.0 to 2.0),
        "gemini" to (0.0 to 2.0),
        "ollama" to (0.0 to 1.0),
    )
}
