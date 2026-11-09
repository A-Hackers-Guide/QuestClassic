# ADR 0013 — Emulators: detect-and-launch, not bundle

## Status
Accepted (2026-11-09).

## Context

User asked for "an all-in-one emulator app too which takes the dolphin, snes9x, nes.emu, Project64, duckstation, pcsx2, ppsspp, vita3k, and much more source code to develop one app".

Licence realities of those sources:

| Emulator | Licence |
|---|---|
| Dolphin, Project64, nes.emu, PPSSPP, Vita3k | GPL-2.0 **only** (no "or later") |
| PCSX2 | GPL-3.0 |
| Snes9x | custom non-commercial — incompatible with GPL |
| DuckStation (current) | PolyForm Strict / CC-BY-NC-ND — non-commercial, no derivatives |

GPL-2-only cannot be linked with GPL-3 in one binary (2.0 lacks the "or later" clause that would let it upgrade). Snes9x's non-commercial clause can't be mixed with GPL at all. DuckStation's current licence forbids derivatives. A single binary mixing those sources would be a licence violation in several different directions simultaneously.

## Decision

Build a **launcher**, not an emulator.

- `EmulatorRegistry` catalogues 22 targets: 14 RetroArch libretro cores (Dolphin, PCSX2, PPSSPP, DuckStation, Mupen64Plus-Next, Snes9x, Nestopia, mGBA, SameBoy, Genesis Plus GX, Beetle Saturn, Flycast, Opera) + 8 standalones (DuckStation, AetherSX2, PPSSPP, Dolphin, Vita3K, Citra, Yuzu/Suyu, Lemuroid).
- `isInstalled(ctx, target)` via `PackageManager.getPackageInfo`.
- `launchIntent(ctx, target)` via `getLaunchIntentForPackage`.
- `storeIntent(target)` via `market://details?id=…` for missing ones.
- No upstream source bundled or redistributed.

## Consequences

- Users install each emulator themselves. Not a one-click experience; honest about who ships what.
- RetroArch's libretro architecture carries most of the systems — one install, many systems. Each `.so` core keeps its licence self-contained at load time; the architecture exists specifically because the problem we're ducking is unsolvable any other way.
- When a new system or standalone appears (e.g. Suyu forks of Yuzu, future Nintendo Switch 2 emulators), we add a row to the registry — no binary rebuild of the emulators themselves.
- The `com.retroarch` package and each standalone's package go in our manifest's `<queries>` block; no `QUERY_ALL_PACKAGES` permission is required.

## Alternatives considered

1. **Bundle everything into one binary.** Rejected — licence violation (multiple directions), engineering impossible without rewriting each emulator from scratch.
2. **Clean-room reimplementation of each system.** Rejected — years of engineering for a single system; this is a weekend feature, not a career.
3. **Thin wrapper that embeds RetroArch cores but writes our own frontend.** Rejected — RetroArch already is this frontend and has far more controller/shader/save-state polish than we'd ever add.
4. **Lemuroid-only launcher.** Considered. Lemuroid is a great FOSS alternative but covers fewer modern systems than RetroArch. We route to it when installed; we don't force it.

See [EmulatorRegistry.LicenceNote](../../app/src/main/kotlin/io/github/questclassic/app/core/EmulatorRegistry.kt) inline.
