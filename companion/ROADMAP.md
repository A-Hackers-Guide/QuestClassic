# QuestClassic Companion — roadmap

Milestones are deliberately coarse. Each unlocks the next; none hide a secret dependency.

## M0 — Skeleton (this PR)

- [x] `cargo check --workspace` passes
- [x] Tauri 2 app launches and shows a connection-status window
- [x] Protocol crate compiles on all four target OSes
- [x] Quest-side `VirtualMonitorPanel` stub that lists "no companion found"
- [x] Parent-repo CI workflow for the companion (`.github/workflows/companion.yml`)

## M1 — LAN discovery + handshake

- [ ] Companion advertises `_questclassic._tcp` via mDNS
- [ ] Quest-side mDNS resolver finds companion on the local subnet
- [ ] Websocket handshake: HELLO → WELCOME exchange
- [ ] Quest panel shows "connected to <hostname>"

## M2 — Input injection (mouse)

- [ ] macOS: `CGEvent` mouse synthesis + Accessibility permission flow
- [ ] Windows: `SendInput` synthesis
- [ ] Linux: `uinput` device creation + permission check
- [ ] Quest-side controller ray → mouse coordinate mapping
- [ ] Round-trip latency under 25 ms

## M3 — Virtual display (one)

- [ ] macOS: `CoreGraphics` virtual display via a notarised system extension
- [ ] Windows: Indirect Display Driver (Microsoft partner cert required — if we can't get one, fall back to a windowed stream)
- [ ] Linux: EVDI kernel module integration
- [ ] FreeBSD: EVDI port (community project) or hard-stop here
- [ ] Companion reports `DISPLAY_READY` with real geometry

## M4 — Video stream

- [ ] Quest-side H.264 encoder (MediaCodec)
- [ ] Companion-side H.264 decoder (ffmpeg or OS-native)
- [ ] WebRTC transport (`webrtc-rs` or re-use Tauri's Chromium)
- [ ] 60 fps at 1080p with less than 50 ms end-to-end latency

## M5 — Multiple displays

- [ ] Companion creates N virtual displays on demand
- [ ] Quest panel exposes "+" / "−" to add/remove monitors live
- [ ] Each display streams its own H.264 track
- [ ] Cursor passes between displays by geometry

## M6 — Always-running daemon + installers

- [ ] macOS: `.pkg` + launchd user-agent that restarts on crash
- [ ] Windows: `.msi` + Windows Service via `service-manager` crate
- [ ] Linux: `.AppImage` + `.deb` + `.rpm` + systemd user unit
- [ ] FreeBSD: tarball + rc.d script

## M7 — Beyond

- Touch events (Quest hand poses → multi-touch injection)
- Resize / move Quest-side monitor windows
- Background bridge: AI endpoint proxy, OTA delivery, controller telemetry
- Latency instrumentation + dashboard

## What this roadmap pushes back on

- **20-100 displays** is not a Quest 2 ceiling it will meet. Snapdragon XR2 Gen 1's H.264 encoder tops out around 4× 1080p60 streams before frame drops appear. Realistic M5 target is **4 monitors**; 8 will stretch the SoC on anything busy; anything beyond that needs Quest 3 / Quest Pro with better encoding hardware.
- **No cross-signing miracle.** The Windows IDD requires Microsoft HLK signatures via the Hardware Partner program; without that, we fall back to a windowed stream (less cool, but ships).
- **FreeBSD is best-effort.** No virtual-display kernel module exists upstream; we either port EVDI or ship FreeBSD as a client-only companion that can't *host* monitors.
