package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownForTtsTest {

    @Test
    fun `bold stripped`() {
        assertEquals("important", MarkdownForTts.strip("**important**"))
    }

    @Test
    fun `italic stripped`() {
        assertEquals("italic", MarkdownForTts.strip("*italic*"))
        assertEquals("italic", MarkdownForTts.strip("_italic_"))
    }

    @Test
    fun `inline code preserved as content`() {
        assertEquals("the foo function", MarkdownForTts.strip("the `foo` function"))
    }

    @Test
    fun `code fence replaced with placeholder`() {
        val out = MarkdownForTts.strip("here is code:\n```kotlin\nfun x() {}\n```\nthat's it")
        assertTrue(out.contains("[code block]"))
        assertFalse(out.contains("fun x()"))
    }

    @Test
    fun `heading stripped`() {
        assertEquals("Title\nBody", MarkdownForTts.strip("# Title\nBody"))
    }

    @Test
    fun `link text preserved`() {
        assertEquals("click here", MarkdownForTts.strip("[click here](https://example.com)"))
    }

    @Test
    fun `bullet marker removed`() {
        val out = MarkdownForTts.strip("- one\n- two\n- three")
        assertEquals("one\ntwo\nthree", out)
    }

    @Test
    fun `numbered list marker removed`() {
        val out = MarkdownForTts.strip("1. one\n2. two")
        assertEquals("one\ntwo", out)
    }

    @Test
    fun `intra-word underscores preserved`() {
        // 'snake_case_name' should NOT have the underscores stripped —
        // the italic regex's word-boundary lookaround prevents that.
        val out = MarkdownForTts.strip("variable snake_case_name")
        assertTrue("got $out", out.contains("snake_case_name"))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", MarkdownForTts.strip(""))
    }

    @Test
    fun `plain text passes through unchanged`() {
        val s = "nothing special about this sentence."
        assertEquals(s, MarkdownForTts.strip(s))
    }

    @Test
    fun `triple newlines collapsed`() {
        val out = MarkdownForTts.strip("a\n\n\n\nb")
        assertEquals("a\n\nb", out)
    }
}
