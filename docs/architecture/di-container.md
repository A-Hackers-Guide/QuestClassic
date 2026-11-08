# DI container

One hand-wired class. See [ADR 0003](../adr/0003-hand-wired-di.md) for the full reasoning.

## The container

```kotlin
class AppContainer(private val app: Context) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val appList: AppListRepository = AppListRepository(app)
    val haptics: ControllerHaptics = ControllerHaptics(app)
    val diagnostics: DiagnosticsEngine = DiagnosticsEngine(app, haptics)
    val introspection: DeviceIntrospection = DeviceIntrospection(app)
    val updater: SelfUpdater = SelfUpdater(app, http)
    val ai: AIRepository = AIRepository(app, http)
    val payloadVerifier: PayloadVerifier = PayloadVerifier(app)
    val systemProbe: SystemPropertyProbe = SystemPropertyProbe()
}
```

That's the whole container. Created once in `QuestClassicApplication.onCreate()`; passed as a `val container: AppContainer` into `setContent` → `NostalgicHomeScreen(container)` → every panel.

## Rules

1. **Nothing outside the container constructs a repository.** If you need one, add a `val` here.
2. **Nothing stashes a `Context` long-term** — the container holds `Application` scope (safe) and passes it down; panels use `LocalContext.current` when they need the current `Context`.
3. **No circular deps.** The declaration order in `AppContainer` is the dependency order; if a repo depends on another, the dependency is declared earlier. Right now the only intra-container dep is `DiagnosticsEngine(app, haptics)`.
4. **No lazy init** unless you need to defer a real cost. Everything above is cheap to construct.

## Why not Hilt / Dagger / Koin / Anvil?

- **Hilt / Dagger**: KAPT or KSP, annotation processing, several hundred ms of extra build time, debuggable only through generated code. Worth it on multi-module 50+-screen apps; wildly over-engineered at 10 panels.
- **Koin**: lightweight-ish but runtime graph construction, error-at-runtime (missing binding → crash at first read), DSL lock-in.
- **Anvil**: the Dagger-with-Compose-friendly variant; same arguments as Dagger.
- **Hand-wired**: every dependency is a plain Kotlin val, compile-time-verified, 0 ms of annotation processing, you read the container and you know what exists.

If this app ever grows to 15+ panels or starts needing per-user scopes, we'll revisit. Until then: one class.

## Adding a new dependency

1. Open `core/AppContainer.kt`.
2. Add one line: `val mything: MyThing = MyThing(app, http)` (or whatever args it needs).
3. Done. Any panel that takes a `container: AppContainer` can now use `container.mything`.

If your new thing needs another new thing, declare that one first in the container. The lint rule is just: later lines can refer to earlier `val`s.

---

_Last reviewed against v0.1.0._
