package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerAxisDeadzoneTest {

    @Test
    fun `centre is dead`() {
        val r = ControllerAxisDeadzone.apply(0f, 0f)
        assertEquals(0f, r.x, 0.0001f)
        assertEquals(0f, r.y, 0.0001f)
    }

    @Test
    fun `inside inner radius is dead`() {
        val r = ControllerAxisDeadzone.apply(0.05f, 0.05f)
        assertEquals(0f, r.x, 0.0001f)
        assertEquals(0f, r.y, 0.0001f)
    }

    @Test
    fun `outside inner radius produces non-zero`() {
        val r = ControllerAxisDeadzone.apply(0.5f, 0f)
        assertTrue(r.x > 0f)
    }

    @Test
    fun `direction preserved`() {
        val r = ControllerAxisDeadzone.apply(-0.8f, 0f)
        assertTrue(r.x < 0f)
        assertEquals(0f, r.y, 0.0001f)
    }

    @Test
    fun `full stick maps near 1`() {
        val r = ControllerAxisDeadzone.apply(1f, 0f)
        assertTrue("got ${r.x}", r.x > 0.95f && r.x <= 1f)
    }

    @Test
    fun `invalid inner throws`() {
        var threw = false
        try { ControllerAxisDeadzone.apply(0.5f, 0f, innerRadius = 1.0f) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `invalid outer throws`() {
        var threw = false
        try { ControllerAxisDeadzone.apply(0.5f, 0f, innerRadius = 0.2f, outerRadius = 0.1f) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
