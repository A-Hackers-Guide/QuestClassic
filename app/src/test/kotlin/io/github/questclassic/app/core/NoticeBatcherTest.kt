package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeBatcherTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `first in window posts`() {
        val c = FakeClock()
        val b = NoticeBatcher(now = c::now)
        assertTrue(b.shouldPost("AI", "rate limited") is NoticeBatcher.Decision.Post)
    }

    @Test
    fun `repeated in window skips`() {
        val c = FakeClock()
        val b = NoticeBatcher(windowMs = 1000, now = c::now)
        b.shouldPost("AI", "rate limited")
        for (i in 2..9) {
            assertTrue(b.shouldPost("AI", "rate limited") is NoticeBatcher.Decision.Skip)
        }
    }

    @Test
    fun `every 10th re-posts with suffix`() {
        val c = FakeClock()
        val b = NoticeBatcher(windowMs = 1000, now = c::now)
        for (i in 1..9) b.shouldPost("AI", "rate limited")
        val tenth = b.shouldPost("AI", "rate limited")
        assertTrue(tenth is NoticeBatcher.Decision.Post)
        assertEquals("(×10)", (tenth as NoticeBatcher.Decision.Post).suffix.trim())
    }

    @Test
    fun `after window posts fresh`() {
        val c = FakeClock()
        val b = NoticeBatcher(windowMs = 1000, now = c::now)
        b.shouldPost("AI", "rate limited")
        c.advance(2000)
        val fresh = b.shouldPost("AI", "rate limited")
        assertTrue(fresh is NoticeBatcher.Decision.Post)
        assertEquals("", (fresh as NoticeBatcher.Decision.Post).suffix)
    }

    @Test
    fun `different keys do not interfere`() {
        val c = FakeClock()
        val b = NoticeBatcher(windowMs = 1000, now = c::now)
        assertTrue(b.shouldPost("AI", "a") is NoticeBatcher.Decision.Post)
        assertTrue(b.shouldPost("AI", "b") is NoticeBatcher.Decision.Post)
        assertTrue(b.shouldPost("SECURITY", "a") is NoticeBatcher.Decision.Post)
    }

    @Test
    fun `clear empties`() {
        val c = FakeClock()
        val b = NoticeBatcher(windowMs = 1000, now = c::now)
        b.shouldPost("AI", "x")
        b.clear()
        assertTrue(b.shouldPost("AI", "x") is NoticeBatcher.Decision.Post)
    }
}
