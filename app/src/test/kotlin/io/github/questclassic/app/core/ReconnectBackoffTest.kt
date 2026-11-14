package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectBackoffTest {

    @Test
    fun `first attempt near base`() {
        val b = ReconnectBackoff(baseMillis = 500, maxMillis = 30_000, jitterPercent = 0, random = { 0.5 })
        assertEquals(500L, b.nextDelayMs())
    }

    @Test
    fun `backoff doubles`() {
        val b = ReconnectBackoff(baseMillis = 500, maxMillis = 30_000, jitterPercent = 0, random = { 0.5 })
        assertEquals(500L, b.nextDelayMs())
        assertEquals(1000L, b.nextDelayMs())
        assertEquals(2000L, b.nextDelayMs())
        assertEquals(4000L, b.nextDelayMs())
    }

    @Test
    fun `capped at max`() {
        val b = ReconnectBackoff(baseMillis = 500, maxMillis = 2000, jitterPercent = 0, random = { 0.5 })
        repeat(10) { b.nextDelayMs() }
        assertEquals(2000L, b.nextDelayMs())
    }

    @Test
    fun `jitter spreads around base`() {
        val lowBound = ReconnectBackoff(baseMillis = 1000, maxMillis = 1000, jitterPercent = 20, random = { 0.0 })
        val highBound = ReconnectBackoff(baseMillis = 1000, maxMillis = 1000, jitterPercent = 20, random = { 1.0 })
        val lo = lowBound.nextDelayMs()
        val hi = highBound.nextDelayMs()
        assertTrue("got lo=$lo hi=$hi", lo < 1000 && hi > 1000)
    }

    @Test
    fun `reset returns to base`() {
        val b = ReconnectBackoff(baseMillis = 500, maxMillis = 30_000, jitterPercent = 0, random = { 0.5 })
        b.nextDelayMs(); b.nextDelayMs()
        b.reset()
        assertEquals(500L, b.nextDelayMs())
    }

    @Test
    fun `attempt count tracks`() {
        val b = ReconnectBackoff(baseMillis = 500, maxMillis = 30_000, jitterPercent = 0, random = { 0.5 })
        b.nextDelayMs(); b.nextDelayMs()
        assertEquals(2, b.attemptCount())
    }

    @Test
    fun `invalid base throws`() {
        var threw = false
        try { ReconnectBackoff(baseMillis = 0, maxMillis = 1000) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
