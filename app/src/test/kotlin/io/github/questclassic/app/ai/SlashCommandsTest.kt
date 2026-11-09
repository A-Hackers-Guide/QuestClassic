package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlashCommandsTest {

    @Test
    fun `plain text returns null`() {
        assertNull(SlashCommands.parse("hello there"))
    }

    @Test
    fun `lone slash returns null`() {
        assertNull(SlashCommands.parse("/"))
        assertNull(SlashCommands.parse("/   "))
    }

    @Test
    fun `unknown command returns null`() {
        assertNull(SlashCommands.parse("/nonexistent"))
    }

    @Test
    fun `help is recognised`() {
        val p = SlashCommands.parse("/help")!!
        assertEquals(SlashCommands.Command.HELP, p.command)
        assertTrue(p.args.isEmpty())
    }

    @Test
    fun `switch parses an arg`() {
        val p = SlashCommands.parse("/switch claude")!!
        assertEquals(SlashCommands.Command.SWITCH, p.command)
        assertEquals(listOf("claude"), p.args)
    }

    @Test
    fun `rename collects multiple arg tokens`() {
        val p = SlashCommands.parse("/rename Classic Pro")!!
        assertEquals(SlashCommands.Command.RENAME, p.command)
        assertEquals(listOf("Classic", "Pro"), p.args)
    }

    @Test
    fun `case insensitive keyword`() {
        val p = SlashCommands.parse("/HELP")!!
        assertEquals(SlashCommands.Command.HELP, p.command)
    }

    @Test
    fun `leading whitespace tolerated`() {
        val p = SlashCommands.parse("   /status")!!
        assertEquals(SlashCommands.Command.STATUS, p.command)
    }

    @Test
    fun `helpLines enumerates every command`() {
        val lines = SlashCommands.helpLines()
        assertEquals(SlashCommands.Command.entries.size, lines.size)
        assertTrue(lines.any { it.startsWith("/help") })
        assertTrue(lines.any { it.startsWith("/switch") })
    }

    @Test
    fun `whoami is a shortcut for status`() {
        val p = SlashCommands.parse("/whoami")!!
        assertEquals(SlashCommands.Command.WHOAMI, p.command)
    }
}
