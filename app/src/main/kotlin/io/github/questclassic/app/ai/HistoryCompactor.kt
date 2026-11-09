package io.github.questclassic.app.ai

/**
 * Compacts a long chat history into (summary + recent) when the message
 * count gets near the backend's context window.
 *
 * Long histories are expensive to send and often slow to return. Instead
 * of truncating silently (which loses context), this module replaces the
 * **older** portion with a model-generated summary while keeping the
 * **recent** portion verbatim. The summary function is injected so the
 * compactor is pure and tests don't need a network.
 *
 * Caller decides when to compact: typically once [ChatMessage] count
 * exceeds [triggerCount], the next send triggers [compact] which:
 *
 *   1. Pulls out messages older than [keepRecent],
 *   2. Hands them to [summarize] (a suspend fn, usually a backend call),
 *   3. Replaces them with a single system message carrying the summary,
 *   4. Returns the new list the caller sends.
 */
class HistoryCompactor(
    val triggerCount: Int = 80,
    val keepRecent: Int = 20,
) {
    init {
        require(triggerCount > keepRecent) {
            "triggerCount ($triggerCount) must exceed keepRecent ($keepRecent)"
        }
        require(keepRecent >= 2) { "keepRecent must be at least 2" }
    }

    /** True once the history is long enough that [compact] should fire. */
    fun shouldCompact(history: List<ChatMessage>): Boolean =
        history.size >= triggerCount

    /**
     * Compact [history] if it's over the threshold, otherwise return as-is.
     *
     * [summarize] is called at most once; it receives the oldest
     * (history.size - keepRecent) messages and returns a text summary.
     * If [summarize] returns null or empty, the compactor is a no-op
     * (we'd rather keep the full history than drop context silently).
     */
    suspend fun compact(
        history: List<ChatMessage>,
        summarize: suspend (List<ChatMessage>) -> String?,
    ): List<ChatMessage> {
        if (!shouldCompact(history)) return history
        val splitIdx = history.size - keepRecent
        val older = history.subList(0, splitIdx)
        val recent = history.subList(splitIdx, history.size)

        val summary = summarize(older)?.takeIf { it.isNotBlank() } ?: return history

        val summaryMsg = ChatMessage(
            role = Role.system,
            content = "Earlier conversation summary (${older.size} messages compacted):\n$summary",
        )
        return listOf(summaryMsg) + recent
    }
}
