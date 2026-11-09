package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordDispatcherTest {

    private class FakeBackend(
        override val id: String,
        override val displayName: String,
    ) : AIBackend {
        override suspend fun chat(
            systemPrompt: String,
            history: List<ChatMessage>,
            apiKey: String,
            model: String?,
        ): Result<String> = Result.success("fake-$id")
    }

    private val backends = listOf(
        FakeBackend("claude", "Claude"),
        FakeBackend("openai", "OpenAI"),
        FakeBackend("gemini", "Gemini"),
        FakeBackend("ollama", "Ollama"),
    )
    private val dispatcher = WakeWordDispatcher()

    private fun cfg(
        active: String = "claude",
        names: Map<String, String> = emptyMap(),
    ) = AIConfig(
        assistantName = "Classic",
        backendId = active,
        model = "m",
        apiKey = "",
        systemPrompt = "",
        perBackendNames = names,
        perBackendKeys = emptyMap(),
    )

    @Test
    fun `routes to the backend whose wake name matched`() {
        val d = dispatcher.dispatch(
            "Hey Nova, hello there",
            cfg(active = "claude", names = mapOf("gemini" to "Nova")),
            backends,
        )
        assertEquals("gemini", d.backend.id)
        assertTrue(d.wakeMatched)
        assertEquals("Nova", d.wakeName)
        assertEquals("hello there", d.payload)
    }

    @Test
    fun `also matches by built-in display name`() {
        val d = dispatcher.dispatch(
            "Hey OpenAI, hi",
            cfg(active = "claude"),
            backends,
        )
        assertEquals("openai", d.backend.id)
        assertTrue(d.wakeMatched)
    }

    @Test
    fun `user-set name wins over built-in name on collision`() {
        // A user maps Claude's wake name to "Gemini"; that custom override
        // beats Gemini's built-in display name.
        val d = dispatcher.dispatch(
            "Hey Gemini, hi",
            cfg(active = "claude", names = mapOf("claude" to "Gemini")),
            backends,
        )
        assertEquals("claude", d.backend.id)
    }

    @Test
    fun `no wake phrase falls back to active backend with full utterance`() {
        val d = dispatcher.dispatch(
            "what time is it",
            cfg(active = "ollama"),
            backends,
        )
        assertEquals("ollama", d.backend.id)
        assertFalse(d.wakeMatched)
        assertNull(d.wakeName)
        assertEquals("what time is it", d.payload)
    }

    @Test
    fun `wake name pointing at missing backend falls back`() {
        // User mapped "Ghost" to a backend id nobody registered.
        val d = dispatcher.dispatch(
            "Hey Ghost, hi",
            cfg(active = "claude", names = mapOf("ghost" to "Ghost")),
            backends,
        )
        assertFalse(d.wakeMatched)
        assertEquals("claude", d.backend.id)
    }

    @Test
    fun `active backend missing falls back to first registered`() {
        val d = dispatcher.dispatch(
            "plain text",
            cfg(active = "nonexistent"),
            backends,
        )
        assertEquals(backends.first().id, d.backend.id)
    }
}
