# ADR 0023 — Per-backend rate limit is a token bucket, not a counter

## Status
Accepted (2026-11-09).

## Context

Each AI backend call costs money or CPU. A naive bug (an app-side retry loop, a callback that fires more than once) or a jailbroken prompt that convinces the model to recurse can burn through the user's quota before a human notices. We need a cheap, in-memory ceiling around `AIBackend.chat(...)`.

Options:
- **Fixed-window counter**: "N calls per minute". Simple; edge-case burst behaviour (99 calls in the 59th second then 99 more in the next 1st second is 198 inside 2 seconds).
- **Leaky bucket (water drains)**: smoother than a counter.
- **Token bucket (tokens refill)**: identical steady-state; easier to express a burst cap separately from the sustained rate.

## Decision

`TokenBucketLimiter(capacity, refillPerSecond)` — pure Kotlin, in-memory, no coroutine / daemon / timer.

- Capacity is the burst cap.
- Refill rate is the sustained rate.
- `tryAcquire(cost)` returns false when empty; caller decides whether to block, back-off, or error.
- `secondsUntilAvailable(cost)` gives the UI a specific "retry in 1.4s" to show.
- Clock is injectable (`now: () -> Long`) so tests are deterministic without `Thread.sleep`.
- `drain()` for "abuse detected" and `reset()` for "user purchased more quota" are escape hatches.

Thread-safe via `@Synchronized` on the (read, update) pair.

## Consequences

**Pros**
- Burst-plus-sustained semantics in one data structure.
- Zero background threads / timers — refill is computed on demand.
- Deterministic tests via injected clock (11 of them: refill-after-time, cap clamp, cost > 1, drain, reset, invalid capacity rejection, …).

**Cons**
- Per-backend policy: the actual `capacity` / `refillPerSecond` numbers for Claude vs. OpenAI vs. Gemini vs. Ollama have to be chosen per provider. That's the right place for the knob to be; the limiter itself is generic.

## Alternatives considered

1. **Fixed-window counter.** Rejected — burst behaviour across window edges.
2. **Rate limiter from a library** (okhttp-rate-limit, Guava RateLimiter). Rejected — Guava is heavy, and the OkHttp rate-limit interceptors couple to request rate not cost, which doesn't fit "this is a 10-token prompt vs. a 400-token prompt" differential pricing.
3. **No limiter; trust the user.** Rejected — a jailbroken prompt is enough to run up a bill without the user's intent.
