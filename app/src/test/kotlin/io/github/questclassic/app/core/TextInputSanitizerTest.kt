package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TextInputSanitizerTest {

    @Test
    fun `zero-width characters removed`() {
        val raw = "hello​world‌‍﻿ok"
        val cleaned = TextInputSanitizer.sanitize(raw)
        assertEquals("helloworldok", cleaned)
        for (c in arrayOf('​', '‌', '‍', '﻿')) {
            assertFalse(cleaned.contains(c))
        }
    }

    @Test
    fun `nbsp becomes regular space`() {
        assertEquals("a b c", TextInputSanitizer.sanitize("a b c"))
    }

    @Test
    fun `curly quotes normalised`() {
        assertEquals("he said \"hi\"", TextInputSanitizer.sanitize("he said “hi”"))
        assertEquals("that's it", TextInputSanitizer.sanitize("that’s it"))
    }

    @Test
    fun `en and em dash normalised`() {
        assertEquals("a-b", TextInputSanitizer.sanitize("a–b"))
        assertEquals("a-b", TextInputSanitizer.sanitize("a—b"))
    }

    @Test
    fun `ellipsis expanded`() {
        assertEquals("wait...done", TextInputSanitizer.sanitize("wait…done"))
    }

    @Test
    fun `crlf normalised to lf`() {
        assertEquals("a\nb\nc", TextInputSanitizer.sanitize("a\r\nb\r\nc"))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", TextInputSanitizer.sanitize(""))
    }

    @Test
    fun `ordinary text unchanged`() {
        val s = "nothing special"
        assertEquals(s, TextInputSanitizer.sanitize(s))
    }
}
