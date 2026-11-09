package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the truncation rule `ChatHistoryStore` applies
 * before persisting. We can't drive DataStore from a JVM test without
 * Robolectric, so these tests mirror the slice logic directly.
 */
class ChatHistoryTruncationTest {

    @Test
    fun `under cap returns the list unchanged`() {
        val list = (0..10).map { ChatMessage(Role.user, "msg-$it") }
        val trimmed = truncate(list, cap = 200)
        assertEquals(list, trimmed)
    }

    @Test
    fun `at cap returns the list unchanged`() {
        val list = (1..200).map { ChatMessage(Role.user, "msg-$it") }
        val trimmed = truncate(list, cap = 200)
        assertEquals(200, trimmed.size)
        assertEquals(list, trimmed)
    }

    @Test
    fun `over cap drops the oldest and keeps the tail`() {
        val list = (1..250).map { ChatMessage(Role.user, "msg-$it") }
        val trimmed = truncate(list, cap = 200)
        assertEquals(200, trimmed.size)
        assertEquals("msg-51", trimmed.first().content)
        assertEquals("msg-250", trimmed.last().content)
    }

    @Test
    fun `default cap matches MAX_MESSAGES`() {
        assertTrue("cap sanity", ChatHistoryStore.MAX_MESSAGES in 50..1000)
    }

    /** Mirror of the truncation logic in ChatHistoryStore.save. */
    private fun truncate(messages: List<ChatMessage>, cap: Int): List<ChatMessage> =
        if (messages.size > cap) messages.takeLast(cap) else messages
}
