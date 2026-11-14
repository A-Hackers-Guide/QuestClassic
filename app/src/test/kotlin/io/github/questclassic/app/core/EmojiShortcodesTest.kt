package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiShortcodesTest {

    @Test
    fun `known shortcode expands`() {
        val out = EmojiShortcodes.expand("ready :rocket:")
        assertFalse(":rocket:" in out)
        assertTrue(out.endsWith("🚀"))
    }

    @Test
    fun `unknown shortcode passes through`() {
        assertEquals("stop :nothere:", EmojiShortcodes.expand("stop :nothere:"))
    }

    @Test
    fun `no colons returns input unchanged`() {
        assertEquals("hello world", EmojiShortcodes.expand("hello world"))
    }

    @Test
    fun `case insensitive`() {
        val out = EmojiShortcodes.expand(":SMILE:")
        assertEquals("🙂", out)
    }

    @Test
    fun `multiple in one message`() {
        val out = EmojiShortcodes.expand("ship it :rocket: :fire:")
        assertTrue(out.contains("🚀"))
        assertTrue(out.contains("🔥"))
    }

    @Test
    fun `known includes smile`() {
        assertTrue("smile" in EmojiShortcodes.known())
    }
}
