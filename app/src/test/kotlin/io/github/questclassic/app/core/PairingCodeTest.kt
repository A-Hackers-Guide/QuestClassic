package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeTest {

    @Test
    fun `generate produces 11 chars with 2 dashes`() {
        val c = PairingCode.generate()
        assertEquals(11, c.length)
        assertEquals('-', c[3])
        assertEquals('-', c[7])
    }

    @Test
    fun `generate produces distinct codes`() {
        val a = PairingCode.generate()
        val b = PairingCode.generate()
        assertNotEquals(a, b)   // overwhelmingly likely
    }

    @Test
    fun `generate excludes ambiguous chars`() {
        for (i in 0 until 50) {
            val c = PairingCode.generate()
            for (ch in c) {
                if (ch == '-') continue
                assertFalse("$c contains '$ch'", ch in "0OIL1")
            }
        }
    }

    @Test
    fun `matches tolerates case + separators`() {
        val code = PairingCode.generate()
        assertTrue(PairingCode.matches(code, code.lowercase()))
        assertTrue(PairingCode.matches(code, code.replace("-", "")))
        assertTrue(PairingCode.matches(code, "  $code  "))
    }

    @Test
    fun `matches rejects different codes`() {
        val a = "ABC-DEF-234"
        val b = "ABC-DEF-235"
        assertFalse(PairingCode.matches(a, b))
    }

    @Test
    fun `matches rejects short`() {
        assertFalse(PairingCode.matches("ABC", "ABC"))
    }

    @Test
    fun `isValidShape accepts common shapes`() {
        assertTrue(PairingCode.isValidShape("ABC-DEF-234"))
        assertTrue(PairingCode.isValidShape("abcdef234"))
        assertFalse(PairingCode.isValidShape(""))
        assertFalse(PairingCode.isValidShape("ABC-DEF"))
    }
}
