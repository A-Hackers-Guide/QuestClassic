package io.github.questclassic.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogParserTest {

    private val sample = """
        # Changelog

        ## [Unreleased]

        ### Added
        - new widget

        ## [0.2.0] - 2026-11-08

        ### Added
        - RootDetector

        ### Fixed
        - compile bug

        ## [0.1.0] - 2026-11-07

        Initial release.
    """.trimIndent()

    @Test
    fun `parse returns ordered sections`() {
        val sections = ChangelogParser.parse(sample)
        assertEquals(3, sections.size)
        assertEquals("Unreleased", sections[0].version)
        assertEquals("0.2.0", sections[1].version)
        assertEquals("0.1.0", sections[2].version)
    }

    @Test
    fun `dates captured for release sections`() {
        val sections = ChangelogParser.parse(sample)
        assertEquals("2026-11-08", sections[1].date)
        assertEquals("2026-11-07", sections[2].date)
    }

    @Test
    fun `unreleased has null date`() {
        val sections = ChangelogParser.parse(sample)
        assertNull(sections[0].date)
    }

    @Test
    fun `body contains the section lines`() {
        val sections = ChangelogParser.parse(sample)
        assertTrue(sections[1].body.contains("### Added"))
        assertTrue(sections[1].body.contains("RootDetector"))
        assertTrue(sections[1].body.contains("### Fixed"))
    }

    @Test
    fun `section lookup is case insensitive`() {
        assertNotNull(ChangelogParser.section(sample, "0.2.0"))
        assertNotNull(ChangelogParser.section(sample, "unreleased"))
        assertNotNull(ChangelogParser.section(sample, "UNRELEASED"))
    }

    @Test
    fun `section returns null for unknown version`() {
        assertNull(ChangelogParser.section(sample, "99.9.9"))
    }

    @Test
    fun `latestShipped skips Unreleased`() {
        val s = ChangelogParser.latestShipped(sample)
        assertNotNull(s)
        assertEquals("0.2.0", s!!.version)
    }

    @Test
    fun `latestShipped is null when only Unreleased exists`() {
        val onlyUnreleased = "## [Unreleased]\n\n- a\n"
        val s = ChangelogParser.latestShipped(onlyUnreleased)
        assertNull(s)
    }

    @Test
    fun `empty input yields empty list`() {
        assertEquals(0, ChangelogParser.parse("").size)
    }

    @Test
    fun `plain text with no headers yields empty list`() {
        assertEquals(0, ChangelogParser.parse("just some lines\nno headers").size)
    }
}
