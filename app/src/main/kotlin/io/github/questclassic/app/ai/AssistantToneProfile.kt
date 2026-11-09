package io.github.questclassic.app.ai

/**
 * User-selectable "tone" for the AI companion. Lets the user pick how
 * the model should talk — short/long, formal/casual, terse/explanatory
 * — without having to write their own system prompt by hand.
 *
 * Each profile prepends a short directive to the user's own
 * [AIConfig.systemPrompt]. The directive is deliberately mild (one or
 * two sentences): the user's own prompt stays authoritative, the tone
 * is a modifier, not a replacement.
 *
 * Pure function; unit-testable without the network.
 */
enum class AssistantToneProfile(val displayName: String, val directive: String) {

    TERSE(
        displayName = "Terse",
        directive =
            "Answer in one or two short sentences. Skip preamble and sign-offs. " +
            "If a list fits, use at most four bullets.",
    ),

    CASUAL(
        displayName = "Casual",
        directive =
            "Speak casually, as if to a friend. Short sentences are fine. " +
            "Don't open with 'Certainly!' or 'Of course!'; just answer.",
    ),

    FORMAL(
        displayName = "Formal",
        directive =
            "Answer in complete sentences with a neutral professional register. " +
            "Define any jargon you use. No colloquialisms.",
    ),

    VERBOSE(
        displayName = "Verbose",
        directive =
            "Explain your reasoning before the answer. Mention assumptions " +
            "you made. Include a short example when useful.",
    ),

    SCRIPT_KIDDIE(
        displayName = "Code-first",
        directive =
            "Prefer small code snippets to prose. Answer in Kotlin / shell / Python " +
            "depending on the question. Keep comments minimal.",
    ),

    ;

    companion object {
        /** Prepend the tone directive to the user's own system prompt. */
        fun compose(tone: AssistantToneProfile, userSystemPrompt: String): String {
            val user = userSystemPrompt.trim()
            if (user.isEmpty()) return tone.directive
            return "${tone.directive}\n\n${user}"
        }

        /** Look up by name, case-insensitive; returns null for unknown. */
        fun byName(name: String): AssistantToneProfile? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
