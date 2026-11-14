package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentalFlagsTest {

    @Test
    fun `every flag has a non-blank key`() {
        for (f in ExperimentalFlag.entries) {
            assertTrue(f.name, f.key.isNotBlank())
        }
    }

    @Test
    fun `every flag has a non-blank summary`() {
        for (f in ExperimentalFlag.entries) {
            assertTrue(f.name, f.summary.isNotBlank())
        }
    }

    @Test
    fun `keys are unique`() {
        val keys = ExperimentalFlag.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `prefsKey mirrors the string key`() {
        for (f in ExperimentalFlag.entries) {
            assertNotNull(f.prefsKey())
            assertTrue(f.prefsKey().name == f.key)
        }
    }

    @Test
    fun `5 flags pinned`() {
        // Pin the current count. When adding a flag, bump this + add a
        // test case for the new flag's semantics.
        assertEquals(5, ExperimentalFlag.entries.size)
    }

    @Test
    fun `summary mentions the feature name hint`() {
        // Sanity: each summary carries enough prose to be useful in a
        // user-facing toggle list.
        for (f in ExperimentalFlag.entries) {
            assertTrue("${f.name} summary too short: '${f.summary}'", f.summary.length >= 20)
        }
    }
}
