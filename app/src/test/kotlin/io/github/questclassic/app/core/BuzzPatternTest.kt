package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for [BuzzPattern]. These exercise the dot/dash → timings translation
 * without requiring an Android runtime.
 */
class BuzzPatternTest {

    @Test
    fun `OK pattern starts with the initial gap then the start burst`() {
        val t = BuzzPattern.OK.toTimings()
        assertEquals(0L, t[0])        // initial gap
        assertEquals(600L, t[1])      // start burst to flag "pattern incoming"
        assertEquals(400L, t[2])      // long gap between burst and code
    }

    @Test
    fun `OK pattern renders three short pulses`() {
        val t = BuzzPattern.OK.toTimings()
        // after the start-burst + gap, three dots (80ms) separated by gaps
        val dotsAndGaps = t.drop(3)
        val dotCount = dotsAndGaps.count { it == 80L }
        assertEquals("OK is '...' — three dots", 3, dotCount)
    }

    @Test
    fun `BATTERY_LOW is dash-dot`() {
        val t = BuzzPattern.BATTERY_LOW.toTimings()
        val codes = t.drop(3)
        val dashCount = codes.count { it == 240L }
        val dotCount = codes.count { it == 80L }
        assertEquals("one dash", 1, dashCount)
        assertEquals("one dot", 1, dotCount)
    }

    @Test
    fun `WIFI_DOWN is dash-dot-dash-dot`() {
        val t = BuzzPattern.WIFI_DOWN.toTimings()
        val codes = t.drop(3)
        val dashCount = codes.count { it == 240L }
        val dotCount = codes.count { it == 80L }
        assertEquals(2, dashCount)
        assertEquals(2, dotCount)
    }

    @Test
    fun `every documented pattern survives toTimings()`() {
        val all = listOf(
            BuzzPattern.OK, BuzzPattern.BATTERY_LOW, BuzzPattern.STORAGE_FULL,
            BuzzPattern.WIFI_DOWN, BuzzPattern.OTA_CHECK_FAIL, BuzzPattern.CONTROLLER_MISSING,
            BuzzPattern.THERMAL, BuzzPattern.DISPLAY_FAULT,
        )
        for (p in all) {
            val t = p.toTimings()
            assertNotNull("${p.label} produces timings", t)
            assertTrue("${p.label} is nonempty", t.isNotEmpty())
            assertEquals("${p.label}: starts with initial gap", 0L, t[0])
        }
    }

    @Test
    fun `patterns have unique labels`() {
        val labels = listOf(
            BuzzPattern.OK, BuzzPattern.BATTERY_LOW, BuzzPattern.STORAGE_FULL,
            BuzzPattern.WIFI_DOWN, BuzzPattern.OTA_CHECK_FAIL, BuzzPattern.CONTROLLER_MISSING,
            BuzzPattern.THERMAL, BuzzPattern.DISPLAY_FAULT,
        ).map { it.label }
        assertEquals("no duplicates", labels.size, labels.toSet().size)
    }

    @Test
    fun `patterns have unique dot-dash codes`() {
        val codes = listOf(
            BuzzPattern.OK, BuzzPattern.BATTERY_LOW, BuzzPattern.STORAGE_FULL,
            BuzzPattern.WIFI_DOWN, BuzzPattern.OTA_CHECK_FAIL, BuzzPattern.CONTROLLER_MISSING,
            BuzzPattern.THERMAL, BuzzPattern.DISPLAY_FAULT,
        ).map { it.codeLetters }
        assertEquals("no duplicate buzz codes", codes.size, codes.toSet().size)
    }
}
