package io.github.questclassic.app.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCompactorTest {

    private fun msgs(n: Int): List<ChatMessage> =
        List(n) { ChatMessage(Role.user, "msg #$it") }

    @Test
    fun `short history is not compacted`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 80, keepRecent = 20)
        val h = msgs(50)
        val out = c.compact(h) { "summary" }
        assertEquals(h, out)
    }

    @Test
    fun `compact replaces older with one system summary`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        val h = msgs(20) // over threshold
        var summarizedCount = 0
        val out = c.compact(h) {
            summarizedCount = it.size
            "we talked about X"
        }
        assertEquals(1 + 3, out.size) // 1 summary + 3 recent
        assertEquals(Role.system, out.first().role)
        assertTrue(out.first().content.contains("we talked about X"))
        assertEquals(17, summarizedCount) // 20 - 3 kept
    }

    @Test
    fun `summary empty string keeps history verbatim`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        val h = msgs(15)
        val out = c.compact(h) { "" }
        assertEquals(h, out)
    }

    @Test
    fun `summary null keeps history verbatim`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        val h = msgs(15)
        val out = c.compact(h) { null }
        assertEquals(h, out)
    }

    @Test
    fun `exactly at trigger count triggers compact`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        assertTrue(c.shouldCompact(msgs(10)))
    }

    @Test
    fun `below trigger does not`() {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        assertFalse(c.shouldCompact(msgs(9)))
    }

    @Test
    fun `triggerCount must exceed keepRecent`() {
        var threw = false
        try {
            HistoryCompactor(triggerCount = 5, keepRecent = 5)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `keepRecent must be at least 2`() {
        var threw = false
        try {
            HistoryCompactor(triggerCount = 10, keepRecent = 1)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `preserves order of recent messages`() = runBlocking {
        val c = HistoryCompactor(triggerCount = 10, keepRecent = 3)
        val h = msgs(15)
        val out = c.compact(h) { "s" }
        // out[1] should be msg #12, out[2] #13, out[3] #14
        assertEquals("msg #12", out[1].content)
        assertEquals("msg #13", out[2].content)
        assertEquals("msg #14", out[3].content)
    }
}
