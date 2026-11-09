# Building the companion

## Prerequisites

| OS | Needed |
|---|---|
| macOS | Xcode command-line tools, Rust stable, Node 22 |
| Windows | MSVC Build Tools 2022, Rust stable, Node 22, WiX toolset (for `.msi`) |
| Linux | `build-essential pkg-config libwebkit2gtk-4.1-dev libayatana-appindicator3-dev librsvg2-dev patchelf`, Rust stable, Node 22 |
| FreeBSD | `pkg install rust cmake pkgconf gmake`, Node 22 from pkg |

## Fast path (skeleton only)

```sh
cd companion
cargo check --workspace
cargo test --workspace
```

This is what runs in CI today — it validates the protocol crate + the daemon binary compiles. No Tauri install needed.

## Full Tauri build (adds UI)

```sh
cd companion/ui
npm install
npm run build

cd ..
cargo install tauri-cli --version '^2'
cargo tauri build
```

Produces:

- `src-tauri/target/release/questclassic-companion[.exe]` — the daemon
- `src-tauri/target/release/bundle/` — OS-native installers (`.pkg`, `.msi`, `.AppImage`, `.deb`, `.rpm`) once M6 lands

## Env knobs

| Env | Default | What it does |
|---|---|---|
| `QUESTCLASSIC_COMPANION_PORT` | `48122` | Websocket port |
| `QUESTCLASSIC_COMPANION_HOST` | `127.0.0.1` | Bind address |
| `RUST_LOG` | `info` | Tracing filter (`info`, `debug`, `trace`) |

## Troubleshooting

- **`webkit2gtk-4.1` not found on Linux**: install the `-dev` package; Tauri 2 won't build without it.
- **mDNS doesn't advertise on macOS**: check System Settings → Privacy → Local Network. Grant the daemon the entitlement before shipping.
- **FreeBSD**: Tauri works via webkit2gtk from ports. Virtual-display support is best-effort; ship as "client only" mode until EVDI is ported.
