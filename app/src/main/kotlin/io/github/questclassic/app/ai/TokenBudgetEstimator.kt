package io.github.questclassic.app.ai

/**
 * Rough chars → tokens estimate so the UI can show 'this prompt is
 * ~3,200 tokens (budget: 200,000)' before the send and the user can
 * decide whether to trim.
 *
 * NOT a real tokenizer — we don't want to ship BPE tables and a
 * tokenizer per provider for a progress-bar feature. Instead:
 *
 *  - Latin-script text averages ~4 characters per token (from Anthropic
 *    / OpenAI published guidance).
 *  - CJK / Arabic / Cyrillic scripts tokenize more densely (1-2 chars
 *    per token). We apply a 1.3× penalty when non-Latin codepoints
 *    exceed 20% of the input — the right number is ~2.5 chars/token
 *    for CJK, so 4/1.3 ≈ 3 is a reasonable compromise that keeps the
 *    heuristic conservative.
 *  - Whitespace counts normally.
 *
 * This is a BUDGETING tool, not a BILLING tool. Accuracy within 20%
 * of the real count is enough for the UI chip.
 */
object TokenBudgetEstimator {

    data class Estimate(
        val tokens: Int,
        val budget: Int,
        val percent: Int,
        /** True when the estimate exceeds the budget. */
        val overBudget: Boolean,
    )

    /** Rough estimate for a single string. */
    fun tokensIn(text: String): Int {
        if (text.isEmpty()) return 0
        val nonLatin = text.count { it.code > 0x024F }
        val density = if (nonLatin > text.length * 0.2) 3 else 4
        // 1-token overhead per message is a reasonable rough-add
        // so a one-char input doesn't claim 0 tokens.
        return ((text.length + density - 1) / density).coerceAtLeast(1)
    }

    /** Estimate for a chat history being sent. */
    fun estimateChat(history: List<ChatMessage>, systemPrompt: String = "", budget: Int = 200_000): Estimate {
        val total = tokensIn(systemPrompt) +
            history.sumOf { tokensIn(it.content) + 4 /* role + separators */ }
        val pct = if (budget <= 0) 0 else (total * 100 / budget).coerceAtMost(999)
        return Estimate(
            tokens = total,
            budget = budget,
            percent = pct,
            overBudget = total > budget,
        )
    }

    /** Common per-provider budgets so the UI can pick the right ceiling. */
    fun defaultBudgetFor(backendId: String): Int = when (backendId.lowercase()) {
        "claude" -> 200_000
        "openai" -> 128_000
        "gemini" -> 1_000_000
        "ollama" -> 8_192
        else -> 32_000
    }
}
