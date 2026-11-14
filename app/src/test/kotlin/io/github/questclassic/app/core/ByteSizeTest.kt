package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ByteSizeTest {

    @Test
    fun `small bytes stay as bytes`() {
        assertEquals("512 B", ByteSize.format(512))
        assertEquals("0 B", ByteSize.format(0))
    }

    @Test
    fun `kib`() {
        assertEquals("1.0 KiB", ByteSize.format(1024))
        assertEquals("1.5 KiB", ByteSize.format(1536))
    }

    @Test
    fun `mib gib`() {
        assertTrue(ByteSize.format(1024L * 1024).contains("MiB"))
        assertTrue(ByteSize.format(1024L * 1024 * 1024).contains("GiB"))
    }

    @Test
    fun `decimals respected`() {
        assertEquals("1.50 KiB", ByteSize.format(1536, decimals = 2))
        assertEquals("2 KiB", ByteSize.format(2048, decimals = 0))
    }

    @Test
    fun `invalid decimals throws`() {
        var threw = false
        try { ByteSize.format(1024, decimals = 10) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `short format du-like`() {
        assertEquals("256B", ByteSize.formatShort(256))
        assertEquals("1.0K", ByteSize.formatShort(1024))
        assertEquals("1M", ByteSize.formatShort(1024L * 1024 * 15))
        assertEquals("1T", ByteSize.formatShort(1024L * 1024 * 1024 * 1024 * 42))
    }

    @Test
    fun `big numbers roll up`() {
        val s = ByteSize.format(1024L * 1024 * 1024 * 1024 * 42)
        assertTrue(s.contains("TiB"))
    }
}
