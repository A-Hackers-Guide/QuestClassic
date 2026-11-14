package io.github.questclassic.app.ai

/**
 * Strips Markdown syntax from an AI reply so the TTS voice doesn't
 * literally pronounce "asterisk asterisk important asterisk asterisk".
 *
 * Deliberately minimal — handles the shapes the model writes most
 * often (bold, italic, inline code, code fences, headings, bullet
 * markers, numbered lists, links). Nested structures are flattened
 * by the order of passes; anything unusual passes through as-is.
 *
 * Pure function; unit-testable.
 */
object MarkdownForTts {

    fun strip(input: String): String {
        if (input.isEmpty()) return input
        var s = input
        // Drop fenced code blocks entirely — reading code aloud is useless.
        s = s.replace(Regex("""```[\s\S]*?```"""), " [code block] ")
        // Inline code → just the content.
        s = s.replace(Regex("""`([^`]+)`""")) { it.groupValues[1] }
        // Links [text](url) → "text".
        s = s.replace(Regex("""\[([^]]+)]\([^)]+\)""")) { it.groupValues[1] }
        // Bold **x** / __x__ → x.
        s = s.replace(Regex("""\*\*([^*]+)\*\*""")) { it.groupValues[1] }
        s = s.replace(Regex("""__([^_]+)__""")) { it.groupValues[1] }
        // Italic *x* / _x_ → x. Avoid matching inside words (intra_word).
        s = s.replace(Regex("""(?<!\w)\*([^*\n]+)\*(?!\w)""")) { it.groupValues[1] }
        s = s.replace(Regex("""(?<!\w)_([^_\n]+)_(?!\w)""")) { it.groupValues[1] }
        // Heading markers (## foo → foo).
        s = s.replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
        // Bullet markers (- foo / * foo → foo).
        s = s.replace(Regex("""^\s*[-*]\s+""", RegexOption.MULTILINE), "")
        // Numbered lists (1. foo → foo).
        s = s.replace(Regex("""^\s*\d+\.\s+""", RegexOption.MULTILINE), "")
        // Collapse 3+ blank lines.
        s = s.replace(Regex("""\n{3,}"""), "\n\n")
        return s.trim()
    }
}
