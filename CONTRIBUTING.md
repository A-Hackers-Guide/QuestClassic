# Contributing to QuestClassic

Welcome. The repo is designed so almost everything useful you can land is a small, single-file PR. Below is the straight path.

## One-time setup

```bash
git clone https://github.com/<owner>/QuestClassic.git
cd QuestClassic
./gradlew :app:assembleDebug
```

Requires JDK 17 and the Android SDK. Gradle brings everything else.

## Where to find the thing to change

| You want to… | Open… |
|---|---|
| Add a widget to the HUD | `app/src/main/kotlin/io/github/questclassic/app/ui/panels/BuildToolsPanel.kt` |
| Add an AI provider | New file under `app/src/main/kotlin/io/github/questclassic/app/ai/` + register in `AIRepository.kt` |
| Add a diagnostic | `app/src/main/kotlin/io/github/questclassic/app/core/DiagnosticsEngine.kt` |
| Add a buzz pattern | `app/src/main/kotlin/io/github/questclassic/app/core/ControllerHaptics.kt` + `docs/diagnostics_patterns.md` |
| Wire up real OpenXR/passthrough for AR Home | `app/src/main/kotlin/io/github/questclassic/app/ar/ARHomeActivity.kt` + `docs/ar_mode.md` |
| Fix the launcher list | `app/src/main/kotlin/io/github/questclassic/app/core/AppListRepository.kt` |
| Change the look | `app/src/main/kotlin/io/github/questclassic/app/ui/theme/*.kt` and `nostalgic/*.kt` |

## PR standards

- **One file, one change** where you can. Easier to review, easier to revert.
- **No new third-party dependencies** without a reason stated in the PR. The whole point of this app is a small build.
- **No telemetry, ever.** Even opt-in. The app's privacy posture is "nothing phones home except api.github.com for updates."
- **Kotlin style**: official `kotlin.code.style=official`. Four-space indent.
- **Compose**: prefer small `@Composable`s over god-functions. Extract when a screen passes ~150 lines.

## Co-authoring commits

Every commit from this project includes a `Co-Authored-By` trailer for the collaborator who worked on it. Contributions by Claude (via Claude Code) carry:

```
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
```

Add your own `Co-Authored-By:` lines if a PR was pair-programmed with another human. Every co-author also unlocks GitHub's `Pair Extraordinaire` badge — see [`docs/achievements.md`](docs/achievements.md).

## Code of conduct

Be kind. Be technically honest. Don't ship surveillance. Don't help anyone extract a signing key.
