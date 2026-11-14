package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRingBufferTest {

    private class FakeClock(var nowMs: Long = 1_000L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `info write appears in snapshot`() {
        val b = LogRingBuffer(capacity = 16)
        b.info("tag", "hello")
        val list = b.snapshot()
        assertEquals(1, list.size)
        assertEquals(LogRingBuffer.Level.INFO, list.first().level)
        assertEquals("hello", list.first().message)
    }

    @Test
    fun `cap drops oldest`() {
        val b = LogRingBuffer(capacity = 16)
        for (i in 0..16) b.info("t", "m$i")
        assertEquals(16, b.size())
        // Oldest should be m1, not m0
        assertEquals("m1", b.snapshot().first().message)
    }

    @Test
    fun `secret redacted on write`() {
        val b = LogRingBuffer(capacity = 16)
        b.error("tag", "Boom! sk-ant-" + "a".repeat(40))
        val msg = b.snapshot().first().message
        assertFalse(msg.contains("a".repeat(40)))
        assertTrue(msg.contains("anthropic-key"))
    }

    @Test
    fun `format produces one line per entry`() {
        val b = LogRingBuffer(capacity = 16)
        b.info("A", "one")
        b.warn("B", "two")
        val lines = b.format().trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("INFO/A:"))
        assertTrue(lines[1].contains("WARN/B:"))
    }

    @Test
    fun `clear empties`() {
        val b = LogRingBuffer(capacity = 16)
        b.info("t", "x")
        b.clear()
        assertEquals(0, b.size())
    }

    @Test
    fun `snapshotAt filters by severity`() {
        val b = LogRingBuffer(capacity = 16)
        b.debug("t", "d")
        b.info("t", "i")
        b.warn("t", "w")
        b.error("t", "e")
        assertEquals(2, b.snapshotAt(LogRingBuffer.Level.WARN).size)
        assertEquals(1, b.snapshotAt(LogRingBuffer.Level.ERROR).size)
    }

    @Test
    fun `clock-stamped timestamps match the injected clock`() {
        val clock = FakeClock(nowMs = 42_000L)
        val b = LogRingBuffer(capacity = 16, now = clock::now)
        b.info("t", "a")
        assertEquals(42_000L, b.snapshot().first().timestampMs)
    }

    @Test
    fun `invalid capacity throws`() {
        var threw = false
        try { LogRingBuffer(capacity = 8) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        threw = false
        try { LogRingBuffer(capacity = 10_000) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
