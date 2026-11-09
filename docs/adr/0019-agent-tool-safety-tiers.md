# ADR 0019 — Agent tool safety is a 5-tier allow-list, not a toggle

## Status
Accepted (2026-11-09).

## Context

The user asked for a "programmable" AI companion — one that can do more than reply with text. The natural shape is a function-call registry the model can trigger (think OpenAI tools / Anthropic tool use). But that same registry is also an attack surface: a jailbroken prompt that reaches the model can try to call anything in it. Treating the registry as "on or off" ignores that different tools carry different blast radii.

## Decision

Every registered `AgentTool` declares a `SafetyLevel` in one of **five tiers**, and the default enforcement policy is per-tier, not per-tool:

- `READ_ONLY` — pure reads of app-local state (battery percent, uptime, locale, backend list). **Default: allow.**
- `WRITE_LOCAL` — writes to app-private DataStore (change the assistant's name, toggle a widget). **Default: allow.**
- `NETWORK` — HTTPS to a user-approved endpoint (the active AI backend, the OTA server). **Default: allow**, because the user already chose the endpoint.
- `SIDE_EFFECT` — visible actions on the device (open an app, flip passthrough, launch an emulator). **Default: allow but surface a chip** so the user sees what happened right after it happens.
- `CROSS_DEVICE` — effects on hardware other than this headset (companion desktop, smart-home, another user). **Default: deny.** Each registration must be paired with an explicit per-feature opt-in in Settings.

`AgentToolSeed.seedAll` registers 8 built-in tools, all at `READ_ONLY`. NETWORK / SIDE_EFFECT / CROSS_DEVICE tools are registered feature-by-feature so the user encounters them one at a time as they enable each feature.

## Consequences

**Pros**
- Jailbroken prompt can't skip tiers: the enforcement is in the registry, not in the model prompt. Even if the model invents a tool call for a tool we never registered, `get()` returns null.
- `CROSS_DEVICE` denied-by-default reflects the user's trust boundary: this headset is theirs, other devices need explicit per-tool approval.
- UI can render tools grouped by tier so the user sees exactly what the assistant is empowered to do.

**Cons**
- A tool author has to think about which tier their action belongs in. This is a feature, not a bug — the thought is the audit.

## Alternatives considered

1. **One global "enable tools" toggle.** Rejected — gives READ_ONLY device.info the same blast radius as a cross-device payment, which no sane user would knowingly grant together.
2. **Per-tool allow-list only.** Rejected — forces the user to make 20+ individual decisions at setup time. Tier defaults cover 90% of the ergonomic surface; per-tool overrides remain possible.
3. **Capability strings matching OS permissions.** Rejected — OS permission vocabulary doesn't fit (`RECORD_AUDIO` ≠ the semantic risk of a tool that merely uses the microphone); the five tiers are custom to this app's threat model.
