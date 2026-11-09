# ADR 0021 — Virtual monitor count is capped at what the SoC actually ships

## Status
Accepted (2026-11-09).

## Context

The user's original brief asked for "20–100 virtual screens depending on processing power". That upper bound doesn't match any shipping Quest-class SoC. Letting the UI offer 100 as a slider setting is lying to the user: the headset would silently drop frames, the companion desktop would starve, and the user would conclude the app is buggy when it's actually asked to do something impossible.

## Decision

`VirtualMonitorEstimator` maps `Build.HARDWARE` / `Build.BOARD` to a `HeadsetTier` and returns a **conservative** `realistic` + `hardCap` pair. The ceilings:

- `LEGACY_XR1` (Quest 1 / Go / Rift S): 1 realistic, 2 hard cap.
- `XR2_GEN_1` (Quest 2 / Quest Pro): 3 realistic, 4 hard cap.
- `XR2_GEN_2` (Quest 3 / Quest 3S): 6 realistic, 8 hard cap.
- `FUTURE`: 8 realistic, 12 hard cap.

The UI consumes `realistic` for the default slider position and `hardCap` for the slider's upper bound. Every tier carries a `humanNote` the UI shows below the slider so the user understands WHY the ceiling is what it is.

A unit test (`no tier returns the user's requested 20-100`) enforces that no future edit can raise any `hardCap` above 12. If H.264/H.265 decoder counts actually increase on a future SoC, the fix is to add a new tier, not to raise an existing one.

## Consequences

**Pros**
- UI never offers a setting the hardware can't hold.
- Unknown-SoC path defaults to `LEGACY_XR1` — under-promise, over-deliver.
- Changing the numbers requires updating the test, which is the audit.

**Cons**
- User who wants 100 monitors gets 6. That's not a bug; the alternative is silently dropping frames.

## Alternatives considered

1. **Trust the user's number** — let the slider go to 100. Rejected; ships a broken feature.
2. **Measure in real time** (probe the decoder's steady-state throughput). Rejected for v1; needs a benchmark pass the user would have to sit through at setup, and the number would vary with battery level.
3. **Per-chip hardcoded cap** instead of tiers. Rejected; the chip-string match surface would grow with every new headset release, while the tier model needs 1 line per new tier.
