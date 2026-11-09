package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIPanelPrefsSnapshotTest {

    @Test
    fun `default has voice off and text on`() {
        val s = AIPanelPrefsSnapshot()
        assertFalse("voice off by default", s.voiceEnabled)
        assertTrue("text on by default", s.textEnabled)
    }

    @Test
    fun `both toggles can be set together`() {
        val s = AIPanelPrefsSnapshot(voiceEnabled = true, textEnabled = true)
        assertTrue(s.voiceEnabled)
        assertTrue(s.textEnabled)
    }

    @Test
    fun `audio-only mode is text off voice on`() {
        val s = AIPanelPrefsSnapshot(voiceEnabled = true, textEnabled = false)
        assertTrue(s.voiceEnabled)
        assertFalse(s.textEnabled)
    }

    @Test
    fun `copy mutates one field at a time`() {
        val base = AIPanelPrefsSnapshot()
        val voiced = base.copy(voiceEnabled = true)
        assertEquals(base.textEnabled, voiced.textEnabled)
        assertTrue(voiced.voiceEnabled)
    }
}
