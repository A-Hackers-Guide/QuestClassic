# ADR 0011 — Chat history persisted per backend

## Status
Accepted (2026-11-09).

## Context

Users switch between Claude / OpenAI / Gemini / Ollama. If the panel keeps a single rolling transcript, switching provider mid-conversation sends the OTHER provider's assistant turns as the user's history on the next request. That's confusing (the model doesn't know why "you" sound different) and bad on cost (every provider pays for the other's context).

## Decision

Store chat history **keyed by backend id** in its own DataStore (`ai_chat_history`) with rows like `history/claude`, `history/openai`, `history/gemini`, `history/ollama`.

- One JSON-encoded `List<ChatMessage>` per backend.
- 200-message cap per backend (`ChatHistoryStore.MAX_MESSAGES`). Over-cap `save()` keeps the tail.
- Switching provider loads the target backend's history and replaces the panel's in-memory list. The transcript follows the provider.

## Consequences

- Conversations don't bleed between providers.
- The Clear button only clears the active backend's history (important: user can clean up one provider without touching the rest).
- 200 × 4 backends × average 2 KB/msg ≈ 1.6 MB of DataStore worst-case. Comfortably below per-row size caps.
- Migration: `load(backendId)` returns `emptyList()` on missing or malformed rows; nothing else to migrate.

## Alternatives considered

1. **One rolling history across providers.** Rejected — the cross-provider bleed is the whole problem we set out to avoid.
2. **Session-ID-based history** (each panel open = a new session). Rejected — the user asked explicitly for "pick back up conversation" across app restarts; sessions would start fresh each time.
3. **Encrypted history** (EncryptedSharedPreferences or SQLCipher). Deferred — DataStore-prefs on app-private storage is the Android default trust boundary. If a later PR needs encryption at rest, the storage shape can migrate without an API change.
