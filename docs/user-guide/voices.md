# Voices

The Voices panel is the registry of TTS voices available to the AI companion.

## Two kinds of voice

**Built-in voices** come from the device's TTS engine (Android's `TextToSpeech`). You don't manage these directly in QuestClassic — they come from whatever TTS engines are installed on the headset (Google TTS, Samsung TTS, etc.). The panel enumerates them and lets you toggle individual ones on or off so the AI only picks from the ones you like.

**Cloned voices** are voices you train in-app from a short sample of your (or a willing volunteer's) speech. A clone goes through a staged training pipeline:

```
QUEUED → RECORDING → TRAINING → READY
   ↓          ↓          ↓
   └────── FAILED ──────┘
              ↓
         QUEUED (retry)
```

Each step persists so a training job survives an app restart.

## Clone-training capacity

- **32-voice cap** on the clones list. Hit it and the panel refuses new entries until you delete a slot — the panel surfaces this as a message, not a silent failure.
- Each clone's training artifacts live in app-private storage. They **do not** travel in the config backup JSON (training data is device-specific and often multi-megabyte).

## Addressing the voice from the AI

Each AI backend has a "preferred voice" setting. On every assistant response, the backend's preferred voice is played if it exists and is enabled; otherwise the first enabled voice is used as a fallback. Set it in the backend's settings card, not here.

## Related decisions

- [ADR 0015 — Voice training as a state machine](../adr/0015-voice-training-state-machine.md) — why invalid transitions are rejected rather than silently repaired.
- [ADR 0019 — Agent tool safety tiers](../adr/0019-agent-tool-safety-tiers.md) — the `voices.list` tool the AI can call (READ_ONLY) to tell you what's available.
