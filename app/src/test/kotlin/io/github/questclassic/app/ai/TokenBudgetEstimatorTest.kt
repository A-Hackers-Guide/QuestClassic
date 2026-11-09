package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenBudgetEstimatorTest {

    @Test
    fun `empty input is zero tokens`() {
        assertEquals(0, TokenBudgetEstimator.tokensIn(""))
    }

    @Test
    fun `short input gets at least 1 token`() {
        assertEquals(1, TokenBudgetEstimator.tokensIn("a"))
    }

    @Test
    fun `latin chars divide by about 4`() {
        val input = "a".repeat(100)
        val t = TokenBudgetEstimator.tokensIn(input)
        assertEquals(25, t)
    }

    @Test
    fun `CJK gets denser tokens`() {
        // 60-char input fully CJK: density 3 → 20 tokens, not 15.
        val input = "東".repeat(60)
        val t = TokenBudgetEstimator.tokensIn(input)
        assertEquals(20, t)
    }

    @Test
    fun `mixed under 20 percent non-latin keeps latin density`() {
        val input = "a".repeat(100) + "東" // 1/101 non-Latin
        val t = TokenBudgetEstimator.tokensIn(input)
        assertEquals(26, t) // ceil(101/4)
    }

    @Test
    fun `estimateChat sums history plus overhead`() {
        val history = listOf(
            ChatMessage(Role.user, "a".repeat(100)),
            ChatMessage(Role.assistant, "b".repeat(100)),
        )
        val est = TokenBudgetEstimator.estimateChat(history, systemPrompt = "")
        // 25 + 4 + 25 + 4 = 58 tokens
        assertEquals(58, est.tokens)
        assertFalse(est.overBudget)
    }

    @Test
    fun `over-budget flag flips`() {
        val history = listOf(ChatMessage(Role.user, "a".repeat(400_000)))
        val est = TokenBudgetEstimator.estimateChat(history, budget = 1_000)
        assertTrue(est.overBudget)
        assertEquals(999, est.percent) // capped
    }

    @Test
    fun `default budgets differ per backend`() {
        assertEquals(200_000, TokenBudgetEstimator.defaultBudgetFor("claude"))
        assertEquals(128_000, TokenBudgetEstimator.defaultBudgetFor("openai"))
        assertEquals(1_000_000, TokenBudgetEstimator.defaultBudgetFor("gemini"))
        assertEquals(8_192, TokenBudgetEstimator.defaultBudgetFor("ollama"))
    }

    @Test
    fun `unknown backend gets conservative default`() {
        assertEquals(32_000, TokenBudgetEstimator.defaultBudgetFor("future"))
    }
}
