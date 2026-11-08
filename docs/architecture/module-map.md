# Module map

Every Kotlin source file in `app/src/main/kotlin/io/github/questclassic/app/`, its role, and what else depends on it.

## `app/`

| File | Role | Depends on |
|---|---|---|
| `QuestClassicApplication.kt` | App init; constructs `AppContainer` on `onCreate` | `core.AppContainer` |
| `MainActivity.kt` | Compose host; passes `AppContainer` into `NostalgicHomeScreen` | `ui.theme.*`, `ui.nostalgic.NostalgicHomeScreen`, `QuestClassicApplication` |

## `app/core/`

| File | Role | Depends on |
|---|---|---|
| `AppContainer.kt` | Hand-wired DI graph; constructs all repositories once per process | everything in `core/`, `ai/`, `os/`, `update/` |
| `AppListRepository.kt` | Enumerate launchable apps; launch one via Intent | — |
| `ControllerHaptics.kt` | `Vibrator`-backed Morse-pattern player + `BuzzPattern` catalog | — |
| `DiagnosticsEngine.kt` | 6 self-tests + buzz-on-failure | `ControllerHaptics` |
| `PayloadVerifier.kt` | On-device port of `verify_payload.py` (OTA signature checker) | Java crypto |
| `MetaHostsAudit.kt` | 24-host list + DNS/ping prober | — |

## `app/ai/`

| File | Role | Depends on |
|---|---|---|
| `AIBackend.kt` | Interface + `ChatMessage`/`Role` types | — |
| `ClaudeBackend.kt` | Anthropic Messages API impl | `OkHttpClient`, `kotlinx.serialization` |
| `OpenAIBackend.kt` | OpenAI chat completions API impl | same |
| `OllamaBackend.kt` | Ollama LAN /api/chat impl | same |
| `AIRepository.kt` | DataStore-backed config (name, backend, model, key, prompt) + backend registry | all three backends |

## `app/os/`

| File | Role | Depends on |
|---|---|---|
| `DeviceIntrospection.kt` | Summary properties (About panel) | reflection |
| `SystemPropertyProbe.kt` | Raw `getprop` reader (System Internals panel) | reflection |

## `app/update/`

| File | Role | Depends on |
|---|---|---|
| `SelfUpdater.kt` | GitHub release poller + PackageInstaller handoff | `OkHttpClient`, `FileProvider` |

## `app/ar/`

| File | Role | Depends on |
|---|---|---|
| `ARHomeActivity.kt` | 2D preview of the 90° semicircle hand menu | Compose Canvas |

## `app/ui/theme/`

| File | Role |
|---|---|
| `Theme.kt` | `QCColors` palette, `DarkScheme`, `QuestClassicTheme` wrapper |
| `Type.kt` | `QCTypography` — system-font sans-serif + monospace for diag values |

## `app/ui/nostalgic/`

| File | Role |
|---|---|
| `NostalgicComponents.kt` | `GlassPanel`, `ChipLabel`, `StatRow`, `PanelHeader`, `TileButton` — reused across every panel |
| `NostalgicHomeScreen.kt` | Home grid + navigation switch over `HomeScreen` enum |

## `app/ui/panels/`

| File | Panel |
|---|---|
| `LauncherPanel.kt` | Launcher (+ shared `PanelClose` affordance) |
| `DiagnosticsPanel.kt` | Diagnostics |
| `AIAssistantPanel.kt` | AI Companion |
| `BuildToolsPanel.kt` | Build Tools HUD (with inline widget Composables) |
| `UpdatePanel.kt` | Updates |
| `SettingsPanel.kt` | About / System |
| `SignatureVerifierPanel.kt` | Signature Verifier |
| `SystemInternalsPanel.kt` | System Internals |
| `MetaHostsAuditPanel.kt` | Meta Hosts Audit |

## Dependency graph summary

```
MainActivity
 └─ NostalgicHomeScreen
     └─ all 9 panels
         └─ AppContainer
             └─ core/*, ai/*, os/*, update/*
                 └─ OkHttp, Java crypto, Android framework
```

Cycle-free, DAG, zero service locators, zero reflection-based DI. Easy to read.

## File-size budget

The whole app is ~2.5 MB APK (unsigned, no bundled native libs). Rough breakdown:

- Kotlin stdlib + coroutines: ~800 KB
- Compose runtime + UI + Material3: ~900 KB
- Material icons extended: ~250 KB
- OkHttp: ~900 KB
- kotlinx.serialization: ~200 KB
- Androidx core / lifecycle / activity: ~400 KB
- Our code + resources: ~150 KB
- DEX overhead + manifests: ~100 KB

Growing much past 5 MB would mean we picked up something unnecessary. CI could add an APK-size check; tracked as a note on [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12).

---

_Last reviewed against v0.1.0._
