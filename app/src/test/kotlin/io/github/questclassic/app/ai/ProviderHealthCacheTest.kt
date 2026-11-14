package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderHealthCacheTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `unrecorded returns null`() {
        val c = ProviderHealthCache(now = FakeClock()::now)
        assertNull(c.healthOf("claude"))
    }

    @Test
    fun `record success then read`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(now = clock::now)
        c.recordSuccess("claude")
        assertTrue(c.healthOf("claude") is ProviderHealthCache.Health.Healthy)
    }

    @Test
    fun `record failure then read carries reason`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(now = clock::now)
        c.recordFailure("openai", "429")
        val h = c.healthOf("openai") as ProviderHealthCache.Health.Unhealthy
        assertEquals("429", h.reason)
    }

    @Test
    fun `ttl expiry returns null`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(ttlMillis = 1000, now = clock::now)
        c.recordSuccess("claude")
        clock.advance(2000)
        assertNull(c.healthOf("claude"))
    }

    @Test
    fun `snapshot filters expired`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(ttlMillis = 1000, now = clock::now)
        c.recordSuccess("claude")
        clock.advance(500)
        c.recordSuccess("openai")
        clock.advance(600) // claude is 1100ms old → expired; openai 600ms
        val snap = c.snapshot()
        assertTrue("openai" in snap)
        assertTrue("claude" !in snap)
    }

    @Test
    fun `backendId case normalised`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(now = clock::now)
        c.recordSuccess("Claude")
        assertTrue(c.healthOf("claude") is ProviderHealthCache.Health.Healthy)
    }

    @Test
    fun `clear empties`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(now = clock::now)
        c.recordSuccess("x")
        c.clear()
        assertNull(c.healthOf("x"))
    }

    @Test
    fun `success overwrites failure`() {
        val clock = FakeClock()
        val c = ProviderHealthCache(now = clock::now)
        c.recordFailure("x", "429")
        c.recordSuccess("x")
        assertTrue(c.healthOf("x") is ProviderHealthCache.Health.Healthy)
    }
}
