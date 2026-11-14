package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebouncerTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `single event fires`() {
        val c = FakeClock()
        val d = Debouncer(quietMillis = 100, now = c::now)
        val fireAt = d.accept()
        // Last-recorded is c.nowMs; shouldFire(c.nowMs) should be true.
        assertTrue(d.shouldFire(c.nowMs))
        assertEquals(c.nowMs + 100, fireAt)
    }

    @Test
    fun `second event supersedes first`() {
        val c = FakeClock(nowMs = 1000)
        val d = Debouncer(quietMillis = 100, now = c::now)
        d.accept()                       // last = 1000
        val firstTs = c.nowMs
        c.advance(50)
        d.accept()                       // last = 1050
        // Checking the first event's planned fire time now fails:
        assertFalse(d.shouldFire(firstTs))
        // The second one is still the latest:
        assertTrue(d.shouldFire(c.nowMs))
    }

    @Test
    fun `reset clears`() {
        val c = FakeClock()
        val d = Debouncer(quietMillis = 100, now = c::now)
        d.accept()
        d.reset()
        assertFalse(d.shouldFire(c.nowMs))
    }
}
