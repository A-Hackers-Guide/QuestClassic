package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreSummaryTest {

    @Test
    fun `failure path prints the reason`() {
        val s = RestoreSummary(ok = false, reason = "parse error")
        assertFalse(s.ok)
        val line = s.humanLine()
        assertTrue(line.contains("restore failed"))
        assertTrue(line.contains("parse error"))
    }

    @Test
    fun `success path lists what was restored`() {
        val s = RestoreSummary(
            ok = true,
            aiRestored = true,
            devicesRestored = 3,
            voicesRestored = 2,
            passthroughRestored = true,
            buildToolsRestored = 5,
        )
        val line = s.humanLine()
        assertTrue(line.startsWith("restored: "))
        assertTrue(line.contains("AI config"))
        assertTrue(line.contains("3 devices"))
        assertTrue(line.contains("2 voices"))
        assertTrue(line.contains("passthrough"))
        assertTrue(line.contains("5 widgets"))
        assertTrue(line.contains("re-enter API keys"))
    }

    @Test
    fun `partial success omits empty counts`() {
        val s = RestoreSummary(
            ok = true,
            aiRestored = true,
            devicesRestored = 0,
            voicesRestored = 0,
            passthroughRestored = false,
            buildToolsRestored = 0,
        )
        val line = s.humanLine()
        assertTrue(line.contains("AI config"))
        assertFalse(line.contains("devices"))
        assertFalse(line.contains("voices"))
        assertFalse(line.contains("passthrough"))
        assertFalse(line.contains("widgets"))
    }

    @Test
    fun `version constant is 1`() {
        assertEquals(1, ConfigBackup.CURRENT_VERSION)
    }
}
