# Architecture overview

One page. If any of this gets hazy, each bullet's deeper doc is linked.

## The whole app on one diagram

```
┌────────────────── Compose UI layer ───────────────────┐
│  MainActivity → NostalgicHomeScreen → 9 panel files   │
│       ↓                                                │
│  QuestClassicTheme (dark navy + teal nostalgic)        │
└───────────────┬────────────────────────────────────────┘
                │
        ┌───────┴─────────┐
        │  AppContainer   │   hand-wired DI
        │  (single class) │
        └───────┬─────────┘
                │
   ┌────────────┼──────────────────┬──────────────┬───────────┐
   ▼            ▼                  ▼              ▼           ▼
 core/        ai/                 os/           update/       …
 ─────        ───                 ──            ──────
 AppList      AIBackend           Device-       SelfUpdater
 Controller   ClaudeBackend       Introspec-
 Haptics      OpenAIBackend       tion
 Diagnos-     OllamaBackend       SystemProp-
 ticsEngine   AIRepository        ertyProbe
 PayloadVer-
 ifier
 MetaHosts-
 Audit
```

- **Single-module** app (`:app`) — see [ADR 0003](../adr/0003-hand-wired-di.md) on why no multi-module.
- **No runtime DI library** — `AppContainer` is one hand-wired class. See same ADR.
- **OkHttp** for every network call — one client, shared, with 10 s / 30 s timeouts.

## Request flow — the longest path

User taps **Signature Verifier → Pick OTA**:

```
LauncherPanel ▸ sys file-picker ▸ ActivityResultContracts.OpenDocument
                      ↓ user picks .zip
PayloadVerifier.verify(uri)
  on Dispatchers.IO
  ▸ ContentResolver.openInputStream
    ▸ ZipInputStream → extract payload.bin + otacert.pem
  ▸ RandomAccessFile(payload.bin, "r")
    ▸ read CrAU header → parse version / manifest_size / meta_sig_size
    ▸ SHA-256 pass 1 (header + manifest)
    ▸ parse Signatures protobuf → RSA sig bytes
    ▸ Signature.getInstance("NONEwithRSA") → verify
    ▸ parse manifest → find signatures_offset / _size
    ▸ SHA-256 pass 2 (header + manifest + data_blobs section)
    ▸ parse 2nd Signatures protobuf → verify
  → Report(digests, valid/invalid per sig, cert subject/issuer/fingerprint)
Panel redraws with the glass cards
```

Where it touches Android: ContentResolver + ZipInputStream + RandomAccessFile. Where it touches Java crypto: `MessageDigest`, `Signature`, `CertificateFactory`. Where it touches our code: three Kotlin files. That's it.

## Threading model

- **All Composables** on the main thread.
- **All I/O** on `Dispatchers.IO` via explicit `withContext` blocks.
- **`CoroutineScope(Dispatchers.IO).launch { … }`** for fire-and-forget (ping widget).
- **No app-level singletons** aside from `AppContainer` itself.

## Package layout

| Package | Responsibility |
|---|---|
| `io.github.questclassic.app` | `MainActivity`, `QuestClassicApplication` |
| `app.core` | DI container + repositories (AppList, Haptics, Diagnostics, PayloadVerifier, MetaHostsAudit) |
| `app.ai` | Backend interface + Claude/OpenAI/Ollama impls + `AIRepository` |
| `app.os` | `DeviceIntrospection`, `SystemPropertyProbe` (reflection-wrapped getprop) |
| `app.update` | `SelfUpdater` + Github release protobuf types |
| `app.ar` | `ARHomeActivity` (immersive-ish; currently 2D preview) |
| `app.ui.theme` | Color palette + typography |
| `app.ui.nostalgic` | Shared glass panel, chip label, stat row, home grid |
| `app.ui.panels` | One file per panel |

## What's deliberately absent

- No analytics SDK.
- No crash-reporting SDK (we might add Sentry opt-in later; design decision tracked in [ADR 0004](../adr/0004-no-telemetry.md)).
- No Firebase.
- No Hilt / Dagger / Koin / Anvil.
- No multi-module.
- No `BuildFlavor` gymnastics — one app, one flavor.

## Where it goes from here

- [architecture/module-map.md](module-map.md) — file-by-file count and purpose.
- [architecture/compose-ui.md](compose-ui.md) — what we use from Compose and what we don't.
- [architecture/di-container.md](di-container.md) — why one class is enough.
- [architecture/coroutines.md](coroutines.md) — scope rules.
- [architecture/storage.md](storage.md) — DataStore + cache usage.
- [architecture/theming.md](theming.md) — the nostalgic palette and why.

---

_Last reviewed against v0.1.0._
