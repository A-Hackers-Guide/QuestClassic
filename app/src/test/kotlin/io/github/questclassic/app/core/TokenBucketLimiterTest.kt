package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenBucketLimiterTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `starts full`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 5.0, refillPerSecond = 1.0, now = clock::now)
        assertEquals(5.0, lim.available(), 0.001)
    }

    @Test
    fun `consume one`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 5.0, refillPerSecond = 1.0, now = clock::now)
        assertTrue(lim.tryAcquire())
        assertEquals(4.0, lim.available(), 0.001)
    }

    @Test
    fun `drain and refuse`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 3.0, refillPerSecond = 1.0, now = clock::now)
        repeat(3) { assertTrue(lim.tryAcquire()) }
        assertFalse(lim.tryAcquire())
    }

    @Test
    fun `refill after time passes`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 3.0, refillPerSecond = 1.0, now = clock::now)
        repeat(3) { lim.tryAcquire() }
        assertFalse(lim.tryAcquire())
        clock.advance(2000) // 2 seconds → 2 tokens
        assertTrue(lim.tryAcquire())
        assertTrue(lim.tryAcquire())
        assertFalse(lim.tryAcquire())
    }

    @Test
    fun `refill is capped at capacity`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 2.0, refillPerSecond = 1.0, now = clock::now)
        clock.advance(10_000) // 10 seconds
        // Capacity is 2, even though 10 seconds would add 10 tokens.
        assertEquals(2.0, lim.available(), 0.001)
    }

    @Test
    fun `secondsUntilAvailable is zero when a token is available`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 3.0, refillPerSecond = 1.0, now = clock::now)
        assertEquals(0.0, lim.secondsUntilAvailable(), 0.001)
    }

    @Test
    fun `secondsUntilAvailable scales with refill rate`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 1.0, refillPerSecond = 2.0, now = clock::now)
        lim.tryAcquire() // drain
        // 1 token at 2/sec = 0.5s
        assertEquals(0.5, lim.secondsUntilAvailable(), 0.001)
    }

    @Test
    fun `drain empties immediately`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 10.0, refillPerSecond = 1.0, now = clock::now)
        lim.drain()
        assertEquals(0.0, lim.available(), 0.001)
        assertFalse(lim.tryAcquire())
    }

    @Test
    fun `reset refills to capacity`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 5.0, refillPerSecond = 1.0, now = clock::now)
        repeat(5) { lim.tryAcquire() }
        lim.reset()
        assertEquals(5.0, lim.available(), 0.001)
    }

    @Test
    fun `cost of 2 consumes 2 tokens`() {
        val clock = FakeClock()
        val lim = TokenBucketLimiter(capacity = 3.0, refillPerSecond = 1.0, now = clock::now)
        assertTrue(lim.tryAcquire(cost = 2.0))
        assertEquals(1.0, lim.available(), 0.001)
        assertFalse(lim.tryAcquire(cost = 2.0))
    }

    @Test
    fun `invalid capacity throws`() {
        val clock = FakeClock()
        var threw = false
        try {
            TokenBucketLimiter(capacity = 0.0, refillPerSecond = 1.0, now = clock::now)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }
}
