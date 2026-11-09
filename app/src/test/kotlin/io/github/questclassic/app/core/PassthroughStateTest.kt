package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests over PassthroughState's semantics. The
 * [PassthroughController.cameraPassthroughSupported] check depends on
 * [RootDetector] which hits Android APIs; that part is covered in
 * instrumentation tests.
 */
class PassthroughStateTest {

    @Test
    fun `defaults are camera-off audio-off text-on`() {
        val s = PassthroughState()
        assertFalse(s.cameraPassthrough)
        assertFalse(s.audioPassthrough)
        assertTrue(s.textPassthrough)
    }

    @Test
    fun `audio-only mode is text off audio on camera off`() {
        val s = PassthroughState(
            cameraPassthrough = false,
            audioPassthrough = true,
            textPassthrough = false,
        )
        assertFalse(s.cameraPassthrough)
        assertTrue(s.audioPassthrough)
        assertFalse(s.textPassthrough)
    }

    @Test
    fun `copy updates one field without disturbing others`() {
        val base = PassthroughState()
        val camOn = base.copy(cameraPassthrough = true)
        assertTrue(camOn.cameraPassthrough)
        assertEquals(base.audioPassthrough, camOn.audioPassthrough)
        assertEquals(base.textPassthrough, camOn.textPassthrough)
    }
}
