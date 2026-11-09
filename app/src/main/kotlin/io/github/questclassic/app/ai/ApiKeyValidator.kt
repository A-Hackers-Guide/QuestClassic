package io.github.questclassic.app.ai

/**
 * Cheap shape check for AI provider API keys so the user gets a clear
 * error at the settings screen instead of a 401 from the network.
 *
 * This is a FORMAT validator, not an auth validator — a well-formed
 * key may still be revoked server-side. It catches the common mistakes:
 *
 *   - pasted the whole `Authorization: Bearer <key>` header,
 *   - pasted the key with a quote or newline attached,
 *   - swapped providers (Anthropic key in the OpenAI field),
 *   - key clearly too short to be real.
 *
 * Pure function; unit-testable.
 */
object ApiKeyValidator {

    sealed interface Verdict {
        val reason: String?

        object Ok : Verdict { override val reason: String? = null }
        data class Error(override val reason: String) : Verdict
        data class Warn(override val reason: String) : Verdict
    }

    /**
     * @param backendId one of 'claude' / 'openai' / 'gemini' / 'ollama'.
     * For Ollama the field is treated as optional (local endpoints usually
     * don't require one).
     */
    fun validate(backendId: String, raw: String): Verdict {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return when (backendId.lowercase()) {
                "ollama" -> Verdict.Ok
                else -> Verdict.Error("key is required")
            }
        }

        if (trimmed != raw) {
            return Verdict.Warn(
                "key has leading/trailing whitespace — use the trimmed value: " +
                    "${trimmed.take(4)}…${trimmed.takeLast(4)}",
            )
        }

        if (trimmed.contains('\n') || trimmed.contains('\r')) {
            return Verdict.Error("key has embedded newlines; copy once more without the header")
        }
        if (trimmed.contains('"') || trimmed.contains('\'')) {
            return Verdict.Error("key has embedded quotes; copy without surrounding quotes")
        }
        if (trimmed.contains(' ')) {
            return Verdict.Error("key has embedded spaces; did you paste the whole 'Authorization: Bearer …' header?")
        }

        return when (backendId.lowercase()) {
            "claude" -> validatePrefix(trimmed, "sk-ant-", minLength = 20, provider = "Anthropic")
            "openai" -> {
                if (trimmed.startsWith("sk-ant-")) {
                    Verdict.Error("this looks like an Anthropic key (sk-ant-…); set it on the Claude backend instead")
                } else {
                    validatePrefix(trimmed, "sk-", minLength = 20, provider = "OpenAI")
                }
            }
            "gemini" -> validatePrefix(trimmed, "AIza", minLength = 39, provider = "Google")
            "ollama" -> Verdict.Ok // any string, including raw URL for an endpoint
            else -> Verdict.Warn("unknown backend $backendId; key format NOT validated")
        }
    }

    private fun validatePrefix(
        key: String,
        prefix: String,
        minLength: Int,
        provider: String,
    ): Verdict {
        if (!key.startsWith(prefix)) {
            return Verdict.Error("$provider keys start with '$prefix'; got '${key.take(8)}…'")
        }
        if (key.length < minLength) {
            return Verdict.Error("$provider key is too short (${key.length} < $minLength)")
        }
        return Verdict.Ok
    }
}
