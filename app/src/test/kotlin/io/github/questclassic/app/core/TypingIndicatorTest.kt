package io.github.questclassic.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingIndicatorTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `initial not typing`() {
        val c = FakeClock()
        val t = TypingIndicator(now = c::now)
        assertFalse(t.isTyping())
    }

    @Test
    fun `keystroke marks typing`() {
        val c = FakeClock()
        val t = TypingIndicator(quietMillis = 1000, now = c::now)
        t.onKeystroke()
        assertTrue(t.isTyping())
    }

    @Test
    fun `after quiet window not typing`() {
        val c = FakeClock()
        val t = TypingIndicator(quietMillis = 1000, now = c::now)
        t.onKeystroke()
        c.advance(1500)
        assertFalse(t.isTyping())
    }

    @Test
    fun `new keystroke resets window`() {
        val c = FakeClock()
        val t = TypingIndicator(quietMillis = 1000, now = c::now)
        t.onKeystroke()
        c.advance(900)
        t.onKeystroke()
        c.advance(500) // 1400 since first, 500 since second
        assertTrue(t.isTyping())
    }

    @Test
    fun `reset makes not typing`() {
        val c = FakeClock()
        val t = TypingIndicator(now = c::now)
        t.onKeystroke()
        t.reset()
        assertFalse(t.isTyping())
    }
}
