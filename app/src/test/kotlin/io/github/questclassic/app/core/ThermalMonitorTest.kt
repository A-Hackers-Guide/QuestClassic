package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalMonitorTest {

    @Test
    fun `cool and light are silent`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(0))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(1))
    }

    @Test
    fun `moderate emits warm once`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.WARM, m.accept(2))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(2))
    }

    @Test
    fun `severe emits hot once`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.HOT, m.accept(3))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(3))
    }

    @Test
    fun `critical emits CRITICAL once`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.CRITICAL, m.accept(4))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(5))
    }

    @Test
    fun `cooling rearms`() {
        val m = ThermalMonitor()
        m.accept(3) // HOT
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(0))
        assertEquals(ThermalMonitor.Emit.HOT, m.accept(3))
    }

    @Test
    fun `jumping to critical doesn't re-fire warm hot`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.CRITICAL, m.accept(5))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(2))
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(3))
    }

    @Test
    fun `clamp input`() {
        val m = ThermalMonitor()
        assertEquals(ThermalMonitor.Emit.NONE, m.accept(-5))
        assertEquals(ThermalMonitor.Emit.CRITICAL, m.accept(100))
    }

    @Test
    fun `reset clears`() {
        val m = ThermalMonitor()
        m.accept(3)
        m.reset()
        assertEquals(ThermalMonitor.Emit.HOT, m.accept(3))
    }
}
