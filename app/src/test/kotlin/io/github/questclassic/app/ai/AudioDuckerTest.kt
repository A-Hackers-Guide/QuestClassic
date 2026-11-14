package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioDuckerTest {

    @Test
    fun `speaking at t=0 is 1 0`() {
        assertEquals(1.0f, AudioDucker.gain(AudioDucker.State.SPEAKING, 0), 0.001f)
    }

    @Test
    fun `speaking after attack is ducked`() {
        val cfg = AudioDucker.Config(duckedGain = 0.25f, attackMs = 100, releaseMs = 600)
        assertEquals(0.25f, AudioDucker.gain(AudioDucker.State.SPEAKING, 100, cfg), 0.001f)
        assertEquals(0.25f, AudioDucker.gain(AudioDucker.State.SPEAKING, 500, cfg), 0.001f)
    }

    @Test
    fun `speaking midway interpolates`() {
        val cfg = AudioDucker.Config(duckedGain = 0.0f, attackMs = 100, releaseMs = 600)
        assertEquals(0.5f, AudioDucker.gain(AudioDucker.State.SPEAKING, 50, cfg), 0.001f)
    }

    @Test
    fun `silent at t=0 is ducked`() {
        val cfg = AudioDucker.Config(duckedGain = 0.25f, attackMs = 100, releaseMs = 100)
        assertEquals(0.25f, AudioDucker.gain(AudioDucker.State.SILENT, 0, cfg), 0.001f)
    }

    @Test
    fun `silent after release is 1 0`() {
        val cfg = AudioDucker.Config(duckedGain = 0.25f, attackMs = 100, releaseMs = 100)
        assertEquals(1.0f, AudioDucker.gain(AudioDucker.State.SILENT, 100, cfg), 0.001f)
        assertEquals(1.0f, AudioDucker.gain(AudioDucker.State.SILENT, 999, cfg), 0.001f)
    }

    @Test
    fun `silent midway interpolates`() {
        val cfg = AudioDucker.Config(duckedGain = 0.0f, attackMs = 100, releaseMs = 100)
        assertEquals(0.5f, AudioDucker.gain(AudioDucker.State.SILENT, 50, cfg), 0.001f)
    }

    @Test
    fun `negative elapsed treated as zero`() {
        assertEquals(1.0f, AudioDucker.gain(AudioDucker.State.SPEAKING, -10), 0.001f)
    }

    @Test
    fun `invalid config throws`() {
        var threw = false
        try { AudioDucker.Config(duckedGain = 2f) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        threw = false
        try { AudioDucker.Config(attackMs = 0) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
