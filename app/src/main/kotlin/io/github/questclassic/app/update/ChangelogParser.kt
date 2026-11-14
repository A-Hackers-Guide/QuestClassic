package io.github.questclassic.app.update

/**
 * Extracts a release-notes blurb from a Keep-a-Changelog-shaped file.
 *
 * The CHANGELOG.md in this repo follows the KaC format:
 *
 *   ## [Unreleased]
 *   ### Added — …
 *   - item
 *
 *   ## [0.2.0] - 2026-11-08
 *   ### Added
 *   - item
 *
 * This parser lets the About panel show "what's new in 0.2.0?" without
 * bundling a separate docs page, and lets the release pipeline pull a
 * GitHub-release body out of the same source of truth.
 *
 * Pure function; no I/O (callers pass the file content as a String).
 */
object ChangelogParser {

    data class Section(
        val version: String,
        val date: String?,
        /** Lines verbatim inside the section, minus the H2 header. */
        val body: String,
    )

    /** Parse the whole file into ordered sections. */
    fun parse(text: String): List<Section> {
        val lines = text.lines()
        val result = mutableListOf<Section>()
        var currentVersion: String? = null
        var currentDate: String? = null
        var buffer = StringBuilder()

        fun flush() {
            if (currentVersion != null) {
                result += Section(
                    version = currentVersion!!,
                    date = currentDate,
                    body = buffer.toString().trim('\n'),
                )
            }
            buffer = StringBuilder()
        }

        for (line in lines) {
            val m = H2.find(line)
            if (m != null) {
                flush()
                currentVersion = m.groupValues[1]
                currentDate = m.groupValues[2].ifBlank { null }
            } else if (currentVersion != null) {
                buffer.appendLine(line)
            }
        }
        flush()
        return result
    }

    /** Pick a single named section. Case-insensitive; returns null when
     *  nothing matches. "Unreleased" works as a lookup. */
    fun section(text: String, version: String): Section? =
        parse(text).firstOrNull { it.version.equals(version, ignoreCase = true) }

    /** The latest *shipped* version — i.e. the first section whose
     *  version isn't "Unreleased". Returns null for an empty log. */
    fun latestShipped(text: String): Section? =
        parse(text).firstOrNull { !it.version.equals("Unreleased", ignoreCase = true) }

    // "## [0.2.0] - 2026-11-08"    captures 0.2.0 + 2026-11-08
    // "## [Unreleased]"            captures Unreleased + ""
    private val H2 = Regex("""^##\s*\[([^]]+)](?:\s*-\s*(\d{4}-\d{2}-\d{2}))?\s*$""")
}
