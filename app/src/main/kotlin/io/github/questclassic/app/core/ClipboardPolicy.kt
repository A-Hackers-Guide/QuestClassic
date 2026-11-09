package io.github.questclassic.app.core

/**
 * Centralised policy for what the app may put on the system clipboard.
 *
 * Android's clipboard is a system-wide, cross-app surface. Other apps
 * listening for CLIP_CHANGED can read what we wrote. That makes the
 * clipboard a terrible place to put raw API keys, JWTs, or chat
 * history snippets — even transiently.
 *
 * This module is where every call-site that wants to copy text flows
 * through. It redacts known secrets via [SecretRedactor] and refuses
 * to copy content marked [Sensitivity.SECRET] outright (the UI should
 * show a toast and do nothing).
 *
 * Pure function; the ClipboardManager call itself is in the Android-
 * facing wrapper so this module is unit-testable without Context.
 */
object ClipboardPolicy {

    enum class Sensitivity {
        /** Plain text; redactor is applied as a safety belt but the
         *  content is not expected to carry secrets. */
        ORDINARY,

        /** Developer-facing debug text; redactor applied. */
        DEBUG,

        /** Contains API keys / tokens / credentials by construction.
         *  NEVER goes to the clipboard — call-sites that try get a
         *  [Decision.Refused] back with a clear reason. */
        SECRET,
    }

    sealed interface Decision {
        data class Copy(val text: String, val wasRedacted: Boolean) : Decision
        data class Refused(val reason: String) : Decision
    }

    fun decide(sensitivity: Sensitivity, raw: String): Decision {
        if (raw.isEmpty()) return Decision.Refused("empty string")
        return when (sensitivity) {
            Sensitivity.SECRET -> Decision.Refused(
                "content is marked SECRET; refusing to put it on the system clipboard"
            )
            Sensitivity.ORDINARY, Sensitivity.DEBUG -> {
                val scrubbed = SecretRedactor.redact(raw)
                Decision.Copy(text = scrubbed, wasRedacted = scrubbed != raw)
            }
        }
    }
}
