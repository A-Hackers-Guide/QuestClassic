package io.github.questclassic.app.ai

/**
 * Bridges [WakeWordRouter] and [AIConfig]:
 *
 *  - If the utterance starts with "Hey {Name}, …", returns the backend
 *    whose per-backend wake name matched, plus the request with the wake
 *    phrase stripped off.
 *  - Otherwise returns the currently-active backend (from [AIConfig.backendId])
 *    with the full utterance as the payload, so a non-wake utterance still
 *    reaches an assistant.
 *
 * This is where the user's "run Claude + Gemini + OpenAI side by side,
 * addressable by name" ask becomes a concrete dispatcher: a voice input
 * or a chat box calls [dispatch] and gets back (backend, payload).
 *
 * Pure function, so unit-testable without DataStore or Context.
 */
class WakeWordDispatcher(
    private val router: WakeWordRouter = WakeWordRouter(),
) {
    data class Dispatch(
        val backend: AIBackend,
        val payload: String,
        /** True when the user said a wake name; false means we fell back. */
        val wakeMatched: Boolean,
        /** The matched wake name, or null when we fell back. */
        val wakeName: String?,
    )

    /**
     * @param utterance raw transcribed input
     * @param config current AI config (its per-backend names feed the router,
     *   its active backend feeds the fallback branch)
     * @param backends the registry — each candidate id must resolve to one
     */
    fun dispatch(
        utterance: String,
        config: AIConfig,
        backends: List<AIBackend>,
    ): Dispatch {
        val nameToId = buildMap {
            config.perBackendNames.forEach { (id, name) ->
                if (name.isNotBlank()) put(name.trim(), id)
            }
            // Also let the user address a backend by its default display name
            // (e.g. "Hey Claude, …") even if no custom name is set.
            backends.forEach { b ->
                // Don't overwrite a user-chosen name that happens to equal a
                // built-in display name.
                val key = b.displayName.trim()
                if (key.isNotBlank() && !containsKey(key)) put(key, b.id)
            }
        }

        router.route(utterance, nameToId)?.let { match ->
            val target = backends.firstOrNull { it.id == match.backendId }
            if (target != null) {
                return Dispatch(
                    backend = target,
                    payload = match.payload,
                    wakeMatched = true,
                    wakeName = match.wakeName,
                )
            }
        }
        // Fallback: active backend consumes the whole utterance.
        val active = backends.firstOrNull { it.id == config.backendId }
            ?: backends.first()
        return Dispatch(
            backend = active,
            payload = utterance.trim(),
            wakeMatched = false,
            wakeName = null,
        )
    }
}
