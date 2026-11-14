package io.github.questclassic.app.core

/**
 * Collapses repeated notices with the same (category, title) into one
 * 'N occurrences' row within a time window, so a burst of
 * 'rate-limited' notices from RateLimitedBackend doesn't flood the
 * NotificationCenter.
 *
 * The caller (NotificationCenter wrapper, future) calls [shouldPost]
 * for every notice; the return is either Post (post the notice,
 * optionally as a batched summary) or Skip.
 *
 * Pure function around an injectable clock; batcher state is a map
 * keyed by (category, title).
 */
class NoticeBatcher(
    val windowMs: Long = 30_000,
    private val now: () -> Long = System::currentTimeMillis,
) {
    sealed interface Decision {
        data class Post(val suffix: String = "") : Decision
        data object Skip : Decision
    }

    private data class State(val firstMs: Long, var count: Int)

    private val states = mutableMapOf<String, State>()

    @Synchronized
    fun shouldPost(category: String, title: String): Decision {
        val key = "$category/$title"
        val existing = states[key]
        val t = now()
        if (existing == null || t - existing.firstMs > windowMs) {
            states[key] = State(t, 1)
            return Decision.Post()
        }
        existing.count++
        // Every 10 occurrences re-post with a suffix so the user
        // sees 'x10' etc. without being spammed every time.
        if (existing.count % 10 == 0) {
            return Decision.Post(suffix = " (×${existing.count})")
        }
        return Decision.Skip
    }

    @Synchronized
    fun clear() = states.clear()
}
