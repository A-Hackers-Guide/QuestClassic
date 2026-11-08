# Buzz-pattern catalog

Every controller buzz pattern the app can fire, what it means, and the exact timing that drives it.

## Reading a pattern you felt

Every pattern starts with one **long burst (~600 ms)** — that's the "pattern incoming" marker. After that, count what follows:

- Short pulse (~80 ms) = **dot (.)**
- Long pulse (~240 ms) = **dash (-)**
- Short gap inside a letter (~120 ms)
- Longer gap between letters (~400 ms)

Match the dots/dashes against this table:

| Code | Pattern | Meaning | Where it fires |
|---|---|---|---|
| `...` | · · · | Self-test passed | `BuzzPattern.OK` (after all Diagnostics tests pass) |
| `-.` | — · | Battery below 10 % | `DiagnosticsEngine.battery` |
| `-..` | — · · | Storage below 5 % free | `DiagnosticsEngine.storage` |
| `-.-.` | — · — · | No validated network | `DiagnosticsEngine.network` |
| `-.-` | — · — | OTA check failed | `UpdatePanel` (reserved; wires up with a planned refactor) |
| `-..-` | — · · — | Controller tracking lost | Reserved — needs vendor HAL access we don't currently have |
| `--` | — — | Thermal throttling / low memory | `DiagnosticsEngine.performance` |
| `...-` | · · · — | Display fault | Reserved |

## Timing constants

```kotlin
val dot        = 80L   // ms
val dash       = 240L
val gap        = 120L   // inside a letter
val longGap    = 400L   // between letters
val startBurst = 600L   // the "pattern incoming" marker
```

Changing these should be deliberate — users calibrate to them. If you tune one, update this table and run the Build Tools → Haptic test widget to feel every pattern.

## Why these patterns specifically

- **Short codes** are short to read.
- **Distinguishable** at first touch — `-.` (BATTERY_LOW) vs `-..` (STORAGE_FULL) is hard to mix up because the second one is noticeably longer.
- **OK is `...` ** — three dots is universally "signal received" / "OK" in Morse-adjacent usage.

## Adding a new pattern

See [issue #7](https://github.com/A-Hackers-Guide/QuestClassic/issues/7). Three steps:

1. Add a `BuzzPattern(...)` constant to `ControllerHaptics.Companion` in `app/src/main/kotlin/io/github/questclassic/app/core/ControllerHaptics.kt`.
2. Make sure the code-letters are unique (collisions are bad — users identify by feel).
3. Add a row to the table above.
4. Wire it up in the panel/engine that detects the condition.

## Why controllers and not the headset

On Quest 2:

- The headset has a motor in the strap (`VrDeviceManagerServer` can access it) but the vendor HAL is signature-gated.
- The controllers have motors that respond to the standard Android `Vibrator` API.

So controllers are what a sideloaded app can reach. That's a limitation we chose to work with rather than around; the pattern model works equally well either way.

---

_Last reviewed against v0.1.0._
