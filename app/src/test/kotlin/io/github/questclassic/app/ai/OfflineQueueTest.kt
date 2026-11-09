package io.github.questclassic.app.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueTest {

    private class FakeClock(var nowMs: Long = 0L) {
        fun now(): Long = nowMs
        fun advance(ms: Long) { nowMs += ms }
    }

    @Test
    fun `enqueue grows the queue`() {
        val q = OfflineQueue(capacity = 10)
        q.enqueue("a", "claude", "hello")
        q.enqueue("b", "claude", "world")
        assertEquals(2, q.size())
    }

    @Test
    fun `fifo order`() {
        val q = OfflineQueue(capacity = 10)
        q.enqueue("a", "claude", "first")
        q.enqueue("b", "claude", "second")
        assertEquals(listOf("a", "b"), q.snapshot().map { it.id })
    }

    @Test
    fun `cap drops oldest`() {
        val q = OfflineQueue(capacity = 3)
        q.enqueue("a", "claude", "1")
        q.enqueue("b", "claude", "2")
        q.enqueue("c", "claude", "3")
        q.enqueue("d", "claude", "4") // 'a' dropped
        assertEquals(listOf("b", "c", "d"), q.snapshot().map { it.id })
        assertEquals(1, q.droppedCount())
    }

    @Test
    fun `drain sends all when ok`() = runBlocking {
        val q = OfflineQueue(capacity = 5)
        q.enqueue("a", "c", "1")
        q.enqueue("b", "c", "2")
        val sent = q.drain { true }
        assertEquals(2, sent)
        assertEquals(0, q.size())
    }

    @Test
    fun `drain stops on first false`() = runBlocking {
        val q = OfflineQueue(capacity = 5)
        q.enqueue("a", "c", "1")
        q.enqueue("b", "c", "2")
        q.enqueue("c", "c", "3")
        var callCount = 0
        val sent = q.drain {
            callCount++
            callCount < 2 // first ok, second fails
        }
        assertEquals(1, sent)
        // After drain: 'a' sent, 'b' not (call returned false). Both 'b' and 'c' remain.
        assertEquals(2, q.size())
    }

    @Test
    fun `clear empties the queue and resets dropped`() {
        val q = OfflineQueue(capacity = 2)
        q.enqueue("a", "c", "1")
        q.enqueue("b", "c", "2")
        q.enqueue("c", "c", "3") // drops a
        q.clear()
        assertEquals(0, q.size())
        assertEquals(0, q.droppedCount())
    }

    @Test
    fun `enqueue captures clock-stamped time`() {
        val clock = FakeClock(nowMs = 42_000L)
        val q = OfflineQueue(capacity = 2, now = clock::now)
        q.enqueue("a", "c", "1")
        assertEquals(42_000L, q.snapshot().first().enqueuedAtMs)
    }

    @Test
    fun `invalid capacity throws`() {
        var threw = false
        try { OfflineQueue(capacity = 0) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        threw = false
        try { OfflineQueue(capacity = 10_000) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
