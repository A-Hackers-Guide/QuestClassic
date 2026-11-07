---
title: "good first issue: add a new Build Tools widget"
labels: [enhancement, good first issue, build-tools]
---
Each HUD widget is a small `@Composable` in `BuildToolsPanel.kt`. See [`docs/widget_sdk.md`](../../docs/widget_sdk.md) for the contract. Good starter widgets:

- GPU frame time (Choreographer frame-callback graph)
- Wi-Fi RSSI + current channel
- Controller battery levels (left/right)
- Headset uptime (`SystemClock.elapsedRealtime`)
- Thermal state (`PowerManager.ThermalStatus` listener)

### Rules

- One file in `ui/panels/widgets/` if it's >60 lines.
- No new permissions in the manifest without a note in the PR.
- No background work — widgets only tick while visible (use `LaunchedEffect`).

### Done when

- [ ] Enum entry added to `Widget`.
- [ ] Composable implemented.
- [ ] `docs/widget_sdk.md` table updated.
- [ ] Tested on device.
