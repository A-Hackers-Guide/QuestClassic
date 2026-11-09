# ADR 0029 — ApiKeyValidator checks shape, not authority

## Status
Accepted (2026-11-09).

## Context

The AI panel's settings form takes an API key. Today a bad key fails late — the user saves, tries to send, gets a 401, has to go back to settings. Much of the time the error is obvious at type-time (wrong prefix for the chosen provider, embedded quotes from a paste, the whole `Authorization: Bearer …` header pasted in).

A full auth check requires a network call to the provider. That's expensive, slow, and sometimes impossible (the user is on a plane). A shape check is free and catches most of the common mistakes immediately.

## Decision

`ApiKeyValidator.validate(backendId, raw)` returns a three-value `Verdict`:

- `Ok` — shape looks right; save it.
- `Warn` — recoverable but surprising (e.g. leading whitespace); the UI shows a hint and still allows saving the trimmed form.
- `Error` — the format is clearly wrong; save is blocked until the user fixes it.

Patterns it catches:

- Anthropic key in the OpenAI field (`sk-ant-…`).
- OpenAI key without the `sk-` prefix.
- Google key without the `AIza` prefix.
- Key with embedded newlines, quotes, or spaces (pasted the whole header).
- Key too short to be real (prefix matches, rest is stub).

Ollama accepts any string (including a URL for the endpoint) because self-hosted Ollama's auth is caller-defined.

Pure function; the settings-form wire-in is one call + an inline error state.

## Consequences

**Pros**
- Catches the common paste mistakes at the settings form instead of after the first 401.
- Shape-only → no network needed → works offline.
- `Warn` tier means we don't block a save on whitespace alone (we trim and save).
- 13 JUnit tests cover every Verdict branch.

**Cons**
- A **well-formed** key may still be revoked server-side; the user discovers that on first send. Acceptable; the validator's claim is "shape looks real", not "key is live".

## Alternatives considered

1. **Call the provider to validate.** Rejected per context — expensive, slow, needs network, breaks "set up on a plane" use case.
2. **No validation — rely on the 401.** Rejected — user has to context-switch twice (type key → leave settings → try to send → get 401 → go back to settings) for what the validator catches in one step.
3. **Regex equals auth.** Not a claim; the ADR is titled "shape, not authority" for a reason.
