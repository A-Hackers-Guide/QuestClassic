# Changelog

All notable changes to QuestClassic are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows semver-adjacent (`MAJOR.MINOR.PATCH`).

## [Unreleased]

### Added — AI

- **Google Gemini backend** (`ai/GeminiBackend.kt`) alongside Claude / OpenAI / Ollama. `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.
- **Per-backend wake names + API keys** in `AIConfig` (`perBackendNames`, `perBackendKeys`). Switch provider in setup → the previous name + key stash under the old id, and the new id's saved values restore. Defaults: Claude = *Classic*, OpenAI = *Sage*, Gemini = *Nova*, Ollama = *Local*.
- **Chat history persisted per backend** (`ai/ChatHistoryStore.kt`). 200-message cap, one row per backend id. Switching provider restores that provider's thread; Clear clears only the active backend.
- **Panel toggles persisted** (`ai/AIPanelPrefs.kt`): voice on/off, text on/off. Text-off switches the transcript to a single-card AUDIO-ONLY mode.
- **Programmable AI — `AgentToolRegistry`** (`ai/AgentToolRegistry.kt`). Allowlisted function-call surface; 7 categories; 5 safety levels (`CROSS_DEVICE` denied by default). Features register their tools; AI panel builds the per-provider tool list at send-time.
- **Voice registry** (`ai/VoiceRegistry.kt`). Built-in + cloned voices, staged clone-training state machine (QUEUED → RECORDING → TRAINING → READY / FAILED → QUEUED retry), 32-voice cap.

### Added — device + system

- **Device registry** (`core/DeviceRegistry.kt`) — 7 kinds (COMPANION_DESKTOP, OLLAMA_SERVER, SMART_HOME_BRIDGE, GAME_SERVER, MEDIA_SERVER, OTHER), 64-device cap, metadata-only (no credentials).
- **Passthrough controls** (`core/PassthroughController.kt`) — three independent toggles (camera / audio / text). Camera is capability-gated via `RootDetector`; `effectiveCameraOn()` never lies about what the compositor will actually do on stock.

### Added — panels

- **Devices panel** — add / rename / trust / remove over the device registry.
- **Voices panel** — start clones, toggle, remove, retry FAILED ones.
- **Passthrough panel** — preference + effective badge for camera; plain On/Off for audio + text.
- **Virtual Monitors panel (M0 stub)** — scan button + empty state. Real companion handshake lands in companion M1.
- **Emulator launcher panel** — detects and launches 14 RetroArch libretro-core systems + 8 standalones (DuckStation, AetherSX2, PPSSPP, Dolphin, Vita3K, Citra, Yuzu/Suyu, Lemuroid). Market intent for not-yet-installed.

### Added — build tools

- **10 new widgets (16 → 26)**: CPU_CORES, SCREEN_METRICS, LOCALE, TIME_ZONE, KERNEL, PERM_SUMMARY, DEVICE_COUNT, VOICES_SUMMARY, BATTERY_TEMP, DISK_FREE.
- **Build Tools widget layout persisted** via `BuildToolsPrefs`; the picker survives app restarts.
- **8 new buzz patterns (8 → 16)**: TRACKING_LOST, AUDIO_FAULT, UPDATE_READY, GUARDIAN_LOST, OVERHEAT, ROOT_DETECTED, PASSTHROUGH_ON, PASSTHROUGH_OFF. `BuzzPattern.ALL` is now the source of truth.

### Added — companion desktop app (M0 skeleton)

- **`companion/`** — Rust 2021 Tauri 2 workspace alongside `app/`. Two crates:
  - `crates/protocol` — serde JSON wire format (HELLO/WELCOME/FRAME/INPUT/DISPLAY_READY/DISPLAY_GONE/CURSOR/ERROR/GOODBYE). 4 unit tests.
  - `src-tauri` — idle-loop daemon so launchd/systemd/Windows Service can monitor the process.
- **`companion/ROADMAP.md`** — 7-milestone plan (M0 scaffolding → M6 installers).
- **`.github/workflows/companion.yml`** — 3-OS cargo check + test + fmt + clippy.

### Added — docs

- `docs/user-guide/virtual-monitors.md`, `emulators.md`, `device-registry.md`, `voices.md`, `passthrough.md`.
- `docs/reference/programmable-ai.md`.
- ADRs 0010–0015: multi-provider AI wake names, chat history per backend, companion-as-subfolder, emulator detect-and-launch, device-registry metadata-only, voice-training state machine.

### Added — Strip Meta

- 20 more packages (12 → 32): parties, messenger, Quill Player, insights, vrshellenv, mobile_mrc_setup, casting, guardian LEAVE, keyboard LEAVE, mrservice, spatial_capture, cameraservice LEAVE, horizon_updater, integrityservice, ovrmobileplatformsvc, sysutil, brush, horizonmobile, two Facebook residuals.

### Changed — toolchain

- JDK 17 → **21**
- Gradle 8.10 → **8.13**
- AGP 8.6.1 → **8.9.3**
- Kotlin 2.0.21 → **2.1.21**
- Compose BOM 2024.10.01 → **2025.10.00**
- compileSdk + targetSdk 35 → **36**
- activity-compose, lifecycle, coroutines, material3, datastore, core-ktx, navigation-compose all bumped in lockstep.
- CI `java-version` 17 → 21 in `build.yml` + `release.yml`.

### Fixed

- AI provider setup form: Kotlin 2.0 couldn't infer `Map<String, String>` serializer — now passes an explicit `MapSerializer` to `json.encodeToString` / `decodeFromString`.
- Build-time import misses after new panels landed (`VirtualMonitorPanel`, `EmulatorPanel`): both wired through `NostalgicHomeScreen.kt` with explicit imports.
- `PassthroughController.cameraPassthroughSupported` reads `rootDetector.status().isRooted` (nested on `RootStatus`), not `rootDetector.isRooted()`.

## [0.2.0] - 2026-11-08

### Added
- `core/RootDetector.kt` — library-level root detection (su-on-PATH, Magisk paths, /system writable, test-keys build, userdebug).
- `core/FunctionalityRestore.kt` — the 30s ask → attempt-fix → display → fallback → buzz ladder from the design brief.
- Seven new panels: Strip Meta, Firmware Dump, Boot Animation, Dev Super-Mode, Home Menu Swapper, AR Semicircle, Functionality Restore.
- `ui/nostalgic/RootGate.kt` — the verbose "this requires root" explainer card every gated panel uses.
- 10 new Build Tools HUD widgets: IP address, uptime, battery, thermal zones, storage breakdown, SoC info, process count, root status, build host, SELinux mode.
- Firmware review findings F-19 through F-30 added to `docs/security/firmware-review.md` (userdebug `ro.adb.secure=0` cross-build gap, VrShell overprivileged manifest, MLPerf harness in retail, orchestrator socket perms, QSEE 6.0 version pinning, fastboot in ABL).
- `docs/security/emulation-faq.md` — the honest answer to "why can't you just run it in QEMU?"
- ADR 0008 — AR semicircle uses 90° arc (ergonomics; detailed geometry).
- ADR 0009 — Root gating philosophy: detect-don't-elevate, with legal rationale.
- Seven user-guide pages for the new panels.
- JUnit 4 test dependency; unit tests for `BuzzPattern.toTimings()` and the varint parser shape used by `PayloadVerifier`.
- Four new labels: `security`, `root-gated`, `emulation`, `adr`.
- Six new tracking issues (#25-#31).

### Changed
- README "Hard constraints" table split into stock / rooted columns to reflect what the v0.2.0 root-gated panels unlock.
- Docs nav (mkdocs.yml) extended with the seven new user-guide pages + emulation FAQ + ADRs 0008/0009.

## [0.1.0] - 2026-11-07

Initial public release.

### Added
- Nostalgic retro-Oculus Compose UI shell.
- Panels: Launcher, Diagnostics, AI Companion, Build Tools, Updates, About, Signature Verifier, System Internals, Meta Hosts Audit.
- AR Home: 90°-semicircle preview rendered on Compose canvas.
- Three AI backends: Claude, OpenAI, Ollama.
- `ControllerHaptics` with 8 pre-registered buzz patterns.
- `PayloadVerifier` — on-device native-Kotlin port of `tools/verify_payload.py`.
- `MetaHostsAudit` with 24 categorized Meta hosts.
- Self-updater against this repo's GitHub Releases.
- Build Tools HUD: 6 functional widgets (Clock, Fingerprint, Free memory, Network ping, Quick note, Haptic test).
- CI: `build.yml` (debug APK on every push/PR) + `release.yml` (signed release on tag).
- Zero-dep Python tools under `tools/`: `pbdump`, `payload_summary`, `payload_extract`, `inspect_images`, `verify_payload`.
- MIT license. No telemetry. Co-Authored-By trailers on every commit.

[Unreleased]: https://github.com/A-Hackers-Guide/QuestClassic/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/A-Hackers-Guide/QuestClassic/releases/tag/v0.1.0
