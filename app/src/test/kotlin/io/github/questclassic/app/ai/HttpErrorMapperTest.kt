package io.github.questclassic.app.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorMapperTest {

    @Test
    fun `200 maps to null`() {
        assertNull(HttpErrorMapper.humanize(200, "claude"))
    }

    @Test
    fun `401 mentions Settings`() {
        val msg = HttpErrorMapper.humanize(401, "claude")!!
        assertTrue(msg.contains("Settings"))
        assertTrue(msg.contains("Anthropic"))
    }

    @Test
    fun `429 mentions rate limit`() {
        val msg = HttpErrorMapper.humanize(429, "openai")!!
        assertTrue(msg.lowercase().contains("rate"))
    }

    @Test
    fun `413 suggests clear`() {
        val msg = HttpErrorMapper.humanize(413, "claude")!!
        assertTrue(msg.contains("/clear"))
    }

    @Test
    fun `502 503 504 mention status page`() {
        for (code in listOf(502, 503, 504)) {
            val msg = HttpErrorMapper.humanize(code, "claude")!!
            assertTrue("got $msg", msg.contains("status"))
        }
    }

    @Test
    fun `4xx fallback for unmapped codes`() {
        val msg = HttpErrorMapper.humanize(418, "claude")!!
        assertTrue(msg.contains("HTTP 418"))
    }

    @Test
    fun `5xx fallback for unmapped codes`() {
        val msg = HttpErrorMapper.humanize(599, "openai")!!
        assertTrue(msg.contains("HTTP 599"))
    }

    @Test
    fun `provider display names`() {
        assertTrue(HttpErrorMapper.humanize(401, "claude")!!.contains("Anthropic"))
        assertTrue(HttpErrorMapper.humanize(401, "openai")!!.contains("OpenAI"))
        assertTrue(HttpErrorMapper.humanize(401, "gemini")!!.contains("Google"))
        assertTrue(HttpErrorMapper.humanize(401, "ollama")!!.contains("Ollama"))
    }

    @Test
    fun `unknown backend uses the raw id`() {
        assertTrue(HttpErrorMapper.humanize(401, "future-backend")!!.contains("future-backend"))
    }

    @Test
    fun `formatError appends short body`() {
        val out = HttpErrorMapper.formatError(401, "claude", "{\"error\":\"invalid key\"}")
        assertTrue(out.contains("Anthropic"))
        assertTrue(out.contains("invalid key"))
    }

    @Test
    fun `formatError omits blank body`() {
        val out = HttpErrorMapper.formatError(401, "claude", "")
        assertFalse(out.contains("("))
    }
}
