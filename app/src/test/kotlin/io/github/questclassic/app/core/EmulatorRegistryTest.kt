package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pure-JVM tests for the emulator registry — no Android calls, just the
 * static catalog.
 */
class EmulatorRegistryTest {

    @Test
    fun `every target has unique id`() {
        val ids = EmulatorRegistry.ALL.map { it.id }
        assertEquals("no duplicate ids", ids.size, ids.toSet().size)
    }

    @Test
    fun `every target has a non-empty display name system and package`() {
        for (t in EmulatorRegistry.ALL) {
            assertTrue("${t.id} displayName", t.displayName.isNotBlank())
            assertTrue("${t.id} system", t.system.isNotBlank())
            assertTrue("${t.id} packageName looks like one",
                t.packageName.contains('.'))
            assertNotNull("${t.id} homepage", t.homepage)
            assertTrue("${t.id} homepage is https", t.homepage.startsWith("https://"))
        }
    }

    @Test
    fun `RetroArch entries point at com_retroarch and carry a core`() {
        val retroArchOnes = EmulatorRegistry.ALL.filter { it.id.startsWith("retroarch.") }
        assertTrue("at least a dozen cores", retroArchOnes.size >= 12)
        for (t in retroArchOnes) {
            assertEquals("retroarch entries all share the pkg",
                "com.retroarch", t.packageName)
            assertNotNull("${t.id} has a core", t.preferredCore)
            assertTrue("${t.id} core looks like a libretro SO",
                t.preferredCore!!.endsWith(".so"))
        }
    }

    @Test
    fun `standalone entries use different packages and no shared core`() {
        val standalones = EmulatorRegistry.ALL.filter { it.id.startsWith("standalone.") }
        val packages = standalones.map { it.packageName }
        assertEquals("unique packages per standalone",
            packages.size, packages.toSet().size)
        assertFalse("standalone never uses the retroarch package",
            standalones.any { it.packageName == "com.retroarch" })
    }

    @Test
    fun `at least one target per major system`() {
        val systems = EmulatorRegistry.ALL.map { it.system.lowercase() }
        val expected = listOf("gamecube", "wii", "ps2", "psp", "ps1", "n64",
            "snes", "nes", "gba", "genesis", "saturn", "dreamcast")
        for (sys in expected) {
            assertTrue("coverage for $sys", systems.any { sys in it })
        }
    }
}
