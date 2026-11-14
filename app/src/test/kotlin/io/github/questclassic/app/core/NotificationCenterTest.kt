package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCenterTest {

    private class FakeClock(var nowMs: Long = 1000L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `post adds to the list`() {
        val c = NotificationCenter()
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.SYSTEM, "hi")
        assertEquals(1, c.notices.value.size)
    }

    @Test
    fun `cap drops oldest`() {
        val c = NotificationCenter(capacity = 2)
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.SYSTEM, "a")
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.SYSTEM, "b")
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.SYSTEM, "c")
        assertEquals(listOf("b", "c"), c.notices.value.map { it.title })
    }

    @Test
    fun `markAllRead marks everything`() {
        val c = NotificationCenter()
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "a")
        c.post(NotificationCenter.Severity.WARN, NotificationCenter.Category.AI, "b")
        c.markAllRead()
        assertTrue(c.notices.value.all { it.read })
    }

    @Test
    fun `markRead marks only the target`() {
        val c = NotificationCenter()
        val n = c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "a")
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "b")
        c.markRead(n.id)
        val list = c.notices.value
        assertTrue(list.first { it.id == n.id }.read)
        assertFalse(list.first { it.id != n.id }.read)
    }

    @Test
    fun `clear category keeps others`() {
        val c = NotificationCenter()
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "a")
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.UPDATE, "b")
        c.clear(category = NotificationCenter.Category.AI)
        assertEquals(1, c.notices.value.size)
        assertEquals("b", c.notices.value.first().title)
    }

    @Test
    fun `clear with null empties everything`() {
        val c = NotificationCenter()
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "a")
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.UPDATE, "b")
        c.clear()
        assertEquals(0, c.notices.value.size)
    }

    @Test
    fun `unreadCount filters by severity`() {
        val c = NotificationCenter()
        c.post(NotificationCenter.Severity.INFO, NotificationCenter.Category.AI, "a")
        c.post(NotificationCenter.Severity.WARN, NotificationCenter.Category.AI, "b")
        c.post(NotificationCenter.Severity.ERROR, NotificationCenter.Category.AI, "c")
        assertEquals(3, c.unreadCount())
        assertEquals(2, c.unreadCount(atLeast = NotificationCenter.Severity.WARN))
        assertEquals(1, c.unreadCount(atLeast = NotificationCenter.Severity.ERROR))
    }

    @Test
    fun `invalid capacity throws`() {
        var threw = false
        try { NotificationCenter(capacity = 0) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
