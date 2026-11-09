# Virtual Monitors panel

Stream up to four virtual monitors wirelessly to a paired Mac / Windows / Linux / FreeBSD host running the **QuestClassic Companion** desktop app.

The headset handles window geometry + input; the companion owns the per-OS virtual-display driver and routes your Quest controller as mouse + keyboard.

## Status

**M0 (skeleton)** — The companion scaffolding + wire protocol has landed. The Quest panel currently shows "no companion found" and a brief explainer. mDNS scanning + handshake lands in **M1**.

See `companion/ROADMAP.md` in the repo for the full milestone plan — summary:

| Milestone | Delivers |
|---|---|
| M0 ✅ | Workspace, protocol, panel stub, CI |
| M1 | mDNS discovery + HELLO/WELCOME handshake |
| M2 | Mouse / keyboard injection per OS |
| M3 | One virtual display per OS |
| M4 | WebRTC video stream |
| M5 | Multiple displays (realistic cap on Quest 2: **~4**, not 100) |
| M6 | Always-running daemon + `.pkg` / `.msi` / `.AppImage` installers |

## Why the 100-screen ceiling isn't realistic on Quest 2

Snapdragon XR2 Gen 1's H.264 encoder tops out around 4× 1080p60 streams before frame drops. 8 might work for low-motion content; 20+ is Quest 3/Quest Pro territory. We ship with hard caps so the UI never advertises something the SoC can't deliver.

## Why there's a desktop companion

Android alone can't create a virtual display on another OS. Each target needs a native driver:

- **macOS** — CoreGraphics virtual display via a system extension. Requires `com.apple.developer.virtual-display` entitlement; app must be notarised.
- **Windows** — Indirect Display Driver, signed through Microsoft HLK. Without the signature, we fall back to a windowed stream.
- **Linux** — EVDI kernel module or DRM virtual output.
- **FreeBSD** — community EVDI port; best-effort.

The companion owns these; the Quest just paints pixels.

## Where state lives

Nothing on the headset today. Discovered companions live in RAM. Trusted companions move to the [Device Registry](device-registry.md) once M1 lands.

## Protocol quick-reference

See `companion/PROTOCOL.md` for the full spec. One line each:

| Op | Direction | Meaning |
|---|---|---|
| `HELLO` | Quest → Companion | Opening hello with version, capabilities, requested display count |
| `WELCOME` | Companion → Quest | Accept + host OS + max displays + supported input kinds |
| `DISPLAY_READY` | Companion → Quest | Virtual display allocated; geometry + refresh |
| `FRAME` | Quest → Companion | One encoded video frame for a display |
| `INPUT` | Quest → Companion | Mouse / keyboard / touch event |
| `CURSOR` | Companion → Quest | Host-side cursor position for the on-headset reticle |
| `ERROR` | Companion → Quest | Non-fatal error with code + detail |
| `GOODBYE` | both | Clean shutdown |

## Troubleshooting

- **"No companion found"** on a Mac/PC with the companion installed — mDNS isn't advertising yet (M0 doesn't). Expected.
- **Companion won't start on macOS** — grant *Local Network* permission in System Settings → Privacy. Required for mDNS.
- **Linux: "EVDI not available"** — install the `evdi-dkms` package or build it; the AppImage ships a udev rule.

---

_Companion-side source lives under `companion/` in this repo._
