package io.github.questclassic.app.ai

/**
 * Pure math for 'ducking' — lowering a background audio stream's
 * level while the AI companion is speaking, then ramping it back
 * up when it stops.
 *
 * Takes the current foreground state (speaking / silent) + the
 * elapsed milliseconds since the last state change and returns a
 * gain factor in [0.0, 1.0] to apply to the background stream.
 *
 * - Speaking: ramp down from 1.0 to [duckedGain] over [attackMs].
 * - Silent:   ramp back from [duckedGain] to 1.0 over [releaseMs].
 *
 * Pure function; test with explicit (state, elapsed) tuples.
 */
object AudioDucker {

    data class Config(
        val duckedGain: Float = 0.25f,
        val attackMs: Int = 150,
        val releaseMs: Int = 600,
    ) {
        init {
            require(duckedGain in 0f..1f)
            require(attackMs > 0 && releaseMs > 0)
        }
    }

    enum class State { SPEAKING, SILENT }

    /** @param elapsedMs since the state changed (0 = just transitioned). */
    fun gain(state: State, elapsedMs: Long, config: Config = Config()): Float {
        val t = elapsedMs.coerceAtLeast(0L)
        return when (state) {
            State.SPEAKING -> {
                val duration = config.attackMs.toLong()
                if (t >= duration) config.duckedGain
                else lerp(1.0f, config.duckedGain, t.toFloat() / duration)
            }
            State.SILENT -> {
                val duration = config.releaseMs.toLong()
                if (t >= duration) 1.0f
                else lerp(config.duckedGain, 1.0f, t.toFloat() / duration)
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
}
