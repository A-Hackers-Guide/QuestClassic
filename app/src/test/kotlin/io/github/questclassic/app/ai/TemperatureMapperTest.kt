package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureMapperTest {

    @Test
    fun `claude 0 maps to 0 0`() {
        assertEquals(0.0, TemperatureMapper.map(0, "claude"), 0.001)
    }

    @Test
    fun `claude 100 maps to 1 0`() {
        assertEquals(1.0, TemperatureMapper.map(100, "claude"), 0.001)
    }

    @Test
    fun `claude 50 maps to 0 5`() {
        assertEquals(0.5, TemperatureMapper.map(50, "claude"), 0.001)
    }

    @Test
    fun `openai 50 maps to 1 0`() {
        assertEquals(1.0, TemperatureMapper.map(50, "openai"), 0.001)
    }

    @Test
    fun `openai 100 maps to 2 0`() {
        assertEquals(2.0, TemperatureMapper.map(100, "openai"), 0.001)
    }

    @Test
    fun `unknown backend uses claude range`() {
        assertEquals(1.0, TemperatureMapper.map(100, "future-backend"), 0.001)
    }

    @Test
    fun `input clamped`() {
        assertEquals(0.0, TemperatureMapper.map(-10, "claude"), 0.001)
        assertEquals(1.0, TemperatureMapper.map(999, "claude"), 0.001)
    }

    @Test
    fun `unmap round trip near identity`() {
        for (pct in 0..100 step 25) {
            val v = TemperatureMapper.map(pct, "openai")
            val back = TemperatureMapper.unmap(v, "openai")
            assertEquals(pct, back)
        }
    }

    @Test
    fun `backend id case insensitive`() {
        val lower = TemperatureMapper.map(50, "openai")
        val upper = TemperatureMapper.map(50, "OpenAI")
        assertEquals(lower, upper, 0.001)
    }
}
