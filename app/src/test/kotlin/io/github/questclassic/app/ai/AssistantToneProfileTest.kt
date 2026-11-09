package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantToneProfileTest {

    @Test
    fun `every profile has a non-blank directive`() {
        for (p in AssistantToneProfile.entries) {
            assertTrue("$p directive blank", p.directive.isNotBlank())
        }
    }

    @Test
    fun `compose prepends directive`() {
        val out = AssistantToneProfile.compose(AssistantToneProfile.TERSE, "You are a tutor.")
        assertTrue(out.startsWith(AssistantToneProfile.TERSE.directive))
        assertTrue(out.endsWith("You are a tutor."))
    }

    @Test
    fun `compose returns directive alone when user prompt empty`() {
        assertEquals(
            AssistantToneProfile.FORMAL.directive,
            AssistantToneProfile.compose(AssistantToneProfile.FORMAL, ""),
        )
        assertEquals(
            AssistantToneProfile.FORMAL.directive,
            AssistantToneProfile.compose(AssistantToneProfile.FORMAL, "   "),
        )
    }

    @Test
    fun `byName is case insensitive`() {
        assertEquals(AssistantToneProfile.CASUAL, AssistantToneProfile.byName("casual"))
        assertEquals(AssistantToneProfile.CASUAL, AssistantToneProfile.byName("CASUAL"))
        assertEquals(AssistantToneProfile.VERBOSE, AssistantToneProfile.byName("VERbose"))
    }

    @Test
    fun `byName returns null for unknown`() {
        assertNull(AssistantToneProfile.byName("totally-bogus"))
    }

    @Test
    fun `display names are distinct`() {
        val names = AssistantToneProfile.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `5 profiles`() {
        assertEquals(5, AssistantToneProfile.entries.size)
    }
}
