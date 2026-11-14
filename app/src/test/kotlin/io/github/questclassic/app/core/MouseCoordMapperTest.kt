package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MouseCoordMapperTest {

    @Test
    fun `centre maps to centre`() {
        val m = MouseCoordMapper.map(100f, 100f, 200, 200, 1920, 1080)
        assertEquals(960, m.remoteX)
        assertEquals(540, m.remoteY)
    }

    @Test
    fun `top left maps to zero`() {
        val m = MouseCoordMapper.map(0f, 0f, 200, 200, 1920, 1080)
        assertEquals(0, m.remoteX)
        assertEquals(0, m.remoteY)
    }

    @Test
    fun `bottom right clamps inside`() {
        val m = MouseCoordMapper.map(200f, 200f, 200, 200, 1920, 1080)
        assertEquals(1919, m.remoteX)   // clamped to width - 1
        assertEquals(1079, m.remoteY)
    }

    @Test
    fun `out-of-range input clamps`() {
        val m = MouseCoordMapper.map(-10f, -10f, 200, 200, 1920, 1080)
        assertEquals(0, m.remoteX)
        assertEquals(0, m.remoteY)
        val m2 = MouseCoordMapper.map(500f, 500f, 200, 200, 1920, 1080)
        assertEquals(1919, m2.remoteX)
        assertEquals(1079, m2.remoteY)
    }

    @Test
    fun `different aspect ratios`() {
        // Quad is 300x100; remote is 1600x900. Centre of quad → 800, 450.
        val m = MouseCoordMapper.map(150f, 50f, 300, 100, 1600, 900)
        assertEquals(800, m.remoteX)
        assertEquals(450, m.remoteY)
    }

    @Test
    fun `invalid dims throw`() {
        var threw = false
        try { MouseCoordMapper.map(1f, 1f, 0, 1, 1, 1) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
