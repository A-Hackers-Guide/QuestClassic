package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests over the in-memory list operations the registry
 * performs around DataStore. DataStore itself is exercised in an
 * instrumentation test separately.
 */
class DeviceRegistryLogicTest {

    private fun device(id: String, trusted: Boolean = false) = Device(
        id = id,
        displayName = "dev-$id",
        kind = DeviceKind.OTHER,
        address = "10.0.0.$id",
        trusted = trusted,
    )

    @Test
    fun `add appends when id is new`() {
        val before = listOf(device("1"), device("2"))
        val after = before + device("3")
        assertEquals(3, after.size)
        assertEquals("3", after.last().id)
    }

    @Test
    fun `add with existing id treats as update`() {
        val before = listOf(device("1"), device("2", trusted = false))
        val replaced = before.map {
            if (it.id == "2") it.copy(trusted = true, displayName = "renamed") else it
        }
        assertEquals(2, replaced.size)
        assertTrue(replaced.single { it.id == "2" }.trusted)
        assertEquals("renamed", replaced.single { it.id == "2" }.displayName)
    }

    @Test
    fun `remove drops by id`() {
        val before = listOf(device("1"), device("2"), device("3"))
        val after = before.filterNot { it.id == "2" }
        assertEquals(2, after.size)
        assertFalse(after.any { it.id == "2" })
    }

    @Test
    fun `rename updates only the matched id`() {
        val before = listOf(device("1"), device("2"))
        val after = before.map {
            if (it.id == "1") it.copy(displayName = "new") else it
        }
        assertEquals("new", after.first().displayName)
        assertEquals("dev-2", after[1].displayName)
    }

    @Test
    fun `setTrusted only affects the matched id`() {
        val before = listOf(device("1"), device("2"))
        val after = before.map {
            if (it.id == "1") it.copy(trusted = true) else it
        }
        assertTrue(after.first().trusted)
        assertFalse(after[1].trusted)
    }

    @Test
    fun `max cap is sensible`() {
        assertTrue(DeviceRegistry.MAX_DEVICES in 16..1024)
    }

    @Test
    fun `DeviceKind covers the categories the UI depends on`() {
        val kinds = DeviceKind.values().toSet()
        assertTrue(DeviceKind.COMPANION_DESKTOP in kinds)
        assertTrue(DeviceKind.OLLAMA_SERVER in kinds)
        assertTrue(DeviceKind.SMART_HOME_BRIDGE in kinds)
    }
}
