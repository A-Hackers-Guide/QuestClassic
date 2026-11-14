package io.github.questclassic.app.ai

/**
 * Picks a short title for a chat thread so a future 'past conversations'
 * list has something readable to show instead of 'Conversation #4287'.
 *
 * Takes the first user message, strips wake-word filler + markdown,
 * clips at word boundaries, and returns up to [maxChars]. If nothing
 * useful is in there (empty / whitespace / only punctuation), returns
 * a neutral fallback.
 *
 * Pure function; unit-testable.
 */
object ChatTitleGenerator {

    /** Generate a title from the first user message's text. */
    fun fromFirstMessage(text: String, maxChars: Int = 48): String {
        if (text.isBlank()) return FALLBACK
        var t = text.trim()
        // Strip common wake-word openers the user keeps in the thread
        // even after the router strips them off the send payload.
        t = t.replace(Regex("""^(hey|ok|okay|hi|yo)[,\s]+""", RegexOption.IGNORE_CASE), "")
        // Strip a single leading @Name,.
        t = t.replace(Regex("""^@?[A-Za-z][\w-]*[,\s]+"""), "")
        // Collapse whitespace.
        t = t.replace(Regex("""\s+"""), " ").trim()
        // Strip MarkdownForTts noise if it leaked in.
        t = MarkdownForTts.strip(t)
        if (t.isBlank()) return FALLBACK
        if (t.length <= maxChars) return capitalize(t)
        // Clip at a word boundary near maxChars.
        val slice = t.take(maxChars)
        val cut = slice.lastIndexOf(' ')
        val trimmed = if (cut > maxChars * 2 / 3) slice.substring(0, cut) else slice
        return capitalize(trimmed) + "…"
    }

    /** Capitalise the first letter of the first alphabetic character. */
    private fun capitalize(s: String): String {
        if (s.isEmpty()) return s
        val first = s.indexOfFirst { it.isLetter() }
        if (first < 0) return s
        return s.substring(0, first) + s[first].uppercaseChar() + s.substring(first + 1)
    }

    const val FALLBACK: String = "New conversation"
}
