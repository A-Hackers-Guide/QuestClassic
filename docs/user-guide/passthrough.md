# Passthrough controls

Three independent toggles exposed through `PassthroughController`:

| Toggle | Default | Effect |
|---|---|---|
| **cameraPassthrough** | off | Show the real world through the headset cameras |
| **audioPassthrough** | off | Route headset mic audio into the AI companion / virtual-monitor voice channel |
| **textPassthrough** | on | Display AI replies + system notifications as floating text |

## Why camera passthrough is capability-gated

Flipping the camera passthrough surface requires the `com.oculus.permission.USE_SCENE` signature permission, which is only granted to Meta's own shell / system apps. Retail Quest apps cannot request it.

`PassthroughController.cameraPassthroughSupported(rootDetector)` returns `true` only when the device is rooted (because then we can talk to the compositor directly via `setprop` / service calls). On stock, it returns `false` and `effectiveCameraOn()` always resolves to false regardless of the saved preference.

## The three states, honestly

| Device | cameraPassthrough preference | effective? |
|---|---|---|
| Retail Quest | on | **No** — compositor refuses |
| Retail Quest | off | No |
| Rooted Quest | on | **Yes** |
| Rooted Quest | off | No |

We never lie to the UI: when the user toggles camera passthrough on a stock device, the saved preference flips but the panel also shows "Requires root — see docs" and `effectiveCameraOn` stays false.

## Audio and text passthrough

These two are entirely app-side and work on stock:

- **audioPassthrough** — gates whether the mic → AI / companion track is open. Off by default (surprise mic is bad).
- **textPassthrough** — gates whether the AI panel shows the transcript at all. Off = audio-only mode, useful when you're in a game and just want voice.

## Where state lives

`DataStore (Preferences)` at:

```
/data/data/io.github.questclassic.app/datastore/passthrough.preferences_pb
```

Three keys: `camera`, `audio`, `text`. Excluded from Android's cloud backup by `data_extraction_rules.xml`.
