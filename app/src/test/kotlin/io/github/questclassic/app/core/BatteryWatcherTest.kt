package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryWatcherTest {

    @Test
    fun `above 25 no notice`() {
        val w = BatteryWatcher()
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(50))
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(26))
    }

    @Test
    fun `cross 25 emits low once`() {
        val w = BatteryWatcher()
        assertEquals(BatteryWatcher.Emit.LOW, w.accept(24))
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(23))
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(20))
    }

    @Test
    fun `cross 10 emits critical once`() {
        val w = BatteryWatcher()
        assertEquals(BatteryWatcher.Emit.CRITICAL, w.accept(9))
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(5))
    }

    @Test
    fun `critical beats low at the boundary`() {
        val w = BatteryWatcher()
        // Jumping straight to <=10 fires CRITICAL (low gets marked too).
        assertEquals(BatteryWatcher.Emit.CRITICAL, w.accept(8))
        // Rising back to 15 does nothing — we're still armed.
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(15))
    }

    @Test
    fun `rising above 30 rearms`() {
        val w = BatteryWatcher()
        w.accept(24) // LOW
        assertEquals(BatteryWatcher.Emit.NONE, w.accept(35)) // rearm
        assertEquals(BatteryWatcher.Emit.LOW, w.accept(24)) // fires again
    }

    @Test
    fun `clamp input`() {
        val w = BatteryWatcher()
        assertEquals(BatteryWatcher.Emit.CRITICAL, w.accept(-5))
        val w2 = BatteryWatcher()
        assertEquals(BatteryWatcher.Emit.NONE, w2.accept(500))
    }

    @Test
    fun `reset clears armed state`() {
        val w = BatteryWatcher()
        w.accept(24)
        w.reset()
        assertEquals(BatteryWatcher.Emit.LOW, w.accept(24))
    }
}
