package io.github.questclassic.app.core

/**
 * A simple token-bucket rate limiter, pure in-memory, pure Kotlin.
 *
 * Why: each AI backend costs money per call. Even on a self-hosted
 * Ollama endpoint, a jailbroken prompt that convinces the model to
 * call itself in a loop can burn CPU cycles. A token-bucket limiter
 * at the backend-wrapper layer cuts that off cheaply without needing
 * Android's WorkManager or a server.
 *
 * Buckets hold [capacity] tokens, refill at [refillPerSecond] tokens
 * per second, and each [tryAcquire] call tries to consume one. No
 * daemon thread, no timer: the current token count is computed on
 * demand from the elapsed wall clock since the last reference.
 *
 * Thread-safe; synchronized on `this` for the (read, update) pair.
 */
class TokenBucketLimiter(
    val capacity: Double,
    val refillPerSecond: Double,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init {
        require(capacity > 0.0) { "capacity must be positive: $capacity" }
        require(refillPerSecond > 0.0) { "refillPerSecond must be positive: $refillPerSecond" }
    }

    @Volatile private var tokens: Double = capacity
    @Volatile private var lastRefillMillis: Long = now()

    /** @return true if a token was consumed; false if the bucket is empty. */
    @Synchronized
    fun tryAcquire(cost: Double = 1.0): Boolean {
        refill()
        if (tokens < cost) return false
        tokens -= cost
        return true
    }

    /** Current token count (refreshes against the wall clock). */
    @Synchronized
    fun available(): Double {
        refill()
        return tokens
    }

    /**
     * Seconds until [cost] tokens will be available again. 0 if already.
     * Caller can delay for this long before a retry.
     */
    @Synchronized
    fun secondsUntilAvailable(cost: Double = 1.0): Double {
        refill()
        if (tokens >= cost) return 0.0
        return (cost - tokens) / refillPerSecond
    }

    /** Drops the bucket to zero — the "DoS detected" path. */
    @Synchronized
    fun drain() {
        tokens = 0.0
        lastRefillMillis = now()
    }

    /** Fills the bucket to capacity — e.g. 'user purchased more quota'. */
    @Synchronized
    fun reset() {
        tokens = capacity
        lastRefillMillis = now()
    }

    private fun refill() {
        val t = now()
        val elapsedSec = (t - lastRefillMillis) / 1000.0
        if (elapsedSec <= 0) return
        tokens = (tokens + elapsedSec * refillPerSecond).coerceAtMost(capacity)
        lastRefillMillis = t
    }
}
