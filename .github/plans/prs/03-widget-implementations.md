## What

Implements the four widgets that landed as stubs in the initial import:

- **Free memory** — poll `ActivityManager.MemoryInfo` every 1.5s; show MiB free + total + %.
- **Network ping** — user-supplied host; `InetAddress.getByName` + `isReachable(2s)`; show IP + latency or unreachable reason.
- **Quick note** — ephemeral per-session scratch pad (DataStore persistence can come in a follow-up).
- **Haptic test** — tap-to-buzz for every registered `BuzzPattern` so you can match what the Diagnostics panel will do when a test fails.

## Why

Removes six `ⓘ widget — stub` placeholders in `BuildToolsPanel`. Every widget is now real. Haptic test in particular is the one we expect users to leave on while debugging — it validates the controller-vibration path independently of diagnostics firing.

## How

Everything is a small private `@Composable` in `BuildToolsPanel.kt`. The `Widget` enum's `render` lambda signature changed from `() -> Unit` to `(AppContainer) -> Unit` so widgets can reach the DI container (needed for HapticTest → `ControllerHaptics`).

## Test plan

- Build + sideload.
- Build Tools → turn each widget on. Verify:
  - Free memory shows a plausible number and updates.
  - Network ping with `1.1.1.1` succeeds; with a bogus host, shows an error.
  - Quick note accepts and retains input while the HUD is open.
  - Haptic test buzzes the controllers.

## Follow-up ideas (new issues, not this PR)

- DataStore persistence for Quick note.
- Per-widget preferences (which hosts the network widget remembers).
- A "GPU frame time" widget using `Choreographer.FrameCallback` to show headset refresh-rate adherence.
