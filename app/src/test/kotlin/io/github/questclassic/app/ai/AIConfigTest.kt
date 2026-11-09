package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [AIConfig]'s wake-name routing. Persistence (DataStore)
 * lives in the Android layer and is exercised by instrumentation tests
 * elsewhere; these just pin the data-class semantics.
 */
class AIConfigTest {

    @Test
    fun `wakeNameFor returns the per-backend entry when present`() {
        val cfg = AIConfig(
            assistantName = "Classic",
            backendId = "claude",
            model = "",
            apiKey = "",
            systemPrompt = "",
            perBackendNames = mapOf(
                "claude" to "Classic",
                "openai" to "Sage",
                "gemini" to "Nova",
            ),
        )
        assertEquals("Classic", cfg.wakeNameFor("claude"))
        assertEquals("Sage", cfg.wakeNameFor("openai"))
        assertEquals("Nova", cfg.wakeNameFor("gemini"))
    }

    @Test
    fun `wakeNameFor falls back when the entry is missing or blank`() {
        val cfg = AIConfig(
            assistantName = "Classic",
            backendId = "claude",
            model = "",
            apiKey = "",
            systemPrompt = "",
            perBackendNames = mapOf("gemini" to "   "),
        )
        assertEquals("Classic", cfg.wakeNameFor("openai"))
        assertEquals("Classic", cfg.wakeNameFor("gemini"))
    }

    @Test
    fun `wakeNameFor uses supplied fallback`() {
        val cfg = AIConfig(
            assistantName = "Classic",
            backendId = "claude",
            model = "",
            apiKey = "",
            systemPrompt = "",
        )
        assertEquals("Fallback", cfg.wakeNameFor("ollama", fallback = "Fallback"))
    }
}
