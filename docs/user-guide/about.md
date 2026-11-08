# About / System panel

Device identity at-a-glance: what the headset thinks it is, plus a button to open the experimental [AR Home](ar-home.md).

## What it shows

Pulled from `DeviceIntrospection` (reflection-wrapped `SystemProperties.get` + `android.os.Build`):

- Model, manufacturer, codename (`hollywood` on Quest 2)
- Android base + SDK level
- Full build fingerprint
- Build ID
- Build host (the thing with the Meta Tupperware hostname)
- HorizonOS branch (`releases-oculus-14.0-vN`)
- Security patch level (device-wide)
- `ro.oculus.reality_mode` (`mr` on Quest 2 regardless of whether MR is active)
- SoC
- Vendor partition's security patch level (often years behind — see [System Internals](system-internals.md))
- USB VID

## Vs. the System Internals panel

| Panel | Purpose |
|---|---|
| **About** | Quick glance. Good first stop. |
| **System Internals** | Everything, categorized, with the RE findings highlighted. |

If you're doing serious inspection, go to System Internals.

## The "Open AR Home" button

Launches `ARHomeActivity`, the first-cut preview of the 90° semicircular hand menu. On the current build it renders as a flat 2D preview inside the About panel's Activity, with a tick-based hover animation — not yet full passthrough / hand-tracking. Full integration roadmap at [user-guide/ar-home.md](ar-home.md) and [issue #9](https://github.com/A-Hackers-Guide/QuestClassic/issues/9).

---

_Last reviewed against v0.1.0._
