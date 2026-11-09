# ADR 0018 — First-run flow is a linear pipeline, not a menu

## Status
Accepted (2026-11-09).

## Context

New QuestClassic installs need some bootstrapping before the full shell makes sense: welcome, capability briefing (what works on stock, what needs root), AI provider configuration or skip. A naive design is a "Setup" screen with all three sections unlocked at once — pick whichever first.

Problem: the capability briefing feeds the AI setup decision (you discover that passthrough control needs root before you decide whether to add an API key). And AI setup drives whether the assistant panel is immediately useful. The order matters; a menu hides it.

## Decision

Model first-run as a **strict linear pipeline** backed by `FirstRunController.canTransition(from, to)`:

```
BLANK → WELCOMED → CAPABILITIES_REVIEWED → AI_CONFIGURED → COMPLETE
```

Each transition is forward-adjacent only. The controller refuses non-adjacent jumps and self-loops; backward moves are impossible except via `reset()` (the "Replay setup" button in About).

An `aiSkipped: Boolean` bit survives into `COMPLETE` so the AI panel can politely nudge the user later ("want to add a Claude key?") without re-entering the wizard.

## Consequences

**Pros**
- UI code is purely rendering: it reads the step, draws the matching card, calls `advance()` on tap. No branching, no "which step is next" guesswork.
- State machine is covered by 6 JUnit tests without needing Android (`canTransition` and `isDone` are pure functions on the companion object).
- Impossible to arrive at the main shell with half-configured state: `isDone` only returns true for `COMPLETE`.

**Cons**
- Can't skip ahead to AI setup for an impatient power user. Acceptable; the wizard is 3 screens and each screen is seconds. If this becomes real complaining in issues, add a "Skip to end" option that forces `COMPLETE` with `aiSkipped = true`.

## Alternatives considered

1. **Checklist panel** (all steps visible, pick any order). Rejected — the capability briefing is prerequisite reading for the AI step; a free-form order lets the user make the AI decision without context.
2. **No wizard** — ship straight to the main shell with inline hint banners. Rejected — the root-gated panels confuse new users who don't understand why some UI is grayed out; the capability briefing is the one place to explain that cleanly.
3. **Branching state machine** (e.g. "did you skip? jump to COMPLETE"). Rejected — the branching happens inside one step (CAPABILITIES_REVIEWED can advance to AI_CONFIGURED either by configuring AI or by explicitly skipping); keeping the pipeline linear and recording the branch in `aiSkipped` keeps `canTransition` trivially analyzable.
