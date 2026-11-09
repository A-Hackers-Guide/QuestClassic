# Companion (desktop)

QuestClassic Companion is a desktop app (macOS `.pkg`, Windows `.msi`, Linux `.AppImage`) that pairs with the headset over Wi-Fi to provide features that need a bigger brain than the Quest SoC alone: multi-monitor streaming, file transfer, voice training pipelines, LAN-aware tool dispatch.

**Status: M0 (bootable skeleton).** The companion currently announces its presence and listens on `localhost:48122`. Everything beyond that is on its roadmap at `companion/ROADMAP.md`.

## Install

Builds aren't published yet — the GitHub Actions matrix for `companion/.github/workflows/companion.yml` runs on every PR but doesn't upload installers. Build from source:

```bash
cd companion
cargo build --release
```

The binary lands in `target/release/`. On macOS, `cargo bundle` builds the `.app`.

## Pairing (planned)

Pairing goes through mDNS:

- Companion advertises `_questclassic-companion._tcp.local.` on port 48122.
- Headset resolves the service and shows the host in the Devices panel.
- First connection asks you (on both sides) to confirm. Once trusted, the device is saved in the headset's device registry.

The shared service-type / default-port / TXT-record schema lives in `companion/crates/discovery` so both sides compile against the same contract. See [ADR 0024](../adr/0024-mdns-backend-free-core.md).

## Protocol

Wire frames are JSON over WebSocket. The envelope lives in `companion/crates/protocol/`:

```rust
enum Frame {
    Hello { … },
    Welcome { … },
    Input { … },
    Frame { … },
    Bye { … },
    …
}
```

See `companion/PROTOCOL.md` for the current field list and semantics.

## Why not bundle it in the headset build?

Because the headset build is a Quest-class Android app and the desktop build is a Tauri (Rust + webview) binary; different toolchains, different runtimes. The shared data types live in Rust crates that compile standalone (`companion/crates/discovery`, `companion/crates/protocol`) so the APK can read the same wire contracts via a small Kotlin mirror.

See [ADR 0012 — Companion as subfolder](../adr/0012-companion-as-subfolder.md) for why the companion lives in a subfolder rather than a sibling repository.
