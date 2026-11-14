package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeAgoTest {

    private class FakeClock(var nowMs: Long) {
        fun now(): Long = nowMs
    }

    @Test
    fun `under 1 minute reads just now`() {
        val c = FakeClock(100_000)
        val t = TimeAgo(c::now)
        assertEquals("just now", t.format(c.nowMs - 10_000))
    }

    @Test
    fun `minutes`() {
        val c = FakeClock(100_000_000)
        val t = TimeAgo(c::now)
        assertEquals("5m ago", t.format(c.nowMs - 5 * 60_000))
    }

    @Test
    fun `hours`() {
        val c = FakeClock(100_000_000)
        val t = TimeAgo(c::now)
        assertEquals("3h ago", t.format(c.nowMs - 3 * 3_600_000))
    }

    @Test
    fun `days`() {
        val c = FakeClock(100_000_000_000)
        val t = TimeAgo(c::now)
        assertEquals("4d ago", t.format(c.nowMs - 4L * 86_400_000))
    }

    @Test
    fun `months`() {
        val c = FakeClock(100_000_000_000)
        val t = TimeAgo(c::now)
        assertEquals("2mo ago", t.format(c.nowMs - 60L * 86_400_000))
    }

    @Test
    fun `years`() {
        val c = FakeClock(100_000_000_000)
        val t = TimeAgo(c::now)
        assertEquals("2y ago", t.format(c.nowMs - 800L * 86_400_000))
    }

    @Test
    fun `future returns soon`() {
        val c = FakeClock(100_000)
        val t = TimeAgo(c::now)
        assertEquals("soon", t.format(c.nowMs + 5000))
    }
}
