# Diagnostics panel

Runs six userspace self-tests and buzzes the controllers in a Morse-like code pattern if any of them fail. Designed to still convey information when the display is unreadable — useful on a headset where vision is the primary output and losing it is the specific problem you most want to diagnose.

## What it checks

| Test | What it measures | Fail threshold | Buzz pattern |
|---|---|---|---|
| **Battery** | `BatteryManager.BATTERY_PROPERTY_CAPACITY` + `BatteryManager.EXTRA_HEALTH` | <10 % OR health != GOOD | `-.` (BATTERY_LOW) |
| **Storage** | `StatFs(/data).availableBytes / totalBytes` | <5 % free | `-..` (STORAGE_FULL) |
| **RAM** | `ActivityManager.MemoryInfo.lowMemory` | lowMemory=true | *no buzz; informational* |
| **Network** | `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED` on active network | not validated | `-.-.` (WIFI_DOWN) |
| **Device** | `Build.MODEL` + `Build.PRODUCT` + SDK | n/a — informational | — |
| **Performance** | `availableProcessors`, `MemoryInfo.lowMemory` as thermal proxy | proxy fires | `--` (THERMAL) |

## How the buzz works

The `ControllerHaptics.playPattern(pattern)` call constructs a `VibrationEffect.createWaveform` with alternating on/off timings:

```
dot     = 80  ms on
dash    = 240 ms on
gap     = 120 ms off (between pulses inside one letter)
longGap = 400 ms off (between letters)
startBurst = 600 ms on, then 400 ms off   ← "pattern incoming" marker
```

Only the first failed test's pattern is played (otherwise successive buzzes pile up and become un-interpretable). If all tests pass, the OK pattern (`...`) plays.

## Reading a pattern you felt

1. Three short pulses back-to-back? Everything's fine, you're good.
2. One long burst, pause, then a pattern? That's the "pattern incoming" marker — count the dots/dashes that follow.
3. Match against [reference/buzz-patterns.md](../reference/buzz-patterns.md).

## UI controls

- **Re-run** — kicks a fresh pass.
- **Stop buzz** — cancels any in-flight vibration. Useful during the Haptic test widget.

## Why we don't talk to vendor HALs

Fine-grained hardware health (eye-tracking LEDs, modem, structured-light projector, depth sensor) lives in vendor HALs under `vendor.oculus.hardware.*` and `vendor.meta.hardware.*`. Those are SELinux-gated to signed daemons only; a sideloaded app can't bind to them. The six tests here are what's reachable from unprivileged userspace. That's a real limit, documented honestly — we don't claim to detect something we can't.

## Related

- [reference/buzz-patterns.md](../reference/buzz-patterns.md) — full table.
- [Build Tools → Haptic test](build-tools.md) — tap-to-feel every pattern on demand.
- [issue #7](https://github.com/A-Hackers-Guide/QuestClassic/issues/7) — add new patterns via a `good first issue`.

---

_Last reviewed against v0.1.0._
