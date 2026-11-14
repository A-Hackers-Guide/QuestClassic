package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTitleGeneratorTest {

    @Test
    fun `short text passes through capitalized`() {
        assertEquals("Explain monads", ChatTitleGenerator.fromFirstMessage("explain monads"))
    }

    @Test
    fun `wake word stripped`() {
        assertEquals("Help me pick a model", ChatTitleGenerator.fromFirstMessage("Hey Nova, help me pick a model"))
    }

    @Test
    fun `OK filler stripped`() {
        assertEquals("What time is it", ChatTitleGenerator.fromFirstMessage("OK what time is it"))
    }

    @Test
    fun `long text clipped at word boundary`() {
        val title = ChatTitleGenerator.fromFirstMessage(
            "I would really like a thorough overview of all the compile options available",
            maxChars = 30,
        )
        assertTrue("got $title", title.endsWith("…"))
        assertTrue(title.length <= 31)
        assertTrue(!title.contains("options"))
    }

    @Test
    fun `fallback on blank`() {
        assertEquals("New conversation", ChatTitleGenerator.fromFirstMessage(""))
        assertEquals("New conversation", ChatTitleGenerator.fromFirstMessage("   "))
    }

    @Test
    fun `fallback on punctuation-only`() {
        assertEquals("...", ChatTitleGenerator.fromFirstMessage("..."))
    }

    @Test
    fun `markdown bold stripped from title`() {
        assertEquals("Important bug", ChatTitleGenerator.fromFirstMessage("**important bug**"))
    }
}
