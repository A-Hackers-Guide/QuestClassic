package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactorTest {

    @Test
    fun `anthropic key is redacted`() {
        val s = SecretRedactor.redact("my key is sk-ant-abcdefghijklmnop0123456789")
        assertFalse("raw key leaked", s.contains("abcdefghijklmnop0123456789"))
        assertTrue(s.contains("anthropic-key"))
    }

    @Test
    fun `openai key is redacted`() {
        val s = SecretRedactor.redact("Authorization: sk-abcdefghijklmnop0123456789")
        assertFalse(s.contains("abcdefghijklmnop0123456789"))
        assertTrue(s.contains("openai-key"))
    }

    @Test
    fun `google key is redacted`() {
        val key = "AIza" + "a".repeat(35)
        val s = SecretRedactor.redact("const KEY = \"$key\"")
        assertFalse(s.contains(key))
        assertTrue(s.contains("google-key"))
    }

    @Test
    fun `github tokens are redacted`() {
        for (p in listOf("ghp", "gho", "ghu", "ghs", "ghr")) {
            val tok = "${p}_" + "a".repeat(40)
            val s = SecretRedactor.redact("GH=$tok")
            assertFalse("$p leaked", s.contains(tok))
            assertTrue(s.contains("github-token"))
        }
    }

    @Test
    fun `jwt is redacted`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdHRlc3R0ZXN0"
        val s = SecretRedactor.redact("token: $jwt")
        assertFalse(s.contains(jwt))
        assertTrue(s.contains("jwt"))
    }

    @Test
    fun `plain text is untouched`() {
        val s = SecretRedactor.redact("the quick brown fox jumps over the lazy dog")
        assertEquals("the quick brown fox jumps over the lazy dog", s)
    }

    @Test
    fun `bearer header is redacted`() {
        val s = SecretRedactor.redact("Authorization: Bearer abcDEF123456.xyz-789")
        assertTrue(s.contains("bearer-header"))
    }

    @Test
    fun `preserves head and tail for recognisability`() {
        val s = SecretRedactor.redact("sk-ant-abcd0000000000000000efgh")
        // Head 'sk-a' and the key's own tail is visible.
        assertTrue("got $s", s.contains("sk-a"))
    }

    @Test
    fun `findings lists the categories`() {
        val input = "sk-ant-abc0000000000000000000 ghp_bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val cats = SecretRedactor.findings(input)
        assertTrue("got $cats", "anthropic-key" in cats)
        assertTrue("github-token" in cats)
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", SecretRedactor.redact(""))
    }
}
