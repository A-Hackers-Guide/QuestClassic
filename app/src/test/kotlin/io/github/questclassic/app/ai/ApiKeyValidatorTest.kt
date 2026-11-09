package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyValidatorTest {

    @Test
    fun `claude key with correct prefix is ok`() {
        val v = ApiKeyValidator.validate("claude", "sk-ant-" + "a".repeat(40))
        assertEquals(ApiKeyValidator.Verdict.Ok, v)
    }

    @Test
    fun `claude key without prefix is an error`() {
        val v = ApiKeyValidator.validate("claude", "not-the-right-prefix-key")
        assertTrue(v is ApiKeyValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("sk-ant-"))
    }

    @Test
    fun `openai key starting with anthropic prefix is clear error`() {
        val v = ApiKeyValidator.validate("openai", "sk-ant-" + "a".repeat(40))
        assertTrue(v is ApiKeyValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("Claude backend"))
    }

    @Test
    fun `openai key normal prefix is ok`() {
        val v = ApiKeyValidator.validate("openai", "sk-" + "a".repeat(40))
        assertEquals(ApiKeyValidator.Verdict.Ok, v)
    }

    @Test
    fun `gemini key prefix is checked`() {
        val v = ApiKeyValidator.validate("gemini", "AIza" + "a".repeat(35))
        assertEquals(ApiKeyValidator.Verdict.Ok, v)
    }

    @Test
    fun `ollama empty is ok`() {
        val v = ApiKeyValidator.validate("ollama", "")
        assertEquals(ApiKeyValidator.Verdict.Ok, v)
    }

    @Test
    fun `claude empty is error`() {
        val v = ApiKeyValidator.validate("claude", "")
        assertTrue(v is ApiKeyValidator.Verdict.Error)
    }

    @Test
    fun `whitespace yields warn`() {
        val v = ApiKeyValidator.validate("claude", "  sk-ant-${"a".repeat(40)}  ")
        assertTrue(v is ApiKeyValidator.Verdict.Warn)
    }

    @Test
    fun `embedded space yields error about bearer header`() {
        val v = ApiKeyValidator.validate("claude", "Bearer sk-ant-${"a".repeat(40)}")
        assertTrue(v is ApiKeyValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("Bearer"))
    }

    @Test
    fun `embedded newline yields error`() {
        val v = ApiKeyValidator.validate("claude", "sk-ant-" + "a".repeat(40) + "\n")
        assertTrue(v is ApiKeyValidator.Verdict.Warn)   // trimmed first; trim catches it
    }

    @Test
    fun `embedded quote yields error`() {
        val v = ApiKeyValidator.validate("claude", "\"sk-ant-${"a".repeat(40)}\"")
        assertTrue(v is ApiKeyValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("quotes"))
    }

    @Test
    fun `key too short is error`() {
        val v = ApiKeyValidator.validate("claude", "sk-ant-abc")
        assertTrue(v is ApiKeyValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("too short"))
    }

    @Test
    fun `unknown backend yields warn, not error`() {
        val v = ApiKeyValidator.validate("future-provider", "anything-at-all")
        assertTrue(v is ApiKeyValidator.Verdict.Warn)
    }
}
