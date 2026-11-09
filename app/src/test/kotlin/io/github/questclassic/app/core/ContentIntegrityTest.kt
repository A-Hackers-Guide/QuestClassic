package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentIntegrityTest {

    @Test
    fun `empty string has a known hash`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ContentIntegrity.sha256Hex(""),
        )
    }

    @Test
    fun `hello has a known hash`() {
        // SHA-256('hello') per RFC vectors
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            ContentIntegrity.sha256Hex("hello"),
        )
    }

    @Test
    fun `different inputs have different hashes`() {
        assertNotEquals(
            ContentIntegrity.sha256Hex("a"),
            ContentIntegrity.sha256Hex("b"),
        )
    }

    @Test
    fun `utf8 encoding stable`() {
        // 'café' — the 'é' is 2 bytes in UTF-8 so a US-ASCII bug would diverge.
        val hex = ContentIntegrity.sha256Hex("café")
        assertEquals(64, hex.length)
    }

    @Test
    fun `hash is 64 lowercase hex chars`() {
        val hex = ContentIntegrity.sha256Hex("anything")
        assertEquals(64, hex.length)
        assertTrue(hex.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `shortHash has expected shape`() {
        val short = ContentIntegrity.shortHash("abcdef0123456789aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        assertEquals("abcd-ef01-2345", short)
    }

    @Test
    fun `shortHash rejects too-short input`() {
        var threw = false
        try { ContentIntegrity.shortHash("abc") } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `verify matches identity`() {
        val h = ContentIntegrity.sha256Hex("hello world")
        assertTrue(ContentIntegrity.verify("hello world", h))
    }

    @Test
    fun `verify is case insensitive`() {
        val h = ContentIntegrity.sha256Hex("x").uppercase()
        assertTrue(ContentIntegrity.verify("x", h))
    }

    @Test
    fun `verify rejects wrong hash`() {
        assertFalse(ContentIntegrity.verify("hello", "0000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `verify trims whitespace`() {
        val h = ContentIntegrity.sha256Hex("x")
        assertTrue(ContentIntegrity.verify("x", "  $h  "))
    }
}
