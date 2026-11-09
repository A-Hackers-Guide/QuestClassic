# ADR 0028 — ClipboardPolicy is the single choke point for every copy

## Status
Accepted (2026-11-09).

## Context

Android's clipboard is cross-app. Any app that listens for `CLIP_CHANGED` sees what we wrote. Every "Copy to clipboard" button in the app is a potential leak if the copied text contains an API key, a JWT, or an `Authorization` header — the schema-level exclusions in [ConfigBackup](0017-config-backup-excludes-keys.md) and [DiagnosticsBundle](../core/DiagnosticsBundle.kt) protect the exports, but there's no equivalent for a random "Copy" button.

## Decision

Introduce `ClipboardPolicy.decide(sensitivity, raw)` as the single choke point every copy button must flow through. Three sensitivity tiers:

- `ORDINARY` — plain text; `SecretRedactor` applied as a safety belt.
- `DEBUG` — developer-facing debug text; `SecretRedactor` applied.
- `SECRET` — content known to carry credentials by construction. Refused outright; the UI should toast ("refusing to place credentials on clipboard") and do nothing.

Empty strings are refused (no legitimate reason to copy an empty string, and toasting "copied!" on nothing is confusing).

The Android `ClipboardManager` call lives in a thin wrapper around this policy; the policy itself is pure for unit-testability.

## Consequences

**Pros**
- Policy lives in ONE place; a new copy button only needs to pick a sensitivity tier.
- SECRET refusal is a compile-time contract: the caller can't bypass by passing a secret as ORDINARY (well, they can, but then the redactor still runs, and the developer intent is at least visible in the call site).
- 5 JUnit tests over policy ensure the SECRET refusal, the empty refusal, and that ORDINARY with an embedded key gets redacted.

**Cons**
- One more thing for a new contributor to know about. Mitigated by making the import impossible to miss — the `ClipboardPolicy` module lives next to `SecretRedactor` and there's no plain `ClipboardManager` wrapper in `core/`.

## Alternatives considered

1. **Redact in every call site.** Rejected — inevitable drift; one missed site is a leak.
2. **Redact only, never refuse.** Rejected — SECRET-marked content (API keys, JWTs the user pasted) should never reach the system clipboard even in redacted form, because the UI might show a false "copied!" state.
3. **Don't offer copy on fields that carry secrets.** Also considered; we *do* this for the apiKey input in Settings (no copy button; use the system's long-press to edit). The policy covers the belt-and-suspenders path for the surfaces where copy is useful.
