package io.github.questclassic.app.ai

import io.github.questclassic.app.core.TokenBucketLimiter

/**
 * Wraps an [AIBackend] with a [TokenBucketLimiter] so a jailbroken prompt
 * or a looping client can't burn through the user's quota. Promised in
 * PR #88.
 *
 * Each wrapped backend holds its OWN bucket, so Claude running out of
 * tokens doesn't throttle OpenAI. Default rates:
 *
 *  - Hosted providers: 20 capacity, 2/sec refill (= one send every
 *    500ms sustained, 10-burst on top of that).
 *  - Local Ollama:     60 capacity, 10/sec refill (localhost is cheap).
 *
 * On empty-bucket, chat() returns Result.failure(RateLimitedException)
 * so the UI can show "slow down" without crashing.
 */
class RateLimitedBackend(
    private val inner: AIBackend,
    val limiter: TokenBucketLimiter,
) : AIBackend by inner {

    override val id: String get() = inner.id
    override val displayName: String get() = inner.displayName

    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> {
        if (!limiter.tryAcquire()) {
            val wait = limiter.secondsUntilAvailable()
            return Result.failure(
                RateLimitedException(
                    "rate-limited; retry in %.1fs".format(wait),
                    retryAfterSeconds = wait,
                )
            )
        }
        return inner.chat(systemPrompt, history, apiKey, model)
    }

    companion object {
        /** Per-provider default rate bucket parameters. */
        fun defaultLimiterFor(backendId: String): TokenBucketLimiter = when (backendId.lowercase()) {
            "ollama" -> TokenBucketLimiter(capacity = 60.0, refillPerSecond = 10.0)
            else -> TokenBucketLimiter(capacity = 20.0, refillPerSecond = 2.0)
        }
    }
}

class RateLimitedException(message: String, val retryAfterSeconds: Double) : RuntimeException(message)
