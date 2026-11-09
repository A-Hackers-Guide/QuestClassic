package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigMigratorTest {

    private val v1_to_v2 = ConfigMigrator.Migration(
        fromVersion = 1,
        toVersion = 2,
        apply = { it + "v1→v2;" },
    )
    private val v2_to_v3 = ConfigMigrator.Migration(
        fromVersion = 2,
        toVersion = 3,
        apply = { it + "v2→v3;" },
    )

    @Test
    fun `no-op when same version`() {
        val m = ConfigMigrator(listOf(v1_to_v2))
        assertEquals("x", m.migrate("x", 2, 2))
    }

    @Test
    fun `single step`() {
        val m = ConfigMigrator(listOf(v1_to_v2))
        assertEquals("xv1→v2;", m.migrate("x", 1, 2))
    }

    @Test
    fun `chained steps`() {
        val m = ConfigMigrator(listOf(v1_to_v2, v2_to_v3))
        assertEquals("xv1→v2;v2→v3;", m.migrate("x", 1, 3))
    }

    @Test
    fun `downgrade throws`() {
        val m = ConfigMigrator(listOf(v1_to_v2))
        var threw = false
        try { m.migrate("x", 2, 1) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `missing migration throws`() {
        val m = ConfigMigrator(listOf(v1_to_v2))
        var threw = false
        try { m.migrate("x", 1, 3) } catch (_: IllegalStateException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun `non-adjacent migration construction throws`() {
        var threw = false
        try {
            ConfigMigrator.Migration(fromVersion = 1, toVersion = 3, apply = { it })
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `builtins empty today (v1 is CURRENT_VERSION)`() {
        assertEquals(0, ConfigMigrator.BUILT_IN.size)
    }
}
