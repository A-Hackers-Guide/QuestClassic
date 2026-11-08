# Changelog

All notable changes to QuestClassic are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows semver-adjacent (`MAJOR.MINOR.PATCH`).

## [Unreleased]

(no changes since 0.2.0)

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
