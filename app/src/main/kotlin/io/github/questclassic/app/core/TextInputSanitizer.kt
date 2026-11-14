package io.github.questclassic.app.core

/**
 * Normalises user text before it reaches a persistent store or the AI
 * backend. Catches:
 *
 *   - BOM + zero-width characters that paste in from fancy editors
 *     (U+200B, U+200C, U+200D, U+FEFF) and break regex matches silently.
 *   - Fancy quotes (" " ' ') → plain ASCII equivalents, so a /rename
 *     command the user typed on an iOS keyboard is still comparable
 *     against the enum.
 *   - Non-breaking space (U+00A0) → regular space, so whitespace
 *     trimming catches it.
 *   - Mixed CR/LF endings → \n.
 *
 * NOT a replacement for Normalizer.normalize() — Unicode
 * canonicalisation is more invasive than most of our call sites want.
 * This is 'make the user's text behave sensibly' only.
 *
 * Pure function; unit-testable.
 */
object TextInputSanitizer {

    fun sanitize(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '​', '‌', '‍', '﻿' -> continue   // zero-width / BOM
                ' ' -> sb.append(' ')                            // nbsp
                '“', '”' -> sb.append('"')                   // curly double quotes
                '‘', '’' -> sb.append('\'')                  // curly single quotes
                '–', '—' -> sb.append('-')                   // en / em dash
                '…' -> sb.append("...")                           // ellipsis
                '\r' -> continue                                        // drop CR
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
