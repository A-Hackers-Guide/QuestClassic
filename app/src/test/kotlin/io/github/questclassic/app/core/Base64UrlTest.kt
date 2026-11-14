package io.github.questclassic.app.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Base64UrlTest {

    @Test
    fun `round trip bytes`() {
        val original = byteArrayOf(0, 1, 2, -3, 127, -128)
        assertArrayEquals(original, Base64Url.decode(Base64Url.encode(original)))
    }

    @Test
    fun `round trip string`() {
        val s = "hello world ✓"
        assertEquals(s, Base64Url.decodeString(Base64Url.encode(s)))
    }

    @Test
    fun `no padding in output`() {
        val out = Base64Url.encode(byteArrayOf(1, 2, 3))
        assertTrue("got $out", !out.endsWith("="))
    }

    @Test
    fun `decode accepts padded standard input`() {
        val std = java.util.Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), Base64Url.decode(std))
    }

    @Test
    fun `invalid chars rejected`() {
        var threw = false
        try { Base64Url.decode("abc!!") } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `empty decodes to empty`() {
        assertEquals(0, Base64Url.decode("").size)
    }

    @Test
    fun `leading whitespace trimmed`() {
        val s = Base64Url.encode("x")
        assertEquals("x", Base64Url.decodeString("   $s   "))
    }
}
