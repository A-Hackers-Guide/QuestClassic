# ADR 0025 — Tone profile is a directive, not a replacement for the user's prompt

## Status
Accepted (2026-11-09).

## Context

The user may want the AI to answer tersely one day and verbosely the next, or formally for a work task vs. casually for a hobby thread — without rewriting their own system prompt every time. The naive move is to swap the whole system prompt for a canned "terse" / "verbose" version. That loses the user's own instructions (persona, preferences, constraints).

## Decision

`AssistantToneProfile` holds 5 modes (`TERSE`, `CASUAL`, `FORMAL`, `VERBOSE`, `SCRIPT_KIDDIE`), each a short directive (one or two sentences). `compose(tone, userSystemPrompt)` **prepends** the directive to the user's prompt rather than replacing it. The directive is mild enough to be a layering, not an override:

```
Answer in one or two short sentences. …

<user's own system prompt>
```

The user's own prompt stays authoritative; the tone is a modifier.

When the user hasn't set a system prompt, the directive alone is used (otherwise the AI would be given nothing but the tone, which is incomplete context).

## Consequences

**Pros**
- Flipping tone doesn't nuke the user's own persona.
- 5 presets cover the common asks without the user writing prose.
- Pure function → trivial 7-test unit coverage.

**Cons**
- Two voices in the prompt (tone + user). This is intentional and models handle it well.

## Alternatives considered

1. **Replace the whole system prompt.** Rejected — loses user context.
2. **Append to the user's prompt** instead of prepend. Rejected — less visible to the model, since instructions earliest in the prompt get the most weight in most providers.
3. **Instance per user** (user writes their own tone presets). Deferred — the 5 built-in profiles cover most of the asks; custom tones can be a user-managed list later.
