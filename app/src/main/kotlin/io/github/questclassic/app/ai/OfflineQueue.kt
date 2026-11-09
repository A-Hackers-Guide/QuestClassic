package io.github.questclassic.app.ai

/**
 * In-memory FIFO queue for AI backend requests that couldn't be sent
 * because the device was offline. When connectivity returns, the UI
 * drains the queue by calling [drain] with a per-item send lambda.
 *
 * Pure in-memory; survives in-process restarts only. A future revision
 * may add DataStore persistence, but requests are often time-sensitive
 * ("what's the weather now?") and queueing them for weeks is more
 * confusing than useful.
 *
 * Thread-safe via `@Synchronized`.
 */
class OfflineQueue(
    /** Hard cap; oldest entries are dropped when the cap is hit. */
    val capacity: Int = 32,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init { require(capacity in 1..1024) { "capacity out of range: $capacity" } }

    data class QueuedRequest(
        val id: String,
        val backendId: String,
        val payload: String,
        val enqueuedAtMs: Long,
    )

    private val items = ArrayDeque<QueuedRequest>()
    @Volatile private var dropped: Int = 0

    @Synchronized
    fun enqueue(id: String, backendId: String, payload: String) {
        if (items.size >= capacity) {
            items.removeFirst()
            dropped++
        }
        items.addLast(QueuedRequest(id, backendId, payload, now()))
    }

    @Synchronized
    fun snapshot(): List<QueuedRequest> = items.toList()

    @Synchronized
    fun size(): Int = items.size

    /** How many requests were dropped since construction because of cap. */
    @Synchronized
    fun droppedCount(): Int = dropped

    @Synchronized
    fun clear() {
        items.clear()
        dropped = 0
    }

    /**
     * Drain the queue in order. [send] receives each request; return true
     * to confirm it was sent (removed), false to leave it in the queue
     * (and stop draining). Returns the count sent.
     *
     * Not synchronized across the send call: [send] can suspend / await
     * the network, which would otherwise block further enqueues.
     */
    suspend fun drain(send: suspend (QueuedRequest) -> Boolean): Int {
        var sent = 0
        while (true) {
            val next = synchronized(this) { items.firstOrNull() }
                ?: break
            val ok = send(next)
            if (!ok) break
            synchronized(this) {
                // Only pop if it's still the head (in case someone else
                // cleared / rearranged while send() was suspended).
                if (items.firstOrNull()?.id == next.id) items.removeFirst()
            }
            sent++
        }
        return sent
    }
}
