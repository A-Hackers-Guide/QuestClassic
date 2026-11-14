package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `schemeless host gets http default`() {
        val v = UrlNormalizer.normalize("ollama.local:11434")
        assertTrue(v is UrlNormalizer.Verdict.Ok)
        assertEquals("http://ollama.local:11434", v.value)
    }

    @Test
    fun `case lowered on scheme and host`() {
        val v = UrlNormalizer.normalize("Https://Ollama.Local:11434")
        assertEquals("https://ollama.local:11434", v.value)
    }

    @Test
    fun `trailing slash and punctuation stripped`() {
        val v = UrlNormalizer.normalize("http://host/..")
        assertTrue(v is UrlNormalizer.Verdict.Ok)
    }

    @Test
    fun `whitespace rejected`() {
        assertTrue(UrlNormalizer.normalize("http:// host") is UrlNormalizer.Verdict.Error)
    }

    @Test
    fun `empty rejected`() {
        assertTrue(UrlNormalizer.normalize("") is UrlNormalizer.Verdict.Error)
        assertTrue(UrlNormalizer.normalize("   ") is UrlNormalizer.Verdict.Error)
    }

    @Test
    fun `unsupported scheme rejected`() {
        val v = UrlNormalizer.normalize("ftp://host")
        assertTrue(v is UrlNormalizer.Verdict.Error)
    }

    @Test
    fun `path preserved`() {
        val v = UrlNormalizer.normalize("http://host/api/tags")
        assertEquals("http://host/api/tags", v.value)
    }

    @Test
    fun `lanOnly recognises common private addresses`() {
        assertTrue(UrlNormalizer.isLanOnly("localhost"))
        assertTrue(UrlNormalizer.isLanOnly("my.local"))
        assertTrue(UrlNormalizer.isLanOnly("192.168.1.1"))
        assertTrue(UrlNormalizer.isLanOnly("10.0.0.1"))
        assertTrue(UrlNormalizer.isLanOnly("172.16.0.1"))
        assertTrue(UrlNormalizer.isLanOnly("172.31.255.255"))
        assertTrue(UrlNormalizer.isLanOnly("127.0.0.1"))
    }

    @Test
    fun `lanOnly rejects WAN`() {
        assertFalse(UrlNormalizer.isLanOnly("example.com"))
        assertFalse(UrlNormalizer.isLanOnly("8.8.8.8"))
        assertFalse(UrlNormalizer.isLanOnly("172.15.0.1"))  // just outside RFC1918
        assertFalse(UrlNormalizer.isLanOnly("172.32.0.1"))
    }
}
