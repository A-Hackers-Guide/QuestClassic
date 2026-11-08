# Launcher panel

A retro-Oculus-style grid of every launchable app on the headset. Taps launch. Tiles are color-coded by whether an app is marked VR-launchable or a regular 2D-panel app.

## What it shows

- **VR apps** (teal border) — anything that declares `com.oculus.intent.category.VR` in its manifest. Meta's Store filters and the built-in launcher only surface these in the main Library tab.
- **2D apps** (amber border) — anything with `android.intent.category.LAUNCHER`. Settings, the Meta Quest Browser, our own app — everything that runs as a 2D panel.
- **Both tags** — some apps declare both (Horizon, Store, Settings); we show them as VR.

## How it finds apps

```
AppListRepository.load() runs on Dispatchers.IO
  → PackageManager.queryIntentActivities for both intents
  → merge, dedupe by package name, sort by lowercase label
```

VR-marked packages take precedence; apps found in only the 2D list are tagged 2D.

## Manifest permission

Requires `android.permission.QUERY_ALL_PACKAGES`. Declared in our manifest; granted automatically on install. Without it we'd only see a few apps on Android 11+.

## Known limits

- **System apps** that don't have a launcher intent (DownloadProvider, NetworkStack, etc.) don't show up — correct behavior.
- **"Horizon OS" internal apps** sometimes advertise only the VR category and sometimes neither — the launcher reflects what the headset's own launcher would.
- Icons that are adaptive layered drawables render via a Canvas fallback; occasional apps have no icon at all (we render an empty teal square for those — see `AppTile` in `LauncherPanel.kt`).

## Launching

Clicking a tile builds an `Intent` from the `ResolveInfo` and calls `startActivity` with `FLAG_ACTIVITY_NEW_TASK`. That's the same mechanism Meta's own launcher uses. If the target app refuses (rare — usually a signed-permission issue), we silently fall through; the tap just doesn't do anything.

## Future work

- Search / filter box — [issue #10 folds this in as part of usage-stats](https://github.com/A-Hackers-Guide/QuestClassic/issues/10).
- Pinned / favorites row — not tracked yet; worthy of a `good first issue`.
- Launch telemetry (most-used, last-used) — this would be [issue #10](https://github.com/A-Hackers-Guide/QuestClassic/issues/10) proper (reads `UsageStatsManager`).

---

_Last reviewed against v0.1.0._
