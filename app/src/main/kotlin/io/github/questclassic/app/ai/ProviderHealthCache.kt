package io.github.questclassic.app.ai

/**
 * Remembers each provider's last-call outcome so the AI panel can
 * dim the 'Switch to OpenAI' button while OpenAI is throwing 500s,
 * and so the Diagnostics bundle can report 'last call to Claude
 * succeeded 4s ago; OpenAI failed 30s ago (429).'
 *
 * In-memory, thread-safe via @Synchronized; TTL-based so a known-bad
 * provider gets retried eventually.
 *
 * Pure function around an injectable clock so tests are deterministic.
 */
class ProviderHealthCache(
    val ttlMillis: Long = 5 * 60_000,  // 5 minutes
    private val now: () -> Long = System::currentTimeMillis,
) {
    sealed interface Health {
        val recordedAtMs: Long
        data class Healthy(override val recordedAtMs: Long) : Health
        data class Unhealthy(override val recordedAtMs: Long, val reason: String) : Health
    }

    private val map = mutableMapOf<String, Health>()

    @Synchronized
    fun recordSuccess(backendId: String) {
        map[backendId.lowercase()] = Health.Healthy(now())
    }

    @Synchronized
    fun recordFailure(backendId: String, reason: String) {
        map[backendId.lowercase()] = Health.Unhealthy(now(), reason)
    }

    /** Current state for a backend. Returns null if never recorded or
     *  if the record is older than [ttlMillis]. */
    @Synchronized
    fun healthOf(backendId: String): Health? {
        val h = map[backendId.lowercase()] ?: return null
        if (now() - h.recordedAtMs > ttlMillis) {
            map.remove(backendId.lowercase())
            return null
        }
        return h
    }

    @Synchronized
    fun snapshot(): Map<String, Health> = map.toMap().filterValues {
        now() - it.recordedAtMs <= ttlMillis
    }

    @Synchronized
    fun clear() = map.clear()
}
