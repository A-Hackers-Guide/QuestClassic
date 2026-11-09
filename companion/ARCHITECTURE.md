# QuestClassic Companion — architecture

```
┌─────────────────────────────────────────┐
│          Quest-side app (Kotlin)       │
│                                         │
│   VirtualMonitorPanel  ──► MonitorClient│
│                               │         │
│                               ▼         │
└───────────────────────── ws + rtc ──────┘
                          │           │
                   control│           │media
                          ▼           ▼
┌───────────────────────── Companion ─────────────────────────┐
│                                                             │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│   │ Protocol     │    │ Session      │    │ Transport    │  │
│   │  (serde)     │◄──►│  Manager     │◄──►│  (axum)      │  │
│   └──────────────┘    └──────────────┘    └──────────────┘  │
│           ▲                   ▲                             │
│           │                   │                             │
│   ┌───────┴──────────────┬────┴────────────────┐           │
│   ▼                      ▼                     ▼           │
│ ┌────────────┐    ┌──────────────┐    ┌──────────────┐    │
│ │ Display    │    │ Input        │    │ Video        │    │
│ │  driver    │    │  injector    │    │  decoder     │    │
│ │ (per OS)   │    │  (per OS)    │    │  (ffmpeg)    │    │
│ └────────────┘    └──────────────┘    └──────────────┘    │
│      │                   │                    │            │
│      ▼                   ▼                    ▼            │
│   OS GPU            OS input           OS window           │
│   display           queue              surface             │
└─────────────────────────────────────────────────────────────┘
```

## Modules

### `crates/protocol/`

- Pure `serde` definitions of every message in [PROTOCOL.md](PROTOCOL.md).
- No runtime dependencies, no I/O.
- Both the companion and (later, via a Kotlin stub generator) the Quest app consume this as the single source of truth.

### `src-tauri/src/session.rs`

- One `Session` per connected headset.
- Owns a channel to each display driver + input injector + video decoder.
- Routes incoming `INPUT` frames to the injector, incoming `FRAME`s to the decoder.
- Holds the state machine — `Connecting → Ready → Streaming → ClosingDown`.

### `src-tauri/src/transport.rs`

- Axum websocket upgrade for control traffic.
- `webrtc-rs` peer-connection for video + audio.
- mDNS advertiser (`_questclassic._tcp`).

### `src-tauri/src/display/mod.rs` — the per-OS line

Trait `DisplayDriver` with implementations under:

- `display/macos.rs` — CoreGraphics virtual display via a system extension. Requires `com.apple.developer.virtual-display` entitlement; app must be notarised.
- `display/windows.rs` — Indirect Display Driver. Requires Microsoft HLK signing; without it, we expose a windowed H.264 player and set `maxDisplays = 0`.
- `display/linux.rs` — EVDI kernel module (userspace library is `libevdi`); check `/dev/dri/*` for an EVDI-capable node.
- `display/freebsd.rs` — stub. Advertises `maxDisplays = 0` and documents the EVDI port issue.

### `src-tauri/src/input/mod.rs`

Trait `InputInjector`:

- macOS: `CGEventPost` with Accessibility permission request flow.
- Windows: `SendInput` from `winapi`.
- Linux: `uinput` device (`/dev/uinput`). Requires a udev rule for non-root use; we ship one in the AppImage.
- FreeBSD: `/dev/uinput` port (available since 13.0).

### `src-tauri/src/video.rs`

- WebRTC incoming track decoding (ffmpeg via `ffmpeg-next`, or `openh264-sys2` for AV1-less builds).
- One decoder per display.
- Output: ARGB frame buffer → display driver's `push_frame(display_id, buffer)`.

### `ui/`

The Tauri webview. Not the display surface — that's the OS. The webview just shows:

- Connection status (headset model + pairing code).
- Per-display thumbnails + "add display" / "remove display" controls.
- Settings: resolution, codec, port, mDNS name, auto-start.
- A live latency readout (control round-trip + video end-to-end).

## Lifecycle

1. User installs the companion. Installer registers the auto-start hook.
2. On launch, companion starts the Axum server on port 48122 and the mDNS advertiser.
3. Quest app opens the `VirtualMonitorPanel`, scans mDNS, finds the companion, connects, says `HELLO`.
4. Companion replies `WELCOME` with the per-OS cap.
5. Quest asks for `displays: 2`. Companion allocates displays, sends `DISPLAY_READY` × 2.
6. Companion opens a WebRTC peer connection for video + audio.
7. Quest streams `FRAME`s + `INPUT`s. Companion decodes + injects.
8. When the user removes a display in the Quest panel, Quest sends `GOODBYE` on that display's RTC track; companion tears down its virtual display.
9. On companion quit, companion sends `GOODBYE`; Quest shows "disconnected".

## Why Tauri and not a native app per OS

- Same webview code (settings UI) runs on all four OSes.
- Tauri 2 has the right async story (`tokio` under the hood) for an always-on bridge.
- Native integration (display drivers + input injection) goes through the Rust side where we want it anyway — the webview isn't on the hot path.
- Smaller binaries than an Electron companion.

## Why websocket + WebRTC and not pure WebRTC

- Control traffic is small, infrequent, and needs ordered delivery → websocket is simpler.
- Video traffic wants congestion control + low-latency ordered delivery with configurable retransmit → WebRTC data+video tracks are exactly that.
- Keeping them separate means we can hot-swap the video transport (MoonlightVR-style ENet experiment, say) without touching the control protocol.
