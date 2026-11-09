package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardPolicyTest {

    @Test
    fun `secret is refused`() {
        val d = ClipboardPolicy.decide(ClipboardPolicy.Sensitivity.SECRET, "anything")
        assertTrue(d is ClipboardPolicy.Decision.Refused)
    }

    @Test
    fun `empty is refused`() {
        val d = ClipboardPolicy.decide(ClipboardPolicy.Sensitivity.ORDINARY, "")
        assertTrue(d is ClipboardPolicy.Decision.Refused)
    }

    @Test
    fun `ordinary clean text goes through unchanged`() {
        val d = ClipboardPolicy.decide(ClipboardPolicy.Sensitivity.ORDINARY, "hello world")
        assertTrue(d is ClipboardPolicy.Decision.Copy)
        val c = d as ClipboardPolicy.Decision.Copy
        assertEquals("hello world", c.text)
        assertEquals(false, c.wasRedacted)
    }

    @Test
    fun `ordinary text with embedded api key is redacted`() {
        val d = ClipboardPolicy.decide(
            ClipboardPolicy.Sensitivity.ORDINARY,
            "logs contain sk-ant-" + "a".repeat(40) + " which should not leak",
        )
        assertTrue(d is ClipboardPolicy.Decision.Copy)
        val c = d as ClipboardPolicy.Decision.Copy
        assertTrue("key leaked: ${c.text}", !c.text.contains("a".repeat(40)))
        assertEquals(true, c.wasRedacted)
    }

    @Test
    fun `debug sensitivity is also redacted`() {
        val d = ClipboardPolicy.decide(
            ClipboardPolicy.Sensitivity.DEBUG,
            "Bearer sk-" + "a".repeat(40),
        )
        assertTrue(d is ClipboardPolicy.Decision.Copy)
        val c = d as ClipboardPolicy.Decision.Copy
        assertTrue(c.wasRedacted)
    }
}
