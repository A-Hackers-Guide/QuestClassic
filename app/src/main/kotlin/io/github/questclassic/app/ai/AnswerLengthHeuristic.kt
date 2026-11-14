package io.github.questclassic.app.ai

/**
 * Picks a max-token suggestion for the AI reply based on the shape
 * of the user's question. Sent to backends as 'max_tokens' so the
 * model doesn't produce a 2k-token essay in response to 'what time
 * is it'.
 *
 * Heuristic, not a classifier — short decisive questions get short
 * caps, explain-this-code questions get big ones, open-ended 'tell
 * me about X' falls in between.
 *
 * Pure function; unit-testable.
 */
object AnswerLengthHeuristic {

    enum class Bucket(val maxTokens: Int) {
        TERSE(256),       // 'what's the weather?'
        STANDARD(1024),   // most queries
        DETAILED(4096),   // 'explain this stack trace'
    }

    /** Pick a bucket based on simple structural cues in [question]. */
    fun bucket(question: String): Bucket {
        val q = question.lowercase().trim()
        if (q.length < 32 && q.endsWith('?') && q.split(' ').size <= 7) return Bucket.TERSE
        // Short decisive questions ('is X', 'does Y', 'can Z') tend to want short answers.
        if (q.length < 64 && (q.startsWith("is ") || q.startsWith("are ") || q.startsWith("does ") ||
                q.startsWith("do ") || q.startsWith("can ") || q.startsWith("will ") ||
                q.startsWith("should "))) return Bucket.TERSE
        // Code / stack trace / long multi-line input leans DETAILED.
        if (q.length > 500 || q.contains("```") || q.lines().size > 6) return Bucket.DETAILED
        if (q.startsWith("explain ") || q.startsWith("walk me through ") ||
            q.startsWith("write ") || q.startsWith("draft ") || q.startsWith("plan ")) return Bucket.DETAILED
        return Bucket.STANDARD
    }
}
