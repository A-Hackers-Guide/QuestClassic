# QuestClassic

A sideloadable Android app for Meta Quest 2 (and newer) that gives you a **nostalgic Oculus‑style launcher, a programmable AI companion, device self‑diagnostics with Morse‑like buzz patterns, a build‑your‑own HUD, a 90° semicircular hand menu in AR, and its own update channel — all without touching Meta's OTA or breaking anything on your device**.

It installs like any other APK (ADB sideload). It survives every Meta OTA because user‑installed APKs don't get wiped by A/B updates. It updates itself from this repository's GitHub Releases. It phones home to exactly one host: `api.github.com`, to check for its own updates. Nothing else.

## What it actually does

| Panel | Feature |
|---|---|
| **Launcher** | Nostalgic retro-Oculus grid of every launchable app on the headset. Tiles are tagged `VR` (teal) or `2D` (amber). Tap to launch. |
| **Diagnostics** | Runs userspace self‑tests (battery, storage, RAM, network, device fingerprint, performance). On failures, buzzes the controllers in a Morse‑like code pattern you can match against [`docs/diagnostics_patterns.md`](docs/diagnostics_patterns.md) — works even if the display dies. |
| **AI Companion** | You name it, pick a backend (Claude / OpenAI / local Ollama), enter your own API key, write its system prompt. Chat UI. API key stays on-device in DataStore, excluded from Android cloud backup. See [`docs/ai_providers.md`](docs/ai_providers.md). |
| **Build Tools** | Minimizable HUD built from widgets you turn on and off. Ships with Clock + Fingerprint; more widgets are one PR each. See [`docs/widget_sdk.md`](docs/widget_sdk.md). |
| **Updates** | Polls this repo's GitHub Releases, offers to download and install a newer APK through Android's standard `PackageInstaller`. |
| **About** | What your headset actually is — model, HorizonOS branch, codename, SoC, security patch (user‑facing AND the per-partition vendor one that's often years behind), reality mode. Also launches the experimental AR Home. |
| **AR Home** | A Compose preview of the 90° semicircular hand menu. The real passthrough + hand‑tracking integration lands in the follow-up PR described in [`docs/ar_mode.md`](docs/ar_mode.md). |

## Hard constraints — read this before filing a feature request

A sideloaded APK on a retail Meta Quest **cannot** do these things, and no amount of clever programming changes that. We've documented the full reasoning in the parent project's `REVERSE_ENGINEERING_REPORT.md` (sections 7, 9, 15, 18); the TL;DR table:

| Thing | Why not |
|---|---|
| Replace the Meta boot animation | `vr_bootanim` is on a dm-verity-signed partition. |
| Strip Meta apps from the system image | Same. The hashtree would fail verify. |
| Intercept the boot‑time "three lines" menu | That's bootloader + recovery, both signed. We run after boot. |
| Dump device signing keys | They live in Qualcomm TrustZone / Keybox, inaccessible from Android. |
| Grant ourselves `com.oculus.horizon.auth` | Signature permission — needs Meta's platform key, which we don't have. |
| Modify an installed Meta APK in place | Breaks the APK signature; Android refuses to run it. |
| Install a kernel module / driver | AVB verifies every partition at boot. |

What we **can** do is live comfortably alongside Meta's system, in exactly the sandbox any user‑installed app gets on this device. That's what the panels above do.

## Install

### From a release (end-user path)

1. Enable Developer Mode on your Meta account at [developer.meta.com](https://developer.meta.com/manage/) — Meta's one-time "verify yourself" flow. No account unlock, no bootloader unlock — Meta's official developer flag.
2. Enable USB debugging on the headset: *Settings → System → Developer → USB Connection Dialog = on, ADB = on*.
3. Plug the headset into your PC, allow USB debugging in the headset.
4. Download the latest `QuestClassic-release.apk` from this repo's [Releases](../../releases).
5. `adb install QuestClassic-release.apk`.
6. Open it from *Library → Unknown Sources* on the headset.

### From source

```bash
git clone https://github.com/<owner>/QuestClassic.git
cd QuestClassic
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK; Gradle fetches everything else.

## Self-update channel

- The app polls `api.github.com/repos/<owner>/QuestClassic/releases/latest` once per launch.
- On a newer tag, it downloads the attached `*.apk` into `cacheDir` and hands off to `PackageInstaller` via a `FileProvider`-wrapped intent.
- The first time you update, Android asks you to grant "Install unknown apps" to QuestClassic in system Settings. One-time.
- Meta OTAs don't wipe user-installed APKs on A/B updates, so the app and its DataStore survive every HorizonOS update. See [`docs/architecture.md`](docs/architecture.md) for details.

## Repo layout

```
.
├── app/                   Android app source (Kotlin + Compose)
├── tools/                 Our zero-dep Python tools for reading Meta OTAs
├── docs/                  Design docs — architecture, buzz patterns, AR path, widget SDK, AI backends, achievements
├── .github/
│   ├── workflows/         Build + release CI
│   └── ISSUE_TEMPLATE/    Bug + feature templates
├── LICENSE                MIT
└── README.md              You are here
```

## CI and releases

- **`build.yml`** — runs on every push and PR. Builds a debug APK and attaches it as a workflow artifact. Validates the Gradle wrapper. 100 % cache-friendly.
- **`release.yml`** — fires on every `v*` tag. Patches the self-updater to point at this repo, builds a signed release APK (if a keystore secret is configured) or an unsigned one, and attaches it to a GitHub Release with auto-generated notes.

Signing (optional): set these GitHub secrets if you want signed releases:
- `QUESTCLASSIC_KEYSTORE_B64` — base64-encoded `.jks`
- `QUESTCLASSIC_KEYSTORE_PASSWORD`
- `QUESTCLASSIC_KEY_ALIAS`
- `QUESTCLASSIC_KEY_PASSWORD`

Without them, debug-signed APKs are built and published; still installable via ADB, just not update-compatible with a signed install.

## Contributing

PRs welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md). Good first PRs:

- Implement any of the stub widgets in [`docs/widget_sdk.md`](docs/widget_sdk.md).
- Add a new AI backend (see [`docs/ai_providers.md`](docs/ai_providers.md) — the interface is one function).
- Document a new buzz pattern and wire it up ([`docs/diagnostics_patterns.md`](docs/diagnostics_patterns.md)).
- Port a diagnostic from userspace APIs (e.g., real `/sys/class/thermal` readout).

## Legal / IP posture

**This repository contains only our own original code.** We do not distribute Meta's firmware, OTAs, decompiled APKs, or any of their proprietary binaries. See the parent project's `REVERSE_ENGINEERING_REPORT.md` (section 10, "Decompiled Java") for why those stay off-repo — DMCA risk is real and we're not inviting it.

Our tools in `tools/` can read a Meta OTA you obtained yourself from the official update servers, on your own hardware. They don't distribute anything.

## License

[MIT](LICENSE). Use it, fork it, learn from it.

## Credits

Hand-crafted during a reverse-engineering + build session with [Claude](https://www.anthropic.com/claude) ([Claude Code](https://claude.com/claude-code)). The initial RE pass that informed the design is in the parent project; everything here is new work.
