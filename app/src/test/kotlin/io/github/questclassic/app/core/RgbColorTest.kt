package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbColorTest {

    @Test
    fun `parse rrggbb`() {
        val c = RgbColor.parseHex("#1A2B3C")
        assertEquals(26, c.r)
        assertEquals(43, c.g)
        assertEquals(60, c.b)
        assertEquals(255, c.a)
    }

    @Test
    fun `parse aarrggbb`() {
        val c = RgbColor.parseHex("#801A2B3C")
        assertEquals(128, c.a)
    }

    @Test
    fun `parse without hash`() {
        val c = RgbColor.parseHex("1A2B3C")
        assertEquals(26, c.r)
    }

    @Test
    fun `bad length throws`() {
        var threw = false
        try { RgbColor.parseHex("#AB") } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `channel out of range throws`() {
        var threw = false
        try { RgbColor(300, 0, 0) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `toHex round trip`() {
        val raw = "#AABBCC"
        assertEquals(raw, RgbColor.parseHex(raw).toHex())
    }

    @Test
    fun `toHex emits alpha when non-255`() {
        val hex = RgbColor(255, 0, 0, a = 128).toHex()
        assertEquals("#80FF0000", hex)
    }

    @Test
    fun `luminance monotone`() {
        val black = RgbColor(0, 0, 0).luminance()
        val white = RgbColor(255, 255, 255).luminance()
        val grey = RgbColor(128, 128, 128).luminance()
        assertTrue(black < grey && grey < white)
        assertEquals(0.0, black, 0.001)
        assertEquals(1.0, white, 0.001)
    }

    @Test
    fun `lerp midpoint`() {
        val mid = RgbColor.lerp(RgbColor(0, 0, 0), RgbColor(100, 100, 100), 0.5)
        assertEquals(50, mid.r)
    }

    @Test
    fun `lerp clamps t`() {
        val over = RgbColor.lerp(RgbColor(0, 0, 0), RgbColor(100, 100, 100), 2.0)
        assertEquals(100, over.r)
    }
}
