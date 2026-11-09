# ADR 0027 — Slash commands are an enum registry, not a service bus

## Status
Accepted (2026-11-09).

## Context

The AI panel's text input is the user's fastest surface to change app state ("switch backend", "rename assistant", "clear history"). We could:

1. Treat every input as a model prompt, let the model infer intent, and call tools.
2. Parse known slash prefixes out before the model sees them.

Option 1 works for complex requests but is slow and expensive for "clear history" (one HTTP round-trip per intent). Option 2 is pattern recognition: if the user typed `/clear`, we don't need a 2000-token model call to understand that.

## Decision

`SlashCommands` is a pure parser:

- `parse(input)` returns `Parsed(Command, args, raw)` when input starts with `/` and the keyword is in the enum; `null` otherwise (panel passes the input to the model as usual).
- Commands are an **enum**, each with a `keyword` and a `help` string. `helpLines()` enumerates them for the `/help` response automatically — adding a command updates `/help` for free.
- Case-insensitive keyword match; leading whitespace tolerated.
- The panel binds each command to a side-effect (clear history, switch backend, …) at the call site. The parser doesn't do the work; it tells the panel what the user asked for.

## Consequences

**Pros**
- Fast (zero HTTP) for the common intents.
- Help text is a derived property of the enum: no drift.
- 10 JUnit tests cover: plain text null, lone slash null, unknown command null, known commands recognised, multi-arg capture, case insensitivity, whitespace tolerance, helpLines completeness, aliases.

**Cons**
- Users who don't know the slash syntax won't discover it until they type `/help`. Mitigation: the panel can show "type /help for commands" as the empty-input placeholder.

## Alternatives considered

1. **Pure model-inferred intents.** Rejected — slow and costs money for cheap requests.
2. **Freeform command parser (verb + arg, no slash prefix).** Rejected — ambiguous with plain prompts ("clear my thoughts on …" is a prompt, not a `/clear`).
3. **Regex-based parser instead of enum.** Rejected — enum is more discoverable in the codebase and gives `/help` for free.
