# Diagnostic buzz patterns

When the Diagnostics panel detects a failure, the app vibrates the controllers in a Morse-like pattern. If the display fails, the UI crashes, or the user doesn't have their headset on, the pattern still fires — you can match it against the table below by feel.

Every pattern starts with one long burst (~600 ms) to say "a code is coming."

| Code letters | Pattern (dot=short, dash=long) | Meaning | Where it fires |
|---|---|---|---|
| `...` | · · · | Self-test passed | `BuzzPattern.OK` |
| `-.` | — · | Battery below 10 % | `DiagnosticsEngine.battery` |
| `-..` | — · · | Storage below 5 % free | `DiagnosticsEngine.storage` |
| `-.-.` | — · — · | No validated network | `DiagnosticsEngine.network` |
| `-.-` | — · — | OTA check failed | `UpdatePanel` |
| `-..-` | — · · — | Controller tracking lost | reserved (see `Controller IMU` work in `AppContainer`) |
| `--` | — — | Thermal throttling / low memory | `DiagnosticsEngine.performance` |
| `...-` | · · · — | Display fault suspected | reserved |

The pattern timings are defined in `core/ControllerHaptics.kt`:

```
dot     = 80 ms
dash    = 240 ms
gap     = 120 ms   (between pulses inside one letter)
longGap = 400 ms   (between letters)
startBurst = 600 ms  (pattern-incoming marker)
```

## Why not just show the error on-screen

Because the diagnostic that fails most spectacularly is "the display is dead." A buzz pattern is the one channel that still reaches the user when every pixel has gone dark. Audio is second-best (Quest speakers work during display failures) and we'll add TTS of these codes in a follow-up.

## Adding a new pattern

1. Add to `BuzzPattern.Companion` in `core/ControllerHaptics.kt`.
2. Add a row to the table above.
3. Hook it up wherever the diagnostic runs.
