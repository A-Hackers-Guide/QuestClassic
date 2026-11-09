# ADR 0016 — Wake-word routing is a pure function, not a service

## Status
Accepted (2026-11-09).

## Context

The user asked for "Hey, {Name}" wake-up names per AI backend so Claude / Gemini / OpenAI / Ollama can run in parallel, each addressable by name. We need to turn a transcribed utterance into "which backend answers this, and what's the payload?".

The first draft wanted this as a long-lived `WakeWordService` wrapping `AIConfig` lookups, DataStore reads, and side-effects (dispatch, buzz). Felt wrong: dispatch and buzz don't care about parsing; parsing shouldn't care about DataStore.

## Decision

Wake-word logic splits into two **pure functions**:

- `WakeWordRouter.route(utterance, nameToBackendId)` — the parser. Case-insensitive, word-boundary aware, handles optional leading filler ("Hey"/"OK"/"Hi"), strips trailing commas/colons/"please". No Context, no DataStore, no network.
- `WakeWordDispatcher.dispatch(utterance, config, backends)` — thin adapter that builds the name map (user's per-backend names + each backend's built-in display name) and resolves the result to an `AIBackend` instance, with a fallback-to-active branch when no wake phrase matched.

Side-effects — actually chatting with the backend, flipping the active indicator in the UI, playing the `AI_WAKE` buzz pattern — stay in the AIAssistantPanel / voice-input call site, so they can be swapped (CLI, test harness, panel) without touching the parser.

## Consequences

**Pros**
- Unit testable without Android, DataStore, or a network — 17 JUnit tests cover filler words, case, boundaries, prefix collisions, empty/blank input, missing backends, fallback.
- Dispatcher composes trivially with any transcription source (SpeechRecognizer, test text box, external daemon).
- No lifecycle: nothing to scope, nothing to tear down.

**Cons**
- Caller is responsible for actually invoking the backend's `chat()` method. That's one line (`dispatch.backend.chat(...)`), and the dispatcher's `wakeMatched` / `wakeName` fields make it trivial to log or display the routing decision.

## Alternatives considered

1. **`WakeWordService` singleton**. Rejected — more lifecycle than the problem needs. `AIConfig` already carries the per-backend names; the router can read them off a snapshot without owning state.
2. **Parsing in the recognizer onResult callback**. Rejected — couples parsing to a specific speech API and makes testing need Android fakes.
3. **Regex-only matching**. Rejected — users will set names like "Classic Pro" where a naive regex misses the longer prefix. Explicit longest-first iteration is five lines and testable.
