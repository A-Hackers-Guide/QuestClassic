# Build Tools panel

A programmable HUD. You pick which widgets to show from a toggle list; chosen widgets render as a stacked column of glass cards. Each widget is small and self-contained — no background work, no services, no persistent state unless the widget explicitly uses DataStore.

## Shipped widgets

| Widget | Shows | Refresh |
|---|---|---|
| Clock | Weekday + HH:MM:SS | every 500 ms while visible |
| Fingerprint | `Build.FINGERPRINT` (the full one) | static |
| Free memory | `ActivityManager.MemoryInfo.availMem/totalMem` + % | every 1.5 s |
| Network ping | Resolve + `isReachable(2 s)` for a user-supplied host | on tap |
| Quick note | Ephemeral scratch pad (per-session only) | — |
| Haptic test | Tap-to-buzz every registered `BuzzPattern` | — |

## Adding your own widget

See [contributing/getting-started.md](../contributing/getting-started.md) and [issue #8](https://github.com/A-Hackers-Guide/QuestClassic/issues/8). Short version:

1. Open `app/src/main/kotlin/io/github/questclassic/app/ui/panels/BuildToolsPanel.kt`.
2. Add a new entry to the `Widget` enum with a title + a `@Composable (AppContainer) -> Unit` render lambda.
3. Write the Composable body.
4. Done — toggle shows up automatically.

## Widget rules

- **No background services.** Use `LaunchedEffect` + `delay` scoped to the Composable; rendering pauses the tick.
- **No new manifest permissions** without a PR note.
- **No telemetry.**
- **One file per widget** if it grows past ~60 lines — move to `ui/panels/widgets/`.
- **No shared state across widgets** beyond what `AppContainer` already exposes.

## Why a flat enum instead of plugins

Widgets are small enough that a class-registry doesn't earn its complexity. An enum entry is the single source of truth: the toggle list, the render switch, the on-disk order. When we hit 20+ widgets or want user-installable third-party widgets we'll revisit; see [ADR 0003](../adr/0003-hand-wired-di.md) for the general philosophy.

## Future work

- Persist which widgets the user has on (DataStore).
- Draggable reorder.
- Widget grid instead of stack.
- A widget marketplace — [issue #8](https://github.com/A-Hackers-Guide/QuestClassic/issues/8) upgrade.

---

_Last reviewed against v0.1.0._
