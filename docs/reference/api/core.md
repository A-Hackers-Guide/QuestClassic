# API — `core` package

Repositories and engines that panels talk to. Everything here lives under `app/src/main/kotlin/io/github/questclassic/app/core/`.

## `AppContainer`

See [architecture/di-container.md](../../architecture/di-container.md). This is the one-class DI graph.

## `AppListRepository`

```kotlin
class AppListRepository(context: Context) {
    suspend fun load(): List<InstalledApp>
    fun launch(app: InstalledApp): Boolean
}

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val launchIntent: Intent,
    val isVrApp: Boolean,
)
```

- `load()` runs on `Dispatchers.IO`, enumerates `MAIN/LAUNCHER` and `MAIN/com.oculus.intent.category.VR` intents, dedupes, sorts, returns.
- `launch(app)` calls `startActivity(launchIntent)` with `FLAG_ACTIVITY_NEW_TASK`. Returns `true` on success, `false` on any throw.

## `ControllerHaptics`

```kotlin
class ControllerHaptics(context: Context) {
    fun playPattern(pattern: BuzzPattern)
    fun cancel()
}

data class BuzzPattern(val codeLetters: String, val label: String) {
    fun toTimings(): LongArray

    companion object {
        val OK: BuzzPattern
        val BATTERY_LOW: BuzzPattern
        val STORAGE_FULL: BuzzPattern
        val WIFI_DOWN: BuzzPattern
        val OTA_CHECK_FAIL: BuzzPattern
        val CONTROLLER_MISSING: BuzzPattern
        val THERMAL: BuzzPattern
        val DISPLAY_FAULT: BuzzPattern
    }
}
```

See [reference/buzz-patterns.md](../buzz-patterns.md) for timings and semantics.

## `DiagnosticsEngine`

```kotlin
class DiagnosticsEngine(context: Context, haptics: ControllerHaptics) {
    suspend fun runAll(): List<DiagnosticsResult>
    fun buzzFailures(results: List<DiagnosticsResult>)
}

data class DiagnosticsResult(
    val name: String,
    val ok: Boolean,
    val value: String,
    val failPattern: BuzzPattern? = null,
)
```

- `runAll()` runs on `Dispatchers.IO`, returns 6 results.
- `buzzFailures(results)` picks the first failing result with a `failPattern` set and plays it. If none, plays `OK`.

## `PayloadVerifier`

```kotlin
class PayloadVerifier(context: Context) {
    suspend fun verify(archiveUri: Uri): Result<Report>

    data class Report(
        val archiveName: String,
        val payloadSize: Long,
        val manifestSize: Long,
        val metaSigSize: Int,
        val signaturesOffset: Long,
        val signaturesSize: Int,
        val metadataDigestHex: String,
        val finalDigestHex: String,
        val metadataSignatureValid: Boolean,
        val finalSignatureValid: Boolean,
        val certSubject: String,
        val certIssuer: String,
        val certNotAfter: String,
        val certSha256Fingerprint: String,
    ) {
        val allGood: Boolean
    }
}
```

- `verify(uri)` reads the picked .zip streaming, extracts `payload.bin` and `META-INF/com/android/otacert` from the ZIP's central directory on the fly.
- Returns a `Result` — success Report or a captured `Throwable`.
- Zero-dep: pure Java crypto + our own varint parser.

See [user-guide/signature-verifier.md](../../user-guide/signature-verifier.md) for the UX and [reverse-engineering/ota-format.md](../../reverse-engineering/ota-format.md) for the format background.

## `MetaHostsAudit`

```kotlin
object MetaHostsAudit {
    enum class Category { AUTH, GRAPH, GENAI, PAYMENTS, CDN, INTERNAL }

    data class Host(val name: String, val category: Category, val note: String = "")
    data class Probe(val host: Host, val ip: String?, val reachable: Boolean, val latencyMs: Long?, val error: String?)

    val all: List<Host>
    suspend fun probe(host: Host, timeoutMs: Int = 1500): Probe
}
```

- `all` is the 24-host catalog.
- `probe(host)` runs on `Dispatchers.IO`, returns a `Probe` with the resolved IP, latency, or error.

## Threading summary

Every public suspend function here uses `withContext(Dispatchers.IO) { … }` internally. Callers don't need to switch dispatchers themselves.

---

_Last reviewed against v0.1.0._
