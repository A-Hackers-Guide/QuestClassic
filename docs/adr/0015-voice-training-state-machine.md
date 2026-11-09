# ADR 0015 — Voice cloning is a staged state machine

## Status
Accepted (2026-11-09).

## Context

The user asked for "training a voice, turning on/off voices, or text" for the AI companion. Voice cloning on-device is a multi-step process: capture audio samples → fit a model → write the artifacts → mark ready. Each step can fail (user cancels, bad SNR, out of disk).

Naive "add row, set `ready=true` when done" misses the intermediate states. The UI wants to show "recording 7 of 20 phrases" and "training 60%" and offer a Cancel button. The persistence layer needs to survive an app restart mid-training without corrupting state.

## Decision

Model clone training as a finite state machine over `TrainingState`:

```
QUEUED → RECORDING → TRAINING → READY
   │          │          │
   └────► FAILED ◄───────┘
            │
            └──► QUEUED (retry)
```

`VoiceRegistry.canTransition(from, to)` encodes the legal edges. Pipeline code calls it before `advanceTraining(id, to)` so invalid moves — "already READY, drop back to TRAINING from scratch" — are rejected with a clear message instead of silently corrupting the row.

- `QUEUED → RECORDING` or `FAILED` (user cancelled before start)
- `RECORDING → TRAINING` or `FAILED`
- `TRAINING → READY` or `FAILED`
- `READY → READY` (idempotent noop so repeated success callbacks are safe)
- `FAILED → QUEUED` (user retries from the start)

Built-in platform voices go directly to `READY` on `registerBuiltIn`.

## Consequences

- Panel UI can show the current phase with a known-finite set of states.
- An app crash mid-training leaves the row in `RECORDING` or `TRAINING`; next launch can decide to resume or `FAILED` the row (cancel the orphaned clone).
- Retry is explicit: the UI's "try again" button goes `FAILED → QUEUED` and the pipeline picks up from there.
- Adding states later (e.g. `QUEUED_FOR_UPLOAD` if we add a cloud-side option) extends the graph by adding allowed edges; the `canTransition` function stays the single source of truth.

## Alternatives considered

1. **Single `ready: Boolean`.** Rejected — hides every meaningful intermediate state.
2. **Per-voice progress percentage only.** Rejected — doesn't capture the discrete phases, so the UI can't say "recording vs training" without heuristics on the percentage.
3. **State machine without a `canTransition` check.** Rejected — invalid transitions would need to be caught in every call site; one shared helper is the clean place.
