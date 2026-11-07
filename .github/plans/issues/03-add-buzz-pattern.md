---
title: "good first issue: add a new diagnostic buzz pattern"
labels: [enhancement, good first issue, diagnostics]
---
We ship 8 buzz patterns right now (`BuzzPattern.OK`, `BATTERY_LOW`, `STORAGE_FULL`, `WIFI_DOWN`, `OTA_CHECK_FAIL`, `CONTROLLER_MISSING`, `THERMAL`, `DISPLAY_FAULT`). Candidates for new ones:

- Overheating specifically (not just low-memory proxy)
- Bluetooth adapter down
- GPS/Location unavailable (if user enabled location)
- IR projector / tracking camera suspected offline
- Charger connected but current flow interrupted

### How

1. Add a constant to `BuzzPattern.Companion` in `core/ControllerHaptics.kt`:
   ```kotlin
   val MY_FAULT = BuzzPattern("-.-.-", "my fault description")
   ```
2. Add the row to `docs/diagnostics_patterns.md`.
3. Wire it up in `core/DiagnosticsEngine.kt` where the condition is detectable.
4. Open the PR.

### Rule

Code letters must be unique across patterns — users identify by feel, so collisions are bad.

### Done when

- [ ] Pattern constant exists.
- [ ] Docs row added.
- [ ] Wired to a real detection.
- [ ] Manually verified on controllers.
