package io.github.questclassic.app.core

/**
 * Defensive path shape check for anything that reads or writes a
 * user-supplied path (e.g. ConfigBackup export filename, cloned-voice
 * sample path). Rejects path traversal, absolute paths, and
 * platform-reserved names.
 *
 * NOT a replacement for scoping writes to app-private storage; the
 * File / DocumentFile API is the enforcement, this is the pre-check
 * that gives the UI a clearer error than "IOException: EPERM".
 *
 * Pure function; unit-testable.
 */
object PathValidator {

    sealed interface Verdict {
        val reason: String?

        object Ok : Verdict { override val reason: String? = null }
        data class Error(override val reason: String) : Verdict
    }

    fun validate(path: String): Verdict {
        val t = path.trim()
        if (t.isEmpty()) return Verdict.Error("path is empty")
        if (t.length > MAX_LENGTH) return Verdict.Error("path too long (${t.length} > $MAX_LENGTH)")
        if (t.startsWith("/")) return Verdict.Error("absolute paths not allowed (leading slash)")
        if (t.startsWith("~")) return Verdict.Error("home-expansion paths not allowed (leading ~)")

        val segments = t.split('/', '\\')
        for (seg in segments) {
            if (seg == ".." || seg == ".") {
                return Verdict.Error("relative segment '$seg' not allowed")
            }
            if (seg.isBlank()) {
                return Verdict.Error("empty path segment (double slash?)")
            }
            if (RESERVED.contains(seg.uppercase())) {
                return Verdict.Error("reserved name '$seg' (Windows)")
            }
            for (c in seg) {
                if (c in INVALID_CHARS) {
                    return Verdict.Error("invalid char '$c' in segment '$seg'")
                }
            }
        }
        return Verdict.Ok
    }

    /** Sanitize a user-supplied filename by replacing invalid chars with
     *  '_' and refusing path separators. Returns null for non-fixable
     *  inputs (empty / entirely-invalid). */
    fun sanitizeFilename(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (t == "." || t == "..") return null
        if (RESERVED.contains(t.uppercase())) return null
        val sb = StringBuilder()
        for (c in t) {
            sb.append(if (c in INVALID_CHARS || c == '/' || c == '\\') '_' else c)
        }
        return sb.toString().trim('_').ifEmpty { null }
    }

    const val MAX_LENGTH: Int = 255
    private val INVALID_CHARS: CharArray = charArrayOf(
        '\u0000', '<', '>', ':', '"', '|', '?', '*',
    )
    private val RESERVED: Set<String> = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
    )
}
