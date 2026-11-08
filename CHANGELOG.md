# Changelog

All notable changes to QuestClassic are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning follows semver-adjacent (`MAJOR.MINOR.PATCH`).

## [Unreleased]

### Added
- Complete docs reorganization under `docs/` with 30+ Markdown files: `getting-started/`, `user-guide/`, `architecture/`, `reference/`, `contributing/`, `security/`, `adr/`, `reverse-engineering/`, `roadmap/`, plus a top-level FAQ.
- `docs/security/firmware-review.md` — defensive audit of 18 findings observed in the Nov 2026 Quest 2 OTA (SPL gaps, info leaks, SELinux carve-outs, cert chain, crypto choices, attack surface). No exploit code.
- `emulation/` skeleton explaining what's realistic and what's not for live-firmware vulnerability testing (`unidbg/`, `cuttlefish/`, feasibility README).
- Seven ADRs (`docs/adr/0001` through `0007`) recording the decisions behind the current architecture.
- Governance files: `SECURITY.md`, `SUPPORT.md`, `CODE_OF_CONDUCT.md`, `CODEOWNERS`, `CHANGELOG.md`.
- YAML-form issue templates (`.github/ISSUE_TEMPLATE/*.yml`), PR template, issue template config.
- `mkdocs.yml` for optional site rendering via mkdocs-material.

### Changed
- `docs/achievements.md` moved to `docs/contributing/achievements.md`.

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
