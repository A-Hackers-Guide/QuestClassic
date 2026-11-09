# Emulator Launcher panel

Detect and launch the emulators you already have installed. **Nothing is bundled**: the GPL-2-only / GPL-3 / Snes9x-non-commercial / DuckStation-PolyForm licences on these projects can't legally coexist in a single binary. We route to each app's own installed package instead.

## Supported systems

### Via RetroArch (single install → dozens of systems)

| System | Preferred libretro core |
|---|---|
| Nintendo GameCube | `dolphin_libretro_android.so` |
| Nintendo Wii | `dolphin_libretro_android.so` |
| Sony PlayStation 2 | `pcsx2_libretro_android.so` |
| Sony PSP | `ppsspp_libretro_android.so` |
| Sony PlayStation 1 | `duckstation_libretro_android.so` |
| Nintendo 64 | `mupen64plus_next_libretro_android.so` |
| Super Nintendo | `snes9x_libretro_android.so` |
| Nintendo NES | `nestopia_libretro_android.so` |
| Game Boy Advance | `mgba_libretro_android.so` |
| Game Boy / Game Boy Color | `sameboy_libretro_android.so` |
| Sega Genesis / Mega Drive | `genesis_plus_gx_libretro_android.so` |
| Sega Saturn | `mednafen_saturn_libretro_android.so` |
| Sega Dreamcast | `flycast_libretro_android.so` |
| 3DO | `opera_libretro_android.so` |

Install RetroArch (`com.retroarch`) from F-Droid or its website. Use the **Online Updater → Core Downloader** inside RetroArch to pull each core you want.

### Standalone emulators (per-system packages)

| System | Package |
|---|---|
| Sony PlayStation 1 | `com.github.stenzek.duckstation` |
| Sony PlayStation 2 | `xyz.aethersx2.android` (AetherSX2) |
| Sony PSP | `org.ppsspp.ppsspp` |
| Nintendo GameCube / Wii | `org.dolphinemu.dolphinemu` |
| Sony PS Vita | `org.vita3k.emulator` |
| Nintendo 3DS | `org.citra.citra_emu` (Citra MMJ) |
| Nintendo Switch | `org.suyu.suyu_emu` |
| Multi-system FOSS | `com.swordfish.lemuroid` |

Standalones typically have smoother per-system tuning than their libretro cores; try both.

## How detection works

`EmulatorRegistry.isInstalled` calls `PackageManager.getPackageInfo(pkg, 0)`. No `QUERY_ALL_PACKAGES` permission needed — each emulator's package is in our manifest's `<queries>` block so we can see it without a global grant.

## Why we don't bundle them

Four reasons, any one of which is a blocker:

1. **GPL-2-only ↔ GPL-3 incompatibility** (Dolphin / PCSX2).
2. **Snes9x's non-commercial clause** can't be linked with GPL at all.
3. **DuckStation's current PolyForm Strict / CC-BY-NC-ND** forbids derivatives.
4. Each emulator ships its own biases, controller remaps, save-state formats, and shaders. Merging them produces a worse experience than letting each one be itself.

RetroArch's libretro architecture solves this cleanly: each core is loaded as a separate `.so` at runtime, so each core's licence stays self-contained.

## ROMs

Bring your own. The launcher only *launches*; it doesn't ship ROMs or know anything about ROM paths. RetroArch and each standalone have their own content pickers.

Sources that are legally clear: your own dumps from physical media you own, homebrew, public-domain titles. The launcher doesn't check — it's up to you.
