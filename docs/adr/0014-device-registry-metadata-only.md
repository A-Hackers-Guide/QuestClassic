# ADR 0014 — Device Registry is metadata-only

## Status
Accepted (2026-11-09).

## Context

Multiple features need to know which external devices the user has paired with the headset:

- Virtual Monitors — paired companion desktops
- AI Companion — on-LAN Ollama servers
- Future — smart-home bridges, game servers, media servers

Choice: one shared registry, or a per-feature registry.

If shared, where do credentials live? Each feature has different auth: companion is paired by mDNS + trust-on-first-use, Ollama uses an unauthenticated LAN URL, a smart-home bridge may use OAuth, a media server may use an API key.

## Decision

**One shared registry for metadata.** `DeviceRegistry` persists per-device: `id`, `displayName`, `kind`, `address`, `trusted`, `lastSeenEpochMs`. **Credentials never land here.**

Each feature keeps its own credential store:
- AI → `ai_config` (api_key per backend)
- Companion → `companion_pairings` (future PR: trust tokens)
- Smart home → `smarthome_tokens` (future)

7 device kinds: `COMPANION_DESKTOP`, `OLLAMA_SERVER`, `SMART_HOME_BRIDGE`, `GAME_SERVER`, `MEDIA_SERVER`, `OTHER` (+room to add more). 64-device cap; over-cap `add()` throws; `add()` with an existing id is an update (de-dup).

## Consequences

- A security review of each feature's credential layer doesn't have to sweep across unrelated features.
- Removing a device drops the metadata; the credentials on that device's feature still need a separate cleanup call (feature's responsibility to clean up when it sees a removed device).
- The UI can list "every paired thing" in one place; each feature can list only its kind by filtering `load().filter { it.kind == K }`.
- Programmable-AI code (future) can enumerate devices without the AI getting access to credentials.

## Alternatives considered

1. **Per-feature registries only.** Rejected — duplicated metadata (name, address, trust) across features; a user renaming their desktop in one feature doesn't rename it in another.
2. **Shared registry including credentials.** Rejected — mixes concerns, makes the security review table unmanageable, invites accidental credential exposure (e.g. "list all devices" API surfaces includes tokens).
3. **Credentials in Keystore, metadata in registry.** This is still what we do — Keystore for the sensitive bits, DataStore row for the names. The ADR applies only to the registry's split from credential store.
