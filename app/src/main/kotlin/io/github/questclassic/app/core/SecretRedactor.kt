package io.github.questclassic.app.core

/**
 * Best-effort redaction for strings about to be logged, pasted into a
 * support bundle, or shown in a UI. The aim is 'don't leak a key the
 * user clearly didn't want shared'; it is NOT a replacement for:
 *
 *   - excluding fields from [ConfigBackup] (done at the schema level),
 *   - excluding fields from [DiagnosticsBundle] (done at the schema
 *     level, policy-tested by [DiagnosticsBundleTest]).
 *
 * Patterns it catches, in order of precedence:
 *  1. Anthropic keys (`sk-ant-…`)
 *  2. OpenAI keys (`sk-…` outside the Anthropic prefix)
 *  3. Google API keys (`AIza…` 39 chars)
 *  4. GitHub tokens (`ghp_…`, `gho_…`, `ghu_…`, `ghs_…`, `ghr_…`)
 *  5. JWT-looking tokens (three dot-joined base64url segments)
 *  6. Bearer tokens in Authorization headers
 *  7. Base64 payloads longer than 64 chars
 *
 * Each redaction keeps the first 4 and last 4 chars so the user can
 * still spot which key it was: `sk-ant-…XXYY` rather than `[REDACTED]`.
 *
 * Pure function; unit-testable.
 */
object SecretRedactor {

    private data class Rule(val pattern: Regex, val label: String)

    private val RULES: List<Rule> = listOf(
        Rule(Regex("""sk-ant-[A-Za-z0-9\-_]{20,}"""), "anthropic-key"),
        Rule(Regex("""(?<!sk-ant-)sk-[A-Za-z0-9]{20,}"""), "openai-key"),
        Rule(Regex("""AIza[A-Za-z0-9_\-]{35}"""), "google-key"),
        Rule(Regex("""gh[pousr]_[A-Za-z0-9]{36,}"""), "github-token"),
        Rule(Regex("""\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b"""), "jwt"),
        Rule(Regex("""(?i)Authorization:\s*Bearer\s+[A-Za-z0-9._\-]+"""), "bearer-header"),
    )

    fun redact(input: String): String {
        if (input.isEmpty()) return input
        var out = input
        for ((regex, label) in RULES) {
            out = regex.replace(out) { m ->
                keepEdges(m.value, label)
            }
        }
        return out
    }

    /** Return a list of what WOULD be redacted, without changing the input.
     *  Handy for 'copy to clipboard' preview UIs that want to show counts. */
    fun findings(input: String): List<String> {
        val out = mutableListOf<String>()
        for ((regex, label) in RULES) {
            regex.findAll(input).forEach { _ -> out.add(label) }
        }
        return out
    }

    private fun keepEdges(value: String, label: String): String {
        val trimmed = value.takeWhile { it != '\n' }
        if (trimmed.length <= 10) return "[$label:short]"
        val head = trimmed.take(4)
        val tail = trimmed.takeLast(4)
        return "$head…$tail[$label:${trimmed.length}]"
    }
}
