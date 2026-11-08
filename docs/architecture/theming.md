# Theming & the nostalgic look

The retro-Oculus aesthetic in one page: palette, typography, components.

## Palette (`QCColors`)

| Name | Hex | Role |
|---|---|---|
| Space | `#07122A` | App background, deep |
| Deep | `#0E1E3F` | Background gradient low, panel base |
| Panel | `#13274F` | Glass panel top-color |
| Edge | `#1E3666` | Borders, inactive buttons, dividers |
| Teal | `#34E5E7` | Primary accent — VR tiles, active buttons, links |
| Amber | `#FFB347` | Secondary accent — 2D tiles, warnings, chips |
| Soft | `#D7E2FF` | Foreground text |
| Dim | `#7F91B9` | Secondary / label text |
| Warn | `#FF6E6E` | Failure state |
| OK | `#66E0A3` | Pass state |

The Space→Deep gradient is the signature background. Panels sit on a Panel→Deep sub-gradient inside a Teal 35%-alpha border. That border tint flips to Amber or Warn or OK depending on the panel's state.

## Why this palette

- **Oculus Rift / early Gear VR era shells** used deep navy with teal highlights. We're invoking that.
- **High contrast against the Quest OLED-adjacent panels** — pure black glows, pure white burns in VR; we offset.
- **Teal + amber reads as "retro-tech"** rather than "modern Quest" (which leans white + blue).
- **Warn / OK stay away from brand accents** so they don't get confused with just "active."

## Typography (`QCTypography`)

Only system fonts — no font download at runtime. `FontFamily.SansSerif` for all text, `FontFamily.Monospace` for diagnostic values.

| Style | Weight | Size | Letter spacing | Used for |
|---|---|---|---|---|
| `displayLarge` | 800 | 40 sp | 1.0 sp | Home masthead ("Shelf") |
| `displayMedium` | 700 | 28 sp | 0.5 sp | Big numeric readouts (clock widget) |
| `titleLarge` | 700 | 22 sp | 0.3 sp | Panel subtitles |
| `titleMedium` | 600 | 17 sp | — | Dialog headers |
| `bodyLarge` | 400 | 15 sp | — | Default running text |
| `bodyMedium` | 400 | 13 sp | — | Secondary running text |
| `labelLarge` | 600 | 12 sp | 0.8 sp | ALL-CAPS section labels |
| `labelMedium` | 400 (monospace) | 11 sp | — | Diagnostic values, fingerprints |

ALL-CAPS section labels + wide letter spacing are a conscious throwback — they read like an operations console, not a modern mobile UI.

## Components

Everything in `ui/nostalgic/NostalgicComponents.kt`. See [architecture/compose-ui.md § Our reusable Composables](compose-ui.md#our-reusable-composables) for the roster.

## Dark-mode-only

`QuestClassicTheme(darkTheme = …)` hardcodes dark = true. Light mode exists for Compose Preview tooling (a token light palette) but the app forces dark at runtime. Rationale: VR headsets are worn eyes-first; a bright white UI inside a dark room kills your night vision and blooms on OLED. Dark is the right default; we don't offer light.

## Icons

Material Icons Extended, outlined style only:

- `Apps`, `MonitorHeart`, `Chat`, `Build`, `CloudDownload`, `Info`, `Verified`, `Policy`, `Public`, `ViewInAr`

One style keeps the tile grid visually coherent. If a panel ever needs a filled icon, that's a conscious break — put the discussion in the PR.

## Future

- **Multiple palettes** — a "Quest Pro / modern Horizon" skin was mentioned in early feedback. Not v0.1.0.
- **High-contrast / accessibility palette** — we should build one before v1.0.
- **Font downloads** — we'd consider shipping Inter as a bundled asset if we could cut it below 100 KB. System sans-serif is good enough for now.

---

_Last reviewed against v0.1.0._
