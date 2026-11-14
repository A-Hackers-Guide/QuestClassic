package io.github.questclassic.app.ai

/**
 * Lightweight substring + token-prefix search across a chat history
 * so the user can find 'that thing we were talking about yesterday'
 * without scrolling. Pure function; no inverted index because the
 * history cap (200 messages) is small enough that O(n) over the
 * tokens is cheaper than the structure.
 *
 * Returns matches ranked by:
 *   - exact substring score (contains = 2, word-prefix = 1).
 *   - newer messages first (so 'the one we just discussed' ranks
 *     higher than the one from an hour ago).
 *
 * Not a semantic search — users wanting synonym-match should ask the
 * AI itself via the normal send path.
 */
object HistoryIndex {

    data class Hit(val index: Int, val message: ChatMessage, val score: Int)

    fun search(history: List<ChatMessage>, query: String, max: Int = 20): List<Hit> {
        val q = query.trim().lowercase()
        if (q.isEmpty() || history.isEmpty()) return emptyList()
        val tokens = q.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val hits = mutableListOf<Hit>()
        for ((i, msg) in history.withIndex()) {
            val content = msg.content.lowercase()
            if (content.contains(q)) {
                hits += Hit(i, msg, score = 2 * tokens.size + i)
            } else if (tokens.any { tok -> content.contains(tok) }) {
                val matches = tokens.count { tok -> content.contains(tok) }
                hits += Hit(i, msg, score = matches + i / 10)
            }
        }
        return hits.sortedWith(compareByDescending<Hit> { it.score }.thenByDescending { it.index })
            .take(max)
    }
}
