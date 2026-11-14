package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptTemplatesTest {

    @Test
    fun `ids are unique`() {
        val ids = ScriptTemplates.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `titles non blank`() {
        for (t in ScriptTemplates.ALL) {
            assertTrue(t.id, t.title.isNotBlank())
        }
    }

    @Test
    fun `bodies non blank`() {
        for (t in ScriptTemplates.ALL) {
            assertTrue(t.id, t.body.isNotBlank())
        }
    }

    @Test
    fun `byCategory returns matching entries`() {
        val stripMeta = ScriptTemplates.byCategory(ScriptCategory.STRIP_META)
        assertTrue(stripMeta.isNotEmpty())
        assertTrue(stripMeta.all { it.category == ScriptCategory.STRIP_META })
    }

    @Test
    fun `findById round trip`() {
        val sample = ScriptTemplates.ALL.first()
        assertNotNull(ScriptTemplates.findById(sample.id))
    }

    @Test
    fun `findById unknown returns null`() {
        assertNull(ScriptTemplates.findById("totally.made.up"))
    }

    @Test
    fun `all firmware entries are root-gated`() {
        // Firmware category must be root-only; a bug here would surface
        // adb-visible commands as un-gated in the Dev Super-Mode panel.
        val firmware = ScriptTemplates.byCategory(ScriptCategory.FIRMWARE)
        assertTrue(firmware.all { it.requiresRoot })
    }

    @Test
    fun `size matches ALL`() {
        assertEquals(ScriptTemplates.ALL.size, ScriptTemplates.size())
    }

    @Test
    fun `non-firmware strip-meta entries are not root-gated`() {
        // Strip Meta commands are pm uninstall --user 0, which works
        // without root on debuggable builds. Catch an accidental
        // requiresRoot = true that would push the tile out of the
        // non-root path.
        val stripMeta = ScriptTemplates.byCategory(ScriptCategory.STRIP_META)
        assertTrue(stripMeta.all { !it.requiresRoot })
    }
}
