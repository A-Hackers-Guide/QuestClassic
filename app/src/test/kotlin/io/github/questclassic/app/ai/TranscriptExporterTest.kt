package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptExporterTest {

    private val sample = listOf(
        ChatMessage(Role.user, "Hello, Nova."),
        ChatMessage(Role.assistant, "Hi there!"),
        ChatMessage(Role.system, "earlier conversation summary"),
    )

    @Test
    fun `markdown has the header and every message`() {
        val md = TranscriptExporter.export(
            backendId = "gemini",
            assistantName = "Nova",
            history = sample,
            format = TranscriptExporter.Format.MARKDOWN,
        )
        assertTrue(md.contains("# Transcript — Nova (gemini)"))
        assertTrue(md.contains("**You**"))
        assertTrue(md.contains("**Nova**"))
        assertTrue(md.contains("_system_"))
        assertTrue(md.contains("Hello, Nova."))
        assertTrue(md.contains("Hi there!"))
    }

    @Test
    fun `markdown redacts an embedded API key`() {
        val leaky = listOf(
            ChatMessage(Role.user, "my key is sk-ant-" + "a".repeat(40) + " please help"),
        )
        val md = TranscriptExporter.export(
            backendId = "claude",
            assistantName = "Classic",
            history = leaky,
            format = TranscriptExporter.Format.MARKDOWN,
        )
        assertFalse("api key leaked: $md", md.contains("a".repeat(40)))
        assertTrue(md.contains("anthropic-key"))
    }

    @Test
    fun `json round trip preserves backend and assistant name`() {
        val json = TranscriptExporter.export(
            backendId = "openai",
            assistantName = "Sage",
            history = sample,
            format = TranscriptExporter.Format.JSON,
        )
        assertTrue(json.contains("\"backendId\": \"openai\""))
        assertTrue(json.contains("\"assistantName\": \"Sage\""))
        assertTrue(json.contains("\"version\": 1"))
    }

    @Test
    fun `json redacts secrets`() {
        val leaky = listOf(
            ChatMessage(Role.user, "token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.dGVzdHRlc3R0ZXN0test"),
        )
        val json = TranscriptExporter.export(
            backendId = "x",
            assistantName = "X",
            history = leaky,
            format = TranscriptExporter.Format.JSON,
        )
        assertTrue(json.contains("jwt"))
    }

    @Test
    fun `empty history emits a header with zero count`() {
        val md = TranscriptExporter.export(
            backendId = "ollama",
            assistantName = "Local",
            history = emptyList(),
            format = TranscriptExporter.Format.MARKDOWN,
        )
        assertTrue(md.contains("Messages: 0"))
    }

    @Test
    fun `markdown ends in a single trailing newline`() {
        val md = TranscriptExporter.export(
            backendId = "x", assistantName = "X",
            history = sample,
            format = TranscriptExporter.Format.MARKDOWN,
        )
        assertTrue(md.endsWith("\n"))
        assertFalse(md.endsWith("\n\n"))
    }
}
