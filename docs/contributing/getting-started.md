# Getting started as a contributor

Clone, build, run, land a PR — in 15 minutes.

## Prerequisites

- JDK 17 (any distribution; Temurin is fine).
- Android SDK (easiest via Android Studio — install once, accept licenses).
- `adb` on your PATH.
- A Quest 2+ with [Developer Mode](../getting-started/enable-developer-mode.md) enabled OR an AOSP Cuttlefish instance OR any Android 10+ device/emulator.

## Build + run

```bash
git clone https://github.com/A-Hackers-Guide/QuestClassic.git
cd QuestClassic
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Gradle fetches deps on first build (~5 min). Subsequent builds are seconds.

## First-PR walkthroughs

### Add an AI backend

1. `cp app/src/main/kotlin/io/github/questclassic/app/ai/OpenAIBackend.kt \
      app/src/main/kotlin/io/github/questclassic/app/ai/GeminiBackend.kt`
2. Edit: change `id`, `displayName`, endpoint, request shape per [Google Gemini's API docs](https://ai.google.dev/gemini-api/docs).
3. Register in `AIRepository.backends`.
4. Build. Done.

Walkthrough details in [docs/ai_providers.md](../../docs/ai_providers.md).

### Add a diagnostic buzz pattern

1. Open `app/src/main/kotlin/io/github/questclassic/app/core/ControllerHaptics.kt`.
2. Add a constant to `BuzzPattern.Companion`:
   ```kotlin
   val MY_FAULT = BuzzPattern("-.-.-", "my fault description")
   ```
3. Add a row to [`docs/reference/buzz-patterns.md`](../reference/buzz-patterns.md).
4. Wire it up to the detection in `core/DiagnosticsEngine.kt`.

### Add a Build Tools widget

1. Open `app/src/main/kotlin/io/github/questclassic/app/ui/panels/BuildToolsPanel.kt`.
2. Add an entry to `Widget`:
   ```kotlin
   MY_WIDGET("My widget", @Composable { c -> MyWidget(c) }),
   ```
3. Write `@Composable private fun MyWidget(container: AppContainer) { ... }` in the same file.
4. Update the widget row in [`docs/widget_sdk.md`](../../docs/widget_sdk.md).

### Add a whole new panel

1. Create `app/src/main/kotlin/io/github/questclassic/app/ui/panels/MyPanel.kt`:

   ```kotlin
   @Composable
   fun MyPanel(container: AppContainer, onClose: () -> Unit) {
       Column(Modifier.fillMaxSize()) {
           PanelClose(title = "My panel", subtitle = "…", onClose = onClose)
           Spacer(Modifier.height(16.dp))
           GlassPanel(modifier = Modifier.fillMaxWidth()) {
               Text("Hello from my panel", color = QCColors.Soft)
           }
       }
   }
   ```

2. In `NostalgicHomeScreen.kt`:
   - Add `MY` to the `HomeScreen` enum.
   - Add the `when` branch dispatching to `MyPanel`.
   - Add a `HomeTile` entry.

3. Add a `docs/user-guide/my-panel.md` and link it from `docs/README.md`.

4. PR.

## PR process

See [contributing/pr-process.md](pr-process.md).

## Testing

See [contributing/testing.md](testing.md). Short version: there's no formal test suite yet; the first unit tests (for `PayloadVerifier`) are [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12).

---

_Last reviewed against v0.1.0._
