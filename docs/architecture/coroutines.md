# Coroutines & threading

How work actually runs in this app.

## Rules

1. **All Composables run on main.** Nothing blocking in a Composable body.
2. **All I/O (disk, network, crypto) runs on `Dispatchers.IO`** via explicit `withContext(Dispatchers.IO) { … }`.
3. **UI effects (fire-and-forget on user action) use `rememberCoroutineScope`.** That scope is cancelled when the Composable leaves composition, so no leaks.
4. **Fire-and-forget without a scope is a bug.** The one exception right now is `NetworkPingWidget`, which we noted as a cleanup target.
5. **No `GlobalScope`.** Ever.

## Typical patterns

### Background load on panel open

```kotlin
var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
LaunchedEffect(Unit) { apps = container.appList.load() }
```

`LaunchedEffect(Unit)` runs once when the Composable enters composition; if the user navigates away before it finishes, Compose cancels it.

### User-triggered async work

```kotlin
val scope = rememberCoroutineScope()
Button(onClick = {
    scope.launch {
        running = true
        results = container.diagnostics.runAll()
        container.diagnostics.buzzFailures(results)
        running = false
    }
})
```

`rememberCoroutineScope()` gives you a `CoroutineScope` tied to the Composable. Launches on it get cancelled on nav-away.

### Repository-level background work

Repositories use `withContext(Dispatchers.IO) { … }` internally; callers (Composables) stay on main:

```kotlin
suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
    // heavy PackageManager queries here
}
```

### Parallel fan-out

For the Meta Hosts Audit we probe 24 hosts in parallel:

```kotlin
val results = MetaHostsAudit.all.map { host ->
    async { MetaHostsAudit.probe(host) }
}.awaitAll()
```

`async` + `awaitAll` on the current scope. No manual thread pool.

## Why not Flow?

For the current panel set, `remember { mutableStateOf(…) }` + one-shot `LaunchedEffect` covers every use case cleanly. `StateFlow` + `collectAsState` is strictly more verbose for the same outcome here. When we have long-lived subscriptions (a notification stream, continuous diagnostics, real OpenXR frame data) we'll adopt `Flow` there and keep plain State elsewhere.

## Why not a ViewModel per panel?

ViewModels survive configuration changes (rotation, dark-mode flip, keyboard open). On a Quest 2:

- No rotation changes — the headset's primary-display orientation is baked in.
- No dark-mode toggling — we force dark scheme.
- No system keyboard typically — VR apps bring their own.

So a Composable's `rememberSaveable` lifetime matches a ViewModel's for our panels. Adding ViewModels would be extra ceremony with no behavior difference. We'll adopt them when state has to outlive a Composable (e.g. real AR mode's immersive frame pipeline).

## Cancellation safety

A test worth running yourself after adding a long-running coroutine: mid-work, tap the panel's **Close** button. If anything keeps running past that, you have a scope leak. The current codebase has one known leak (the ping widget); everything else cleans up correctly.

---

_Last reviewed against v0.1.0._
