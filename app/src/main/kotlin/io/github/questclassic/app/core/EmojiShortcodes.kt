package io.github.questclassic.app.core

/**
 * Minimal emoji shortcode expander. The AI panel text input runs
 * replies through this (and user-sent messages, optionally via a
 * UserPref) so ':smile:' / ':rocket:' / ':fire:' render as the
 * corresponding glyph instead of a colon-delimited token.
 *
 * Minimal set, not a full codepoint map — the Quest keyboard can
 * type emoji directly, this is just for the 10-or-so shortcodes
 * that routinely leak out of model outputs.
 *
 * Pure function; unit-testable.
 */
object EmojiShortcodes {

    private val TABLE: Map<String, String> = mapOf(
        "smile" to "🙂",
        "grin" to "😀",
        "thinking" to "🤔",
        "fire" to "🔥",
        "rocket" to "🚀",
        "warning" to "⚠️",
        "check" to "✅",
        "cross" to "❌",
        "100" to "💯",
        "heart" to "❤️",
        "eyes" to "👀",
        "sparkles" to "✨",
    )

    fun expand(input: String): String {
        if (input.indexOf(':') < 0) return input
        val re = Regex(""":([a-zA-Z0-9_]+):""")
        return re.replace(input) { m ->
            TABLE[m.groupValues[1].lowercase()] ?: m.value
        }
    }

    /** Known shortcodes (lowercase, without colons). */
    fun known(): Set<String> = TABLE.keys
}
