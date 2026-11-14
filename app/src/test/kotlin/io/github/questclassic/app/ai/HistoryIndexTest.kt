package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryIndexTest {

    private val history = listOf(
        ChatMessage(Role.user, "What's the weather like in Berlin?"),
        ChatMessage(Role.assistant, "It's cold today."),
        ChatMessage(Role.user, "How about Zurich tomorrow?"),
        ChatMessage(Role.assistant, "Zurich is also cold tomorrow."),
    )

    @Test
    fun `empty query returns empty`() {
        assertTrue(HistoryIndex.search(history, "").isEmpty())
        assertTrue(HistoryIndex.search(history, "   ").isEmpty())
    }

    @Test
    fun `exact substring hits`() {
        val hits = HistoryIndex.search(history, "zurich")
        assertTrue(hits.any { it.message.content.contains("Zurich", ignoreCase = true) })
    }

    @Test
    fun `multi-token OR matching`() {
        val hits = HistoryIndex.search(history, "berlin tomorrow")
        assertEquals(3, hits.size)   // berlin / Zurich tomorrow / Zurich cold tomorrow
    }

    @Test
    fun `newer messages ranked higher on tie`() {
        val hits = HistoryIndex.search(history, "cold")
        // Both 'It's cold today' and 'Zurich is also cold tomorrow' match;
        // the latter is index 3 (newer) so it ranks first.
        assertEquals(3, hits.first().index)
    }

    @Test
    fun `no match returns empty`() {
        assertTrue(HistoryIndex.search(history, "pineapple").isEmpty())
    }

    @Test
    fun `max cap honoured`() {
        val many = List(50) { ChatMessage(Role.user, "cold banana $it") }
        val hits = HistoryIndex.search(many, "banana", max = 10)
        assertEquals(10, hits.size)
    }
}
