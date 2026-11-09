# ADR 0010 — Multi-provider AI with per-backend wake names

## Status
Accepted (2026-11-09).

## Context

The AI companion originally supported one provider at a time (Claude, OpenAI, Ollama) chosen in setup. The user asked for Gemini + ChatGPT + Claude all active simultaneously, each addressable by name ("Hey Nova …" → Gemini, "Hey Sage …" → OpenAI, etc.), and configured independently.

We also want to avoid forcing a re-type of the API key when the user switches active provider in the setup form.

## Decision

- Keep the single active-provider model in `AIConfig` for the transcript UI — only one provider is "the current one" at any instant. A multi-headed chat would quickly confuse both the UI and the backing API traffic.
- **Store per-backend state alongside the active row**: `AIConfig.perBackendNames: Map<String, String>` and `AIConfig.perBackendKeys: Map<String, String>`. Switching provider in the setup form stashes the previous entry under the old id and restores the new id's saved values.
- Expose `AIConfig.wakeNameFor(backendId, fallback)` as the lookup the wake-word listener (push-to-talk today; on-device detection in a follow-up issue) will use to route phrases to the right backend.
- Default wake names: Claude=**Classic**, OpenAI=**Sage**, Gemini=**Nova**, Ollama=**Local**. User can rename each.

## Consequences

- The API-key input is per-provider — switching doesn't clear a key.
- The single-backend code paths don't change, so nothing else has to move.
- When wake-word detection lands, a single listener can route to the correct backend without a separate config class.
- Migration from older installs: `load()` tolerates missing `per_backend_*` keys and falls back to `assistantName` / `apiKey` for the active backend.

## Alternatives considered

1. **Multi-headed chat (several active providers in one transcript).** Rejected — the token-cost/latency model diverges per backend; cross-posting user turns to several APIs explodes bandwidth and bill.
2. **One registry per backend, no "active" concept.** Rejected — the panel's transcript view needs a current provider for replies; two sources of truth is worse than one plus memos.
3. **Separate namespace for wake names (keep `assistantName` only as the display label).** Rejected — the active-provider name is also the wake name, so one field serves both. Per-backend map handles the "other providers" case.
