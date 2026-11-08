# AR Semicircle Hand Menu

Option 7 from the design brief: the 90° hand menu. This panel is the flat preview + 2D fallback for the full AR version.

## What you see in AR (when the full version lands)

- Raise your left hand (or controller). A 90° arc fans out from your palm pointing away from your body.
- 8 slots evenly spaced along the arc, at 15 cm radius.
- Tap a slot with your right index finger (or controller) to open it.
- Flick your palm away to dismiss.

## Geometry

See [ADR 0008 — AR semicircle geometry](../adr/0008-ar-semicircle-geometry.md) for the full math.

- Pivot at palm-center (midpoint of thumb-MCP and pinky-MCP).
- Arc plane normal to the palm-to-head vector, so the arc always faces the user as the hand rotates.
- Selection: other-hand index-fingertip enters a 2.5 cm collider sphere.

## What the flat preview shows

- An amber ring = the hand pivot.
- Teal dots = the 8 slot positions along the arc.
- Labels under the canvas list each slot's binding.

## 2D fallback

On a non-Quest Android device (Cuttlefish, emulator, a phone sideload), the flat preview IS the panel — no AR needed. The same interaction (tap a labeled slot) works.

## What's not done yet

- Passthrough + hand-tracking driver wiring — see [issue #9](https://github.com/A-Hackers-Guide/QuestClassic/issues/9).
- Custom slot assignment (drag-drop which panel each slot opens).
- Per-user arc radius + sweep tuning.

## Related

- [ADR 0008 — AR semicircle geometry](../adr/0008-ar-semicircle-geometry.md)
- [Build Tools panel](build-tools.md) — compose your own HUD
