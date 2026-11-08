# Icons

We use `androidx.compose.material.icons.outlined.*` exclusively — one visual style, so the home grid reads coherently. The `material-icons-extended` dep enables everything in the Material catalog.

## Currently used

| Panel / tile | Icon |
|---|---|
| Launcher | `Apps` |
| Diagnostics | `MonitorHeart` |
| AI Companion | `Chat` |
| Build Tools | `Build` |
| Updates | `CloudDownload` |
| About / System | `Info` |
| Signature Verifier | `Verified` |
| System Internals | `Policy` |
| Meta Hosts Audit | `Public` |
| AR Home (reserved) | `ViewInAr` |

## Choosing an icon for a new panel

- Pick from `Icons.Outlined.*` — not Filled, not TwoTone, not Rounded, not Sharp. Keeps the grid consistent.
- Use something semantic — `Apps` for launching apps, `MonitorHeart` for diagnostics. Avoid metaphors that need a label to parse.
- If none fit, write a vector drawable yourself under `app/src/main/res/drawable/` — see `ic_launcher_foreground.xml` as an example — and import it via `painterResource`.

## Why one style

Consistency. The home grid's 2×N tiles form a visual rhythm; mixing Outlined + Filled would break it. If a specific panel really needs a different style (say, Filled for emphasis), use it inside the panel body — not in the tile icon.

## APK size

`material-icons-extended` adds ~250 KB. See [ADR 0006](../adr/0006-material-icons-extended.md).

---

_Last reviewed against v0.1.0._
