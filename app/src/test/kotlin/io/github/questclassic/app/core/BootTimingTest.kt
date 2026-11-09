package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootTimingTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `first mark establishes t=0`() {
        val clock = FakeClock(1_000_000L)
        val bt = BootTiming(clock::now)
        bt.mark("app.created")
        assertEquals(0L, bt.snapshot().first().elapsedSinceStartMs)
    }

    @Test
    fun `subsequent marks are deltas from start`() {
        val clock = FakeClock(500L)
        val bt = BootTiming(clock::now)
        bt.mark("a")
        clock.advance(200)
        bt.mark("b")
        clock.advance(300)
        bt.mark("c")
        assertEquals(listOf(0L, 200L, 500L), bt.snapshot().map { it.elapsedSinceStartMs })
    }

    @Test
    fun `totalMs returns last delta`() {
        val clock = FakeClock()
        val bt = BootTiming(clock::now)
        bt.mark("a")
        clock.advance(100)
        bt.mark("b")
        assertEquals(100L, bt.totalMs())
    }

    @Test
    fun `totalMs is negative when nothing marked`() {
        val bt = BootTiming()
        assertEquals(-1L, bt.totalMs())
    }

    @Test
    fun `reset clears marks and start`() {
        val clock = FakeClock()
        val bt = BootTiming(clock::now)
        bt.mark("a")
        bt.reset()
        assertTrue(bt.snapshot().isEmpty())
        clock.advance(500)
        bt.mark("b")
        assertEquals(0L, bt.snapshot().first().elapsedSinceStartMs)
    }

    @Test
    fun `toJson contains labels and millis`() {
        val clock = FakeClock()
        val bt = BootTiming(clock::now)
        bt.mark("a")
        clock.advance(50)
        bt.mark("b")
        val json = bt.toJson()
        assertTrue(json.contains("\"a\""))
        assertTrue(json.contains("\"b\""))
        assertTrue(json.contains("\"ms\": 0"))
        assertTrue(json.contains("\"ms\": 50"))
    }

    @Test
    fun `toJson escapes quotes and backslashes in labels`() {
        val bt = BootTiming()
        bt.mark("weird\\\"label")
        val json = bt.toJson()
        assertTrue("got $json", json.contains("weird\\\\\\\"label"))
    }
}
