package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathValidatorTest {

    @Test
    fun `ordinary relative path is ok`() {
        assertTrue(PathValidator.validate("subdir/file.txt") is PathValidator.Verdict.Ok)
    }

    @Test
    fun `empty is error`() {
        val v = PathValidator.validate("")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `absolute path rejected`() {
        val v = PathValidator.validate("/etc/passwd")
        assertTrue(v is PathValidator.Verdict.Error)
        assertTrue(v.reason!!.contains("absolute"))
    }

    @Test
    fun `home expansion rejected`() {
        val v = PathValidator.validate("~/.ssh/id_rsa")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `double-dot traversal rejected`() {
        val v = PathValidator.validate("subdir/../other")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `backslash traversal rejected`() {
        val v = PathValidator.validate("subdir\\..\\other")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `reserved name rejected`() {
        assertTrue(PathValidator.validate("subdir/NUL") is PathValidator.Verdict.Error)
        assertTrue(PathValidator.validate("CON") is PathValidator.Verdict.Error)
    }

    @Test
    fun `invalid char rejected`() {
        val v = PathValidator.validate("subdir/foo|bar")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `empty segment rejected`() {
        val v = PathValidator.validate("subdir//file")
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `too long rejected`() {
        val v = PathValidator.validate("a".repeat(300))
        assertTrue(v is PathValidator.Verdict.Error)
    }

    @Test
    fun `sanitizeFilename replaces invalid chars`() {
        assertEquals("foo_bar_baz", PathValidator.sanitizeFilename("foo|bar/baz"))
    }

    @Test
    fun `sanitizeFilename trims underscores`() {
        assertEquals("foo", PathValidator.sanitizeFilename("||foo||"))
    }

    @Test
    fun `sanitizeFilename rejects entirely-invalid`() {
        assertNull(PathValidator.sanitizeFilename("..."))   // dots-only, after strip → empty
        assertNull(PathValidator.sanitizeFilename(""))
        assertNull(PathValidator.sanitizeFilename("CON"))
    }
}
