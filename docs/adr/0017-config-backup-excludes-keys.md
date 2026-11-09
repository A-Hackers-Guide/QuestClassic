# ADR 0017 — Config backup is NOT a credential backup

## Status
Accepted (2026-11-09).

## Context

`ConfigBackupController` (added for easier device migration) exports and imports user settings as a single JSON blob: AI provider config, device registry, voice registry, passthrough toggles, Build Tools widget layout.

Tempting to dump *everything* into the blob — including API keys and cloned-voice training artifacts — so a restore is one tap. But blob files travel: USB sticks, email attachments, cloud drives, shared chats. Anything we put in the blob is now in whatever the user puts it in.

## Decision

The backup JSON excludes, by policy:

- **API keys** (`AIConfig.apiKey`, `perBackendKeys`). After a restore, the user re-enters keys manually. One minor annoyance is worth not shipping credentials in a shareable file.
- **Cloned voice training artifacts.** These are rooted in app-private storage anyway; restoring them on another device would need the same device-specific training environment, and they're multi-megabyte binaries that don't belong in a config JSON.
- **Chat history** (`ChatHistoryStore` rows). Conversations are often sensitive and large; restoring them to another device risks leaking user data if the backup is shared. The user can keep a device alive if they want continuity.
- **PayloadVerifier state** and **update-channel caches.** Device-local operational state; no benefit in moving it.

Everything else — provider names, chosen models, system prompts, per-backend wake names, device metadata, voice *choices* (not samples), passthrough toggles, widget layout — is backed up.

`RestoreSummary.humanLine()` always says "(re-enter API keys manually)" so the policy is visible on every restore.

## Consequences

**Pros**
- Backup file is safe to share: no credentials, no personal conversations.
- Format is small (a few KB), portable, and human-auditable (user can open and verify before importing).
- `CURRENT_VERSION` guard rejects wrong versions with a clear error so a future-schema blob never silently corrupts state.

**Cons**
- One-tap device migration still needs the user to re-enter keys. Acceptable; that's a 15-second task per key.

## Alternatives considered

1. **Encrypt the blob with a user passphrase.** Deferred — proper key derivation (Argon2id / scrypt), salt handling, and UX for passphrase management are more work than a single commit should carry. Can be added later as `ConfigBackupV2` with a version bump; the current v1 format stays safe to share as-is.
2. **Export keys separately into the device's keystore.** Possible, but moving between devices is the whole point of the feature, and a keystore export is device-bound by design.
3. **Everything-including-keys blob with a "this file contains secrets" warning.** Rejected — relies on the user reading the warning before sharing. The safer default is: the file is always safe.
