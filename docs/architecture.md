# Architecture

QuestClassic is a sideloaded Android app. It targets Android 10+ (`minSdk=29`, matching the Quest 2's `ro.product.first_api_level`) and `targetSdk=34` (Android 14).

## Modules

```
app/
├── MainActivity           Jetpack Compose host
├── ui/
│   ├── theme/             Dark navy + teal nostalgic palette, system fonts
│   ├── nostalgic/         Reusable glass-panel card, chip label, stat row, home grid
│   └── panels/            One file per feature — Launcher, Diagnostics, AI, Build, Update, Settings
├── core/
│   ├── AppContainer       Hand-wired DI graph (no runtime DI libraries)
│   ├── AppListRepository  Enumerate every launchable app on the headset (VR + 2D)
│   ├── DiagnosticsEngine  Self-tests from userspace APIs only
│   └── ControllerHaptics  Morse-like buzz patterns for error codes
├── os/
│   └── DeviceIntrospection  getprop via reflection + android.os.Build
├── ai/
│   ├── AIBackend          Interface
│   ├── ClaudeBackend, OpenAIBackend, OllamaBackend
│   └── AIRepository       DataStore-backed config + backend registry
├── update/
│   └── SelfUpdater        GitHub Releases polling + PackageInstaller handoff
└── ar/
    └── ARHomeActivity     Compose preview of the 90° semicircle hand menu
                           (OpenXR + passthrough integration lands in a PR)
```

## Dependency choices

- **Jetpack Compose + Material3** for all UI. Native Compose runs on anything from Android 5+; we don't bring Unity or Unreal just to render menus.
- **OkHttp** for every network call (self-update, AI backends). Small, battle-tested, works on any Android.
- **DataStore (Preferences)** for the AI config. Protobuf DataStore would be overkill.
- **Kotlin Serialization** for Github/Claude/OpenAI/Ollama JSON — no Gson, no Moshi.
- **Coroutines** for everything async. No RxJava.
- **No Hilt / Dagger** — one `AppContainer` class, hand-wired. Zero annotation processing, faster builds.
- **No Firebase, no analytics.** We don't phone home.

## Why there's no module boundary between features

The app is small (10-ish files per feature area, ~2 kLOC total). Compose + Kotlin's single-module speed beats the organisational benefits of multi-module setup at this size. If the app grows past ~15 kLOC we'll split `:core`, `:ai`, `:update` out.

## Testing strategy

- The AI backends are pure functions of `(systemPrompt, history, apiKey, model)` → `Result<String>`. Unit testable with a fake `OkHttpClient` dispatcher.
- `SelfUpdater.compareSemver` is pure and the primary place things can go wrong.
- UI is tested manually on a Quest with the CI-built APK.

See also:
- `diagnostics_patterns.md` — every buzz pattern and what it means.
- `ai_providers.md` — adding a new AI backend.
- `widget_sdk.md` — adding a new HUD widget.
- `ar_mode.md` — how the Compose preview maps to the real OpenXR integration.
- `achievements.md` — the GitHub achievements we can grind on this repo.
