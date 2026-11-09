# ADR 0020 — SecretRedactor is a backstop, not the primary defence

## Status
Accepted (2026-11-09).

## Context

Secrets leak through many paths: a crash report that prints the `Authorization` header, a log line that stringifies an exception carrying the request body, an "export logs" UI that dumps `logcat` straight to a file the user shares. Each new feature is a new chance to accidentally include a key in free-form text.

The schema-level exclusions in [`ConfigBackup`](../core/ConfigBackup.kt) and [`DiagnosticsBundle`](../core/DiagnosticsBundle.kt) prevent the deliberate export paths from carrying secrets. They don't catch the accidental ones.

## Decision

Add `SecretRedactor.redact(text)`: a pure, pattern-based scrubber that catches the main providers (Anthropic `sk-ant-…`, OpenAI `sk-…`, Google `AIza…`, GitHub `gh?_…`, JWTs, `Authorization: Bearer …` headers) with a format that preserves the first 4 and last 4 characters so the user can still correlate which key was redacted. Treat it as the **last** line of defence: schema-level exclusion first, redactor only for free-form strings on their way out.

## Consequences

**Pros**
- Catches leaks the schema review missed (an error message, a debug dump).
- Keeps first/last 4 chars so a user can distinguish keys when triaging their own logs.
- 10 JUnit tests cover every pattern + plain-text no-op guard.

**Cons**
- Regex-based → can't catch every possible secret format. A proprietary key with no distinct prefix won't match. This is an accepted limit; the alternative (match "looks like random bytes") false-positives wildly.
- Order matters: Anthropic `sk-ant-` must beat the generic OpenAI `sk-` rule. Encoded as a negative lookbehind + an explicit test.

## Alternatives considered

1. **Only schema-level exclusion.** Rejected — doesn't help for free-form text in logs or error messages.
2. **Replace secrets with `[REDACTED]`.** Rejected — users triaging their own logs can't tell two different redacted keys apart, which makes support harder.
3. **Entropy-based detection (Shannon entropy over sliding windows).** Rejected — too many false positives in legitimate base64 payloads, hashes, and UUIDs for a defence that already runs after the schema exclusions.
