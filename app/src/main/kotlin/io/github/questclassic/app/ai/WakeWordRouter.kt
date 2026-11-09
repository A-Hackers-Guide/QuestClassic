package io.github.questclassic.app.ai

/**
 * Parses "Hey {name}, {request}"-style utterances and routes them to the
 * right AI backend, so the user can run several providers in parallel —
 * "Hey Nova, …" to Gemini while "Hey Sage, …" goes to OpenAI — without
 * having to open settings and flip a toggle.
 *
 * Rules:
 *  - Case-insensitive match on the wake name (so "hey NOVA," works).
 *  - Leading filler words ("hey", "ok", "hi") are optional.
 *  - A trailing comma / colon / "please" after the name is consumed.
 *  - If no name matches, returns null (caller decides whether to fall
 *    back to the active backend or ignore the utterance).
 *
 * The router is a pure function — it has no dependencies on DataStore,
 * the network, or the Context, so it's cheap to unit-test and reuse in a
 * voice-recognition pipeline or a text input box alike.
 */
class WakeWordRouter {

    data class Match(
        val backendId: String,
        val wakeName: String,
        /** The request with the wake phrase stripped. */
        val payload: String,
    )

    /**
     * @param utterance the raw transcribed user input
     * @param nameToBackendId map of wake name → backend id (e.g. "Nova" → "gemini")
     */
    fun route(utterance: String, nameToBackendId: Map<String, String>): Match? {
        if (utterance.isBlank() || nameToBackendId.isEmpty()) return null
        val trimmed = utterance.trim().removeSuffix(".").removeSuffix("?")
        // Strip one leading filler word if present.
        val afterFiller = trimmed.lowercase().let { lower ->
            LEADING_FILLERS.firstOrNull { lower.startsWith("$it ") }
                ?.let { trimmed.substring(it.length + 1).trimStart() }
                ?: trimmed
        }
        // Longest-name-first so "Classic Pro" is tried before "Classic".
        val sorted = nameToBackendId.entries.sortedByDescending { it.key.length }
        for ((name, backendId) in sorted) {
            if (name.isBlank()) continue
            val lower = afterFiller.lowercase()
            val nameLower = name.lowercase()
            if (!lower.startsWith(nameLower)) continue
            // Must be at end-of-string or followed by a word boundary (so "Novak"
            // doesn't match wake name "Nova").
            val after = afterFiller.substring(name.length)
            val boundary = after.isEmpty() || !after.first().isLetterOrDigit()
            if (!boundary) continue
            val payload = after.trimStart().trimStart(',', ':').trimStart()
                .removePrefix("please ").removePrefix("Please ")
            return Match(backendId = backendId, wakeName = name, payload = payload)
        }
        return null
    }

    companion object {
        private val LEADING_FILLERS = listOf("hey", "ok", "okay", "hi", "yo")
    }
}
