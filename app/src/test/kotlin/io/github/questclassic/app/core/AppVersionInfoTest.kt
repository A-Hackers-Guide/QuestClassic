package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionInfoTest {

    @Test
    fun `human with sha`() {
        val v = AppVersionInfo("0.3.1", 42, "release", "1a2b3c4d5e")
        assertEquals("0.3.1 (release) · 42 · g1a2b3c4", v.human())
    }

    @Test
    fun `human without sha`() {
        val v = AppVersionInfo("0.3.1", 42, "release", null)
        assertEquals("0.3.1 (release) · 42", v.human())
    }

    @Test
    fun `compact with sha`() {
        val v = AppVersionInfo("0.3.1", 42, "release", "1a2b3c4d5e")
        assertEquals("0.3.1+42.release.g1a2b3c4", v.compact())
    }

    @Test
    fun `compact without sha`() {
        val v = AppVersionInfo("0.3.1", 42, "debug", null)
        assertEquals("0.3.1+42.debug", v.compact())
    }

    @Test
    fun `isRelease is case insensitive`() {
        assertTrue(AppVersionInfo("1.0", 1, "RELEASE").isRelease())
        assertTrue(AppVersionInfo("1.0", 1, "release").isRelease())
        assertFalse(AppVersionInfo("1.0", 1, "debug").isRelease())
    }

    @Test
    fun `valid rejects empty version name`() {
        assertFalse(AppVersionInfo.valid(AppVersionInfo("", 1, "release")))
    }

    @Test
    fun `valid rejects negative version code`() {
        assertFalse(AppVersionInfo.valid(AppVersionInfo("1.0", -1, "release")))
    }

    @Test
    fun `valid rejects empty build type`() {
        assertFalse(AppVersionInfo.valid(AppVersionInfo("1.0", 1, "")))
    }

    @Test
    fun `sha trimmed to 7`() {
        val v = AppVersionInfo("0.1.0", 1, "release", "abcdef1234567890")
        assertTrue(v.human().contains("gabcdef1"))
        assertFalse(v.human().contains("gabcdef12"))
    }

    @Test
    fun `blank sha is dropped`() {
        val v = AppVersionInfo("0.1.0", 1, "release", "   ")
        assertFalse(v.human().contains("g"))
    }
}
