# QuestClassic Companion

Desktop daemon that pairs with the QuestClassic headset app to stream wireless virtual monitors, route input, and run an always-on background bridge for AI, OTA and controller events.

**Status: skeleton.** This directory is the scaffolding — build/CI, Tauri app shell, protocol crate, per-OS packaging — so the real work lands piece by piece without a greenfield PR every time. See [ROADMAP.md](ROADMAP.md) for the gated milestones.

## What this is

A Tauri 2 app (Rust backend + React UI) that runs on macOS / Windows / Linux / FreeBSD. It:

1. Exposes a local websocket endpoint the Quest app connects to.
2. Advertises itself on the LAN with mDNS so the Quest can find it without typing an IP.
3. Owns the per-OS *virtual display driver* that the Quest streams pixels into.
4. Owns the per-OS *input injector* that turns Quest controller / hand poses into mouse + keyboard events on the host.
5. Keeps itself alive at login via launchd / systemd / Windows Service, and auto-restarts on crash.

Packaging: `.pkg` (macOS), `.msi` (Windows), `.AppImage` + `.deb` + `.rpm` (Linux), binary tarball (FreeBSD).

## What's in this skeleton

- `Cargo.toml`, `src-tauri/`, `ui/` — Tauri 2 project layout.
- `crates/protocol/` — shared message types (serde-JSON over websocket) used by both ends.
- `ROADMAP.md` — the milestone-by-milestone plan for the real system.
- `ARCHITECTURE.md` — the per-OS driver surface + the stream pipeline.
- `.github/workflows/companion.yml` — 4-OS build matrix for the companion only (lives in the parent repo's `.github/workflows/`).

## What this skeleton deliberately does NOT do

- **It does not stream video yet.** The stream pipeline is TODO behind the protocol boundary.
- **It does not create a virtual display yet.** Each OS needs its own driver (macOS: CoreGraphics virtual display + a system extension; Windows: an Indirect Display Driver signed through Microsoft's partner program; Linux: EVDI kernel module or DRM virtual output). Those are tracked as their own issues in the parent repo.
- **It does not inject input yet.** macOS needs the Accessibility entitlement; Windows needs SendInput privileges; Linux needs uinput.

These are the four big "needs a real engineer-week each" line items. The skeleton makes the shape of the project real so those PRs can be scoped individually.

## Build

```bash
cd companion
rustup target add x86_64-apple-darwin aarch64-apple-darwin x86_64-pc-windows-msvc x86_64-unknown-linux-gnu
cargo check --workspace
cd ui && npm install && npm run build
cd .. && cargo tauri build
```

See [BUILD.md](BUILD.md) for per-OS prerequisites.

## Protocol (short version)

Over `ws://<host>:48122/ws` (default port), JSON frames:

```jsonc
// Quest → Companion
{ "op": "HELLO", "headsetId": "...", "displays": 2, "capabilities": ["h264", "opus"] }
{ "op": "FRAME", "displayId": 0, "ts": 123456, "payload": "<base64 h264 nal>" }
{ "op": "INPUT", "kind": "mouse_move", "x": 123, "y": 456, "displayId": 0 }
{ "op": "INPUT", "kind": "key", "scancode": 0x1C, "down": true }

// Companion → Quest
{ "op": "WELCOME", "companionId": "...", "maxDisplays": 4, "osCaps": ["mouse", "keyboard"] }
{ "op": "DISPLAY_READY", "displayId": 0, "width": 1920, "height": 1080, "scale": 1.0 }
{ "op": "CURSOR", "displayId": 0, "x": 500, "y": 300 }
```

Full spec in [PROTOCOL.md](PROTOCOL.md).
