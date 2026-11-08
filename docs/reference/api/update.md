# API — `update` package

`SelfUpdater` + its wire types.

## `SelfUpdater`

```kotlin
class SelfUpdater(context: Context, http: OkHttpClient) {
    suspend fun checkLatest(): GithubRelease?
    fun pickApk(release: GithubRelease): GithubAsset?
    fun isNewer(release: GithubRelease): Boolean
    suspend fun downloadApk(asset: GithubAsset): File?
    fun install(apk: File)

    companion object {
        const val OWNER = "REPLACE_WITH_OWNER"   // rewritten at release time by CI
        const val REPO = "QuestClassic"
    }
}
```

### Usage

```kotlin
val release = container.updater.checkLatest() ?: return
if (container.updater.isNewer(release)) {
    val asset = container.updater.pickApk(release) ?: return
    val file = container.updater.downloadApk(asset) ?: return
    container.updater.install(file)
}
```

### `checkLatest()`

- HTTP GET `https://api.github.com/repos/$OWNER/$REPO/releases/latest`
- Headers: `Accept: application/vnd.github+json`
- Deserialize via `kotlinx.serialization` with `ignoreUnknownKeys = true`.
- Returns `null` on any non-2xx, non-JSON, or unparseable response.

### `pickApk(release)`

Picks the first asset whose name ends with `.apk` (case-insensitive). If a release has multiple APKs (e.g. we add arm64 + universal later), the picker will need to get smarter.

### `isNewer(release)`

Strips a leading `v` from both `release.tag_name` and `BuildConfig.VERSION_NAME`, then calls `compareSemver`. Returns `true` iff the release version strictly exceeds the installed version.

### `compareSemver(a, b)` (internal)

Simple component-wise numeric compare on `.`/`-`-split pieces. Non-numeric components are treated as 0.

- `"1.0.0" vs "0.9.0"` → 1
- `"1.0.0" vs "1.0.0"` → 0
- `"1.0.0-rc.1" vs "1.0.0"` → 0 (treats `rc` as 0; good enough for v0, see [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12))

### `downloadApk(asset)`

- HTTP GET `asset.browser_download_url`
- Streams to `cacheDir/questclassic-update.apk`
- Returns the `File` on success, `null` on any failure.

### `install(apk)`

- Builds a FileProvider URI with authority `${applicationId}.updates`.
- Starts `Intent.ACTION_VIEW` with `application/vnd.android.package-archive` + `FLAG_GRANT_READ_URI_PERMISSION` + `FLAG_ACTIVITY_NEW_TASK`.
- Android's system package installer takes over.
- First time, the user is prompted to grant "Install unknown apps" to QuestClassic.

## Wire types

```kotlin
@Serializable
data class GithubAsset(val name: String, val browser_download_url: String, val size: Long)

@Serializable
data class GithubRelease(
    val tag_name: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
    val body: String? = null,
)
```

`snake_case` because that's what GitHub's REST API emits. Keeping them that way (vs renaming with `@SerialName`) makes the types traceable one-to-one with [GitHub's release schema](https://docs.github.com/en/rest/releases/releases#get-the-latest-release).

## Owner/repo rewrite at CI time

`release.yml` runs:

```
sed -i "s|REPLACE_WITH_OWNER|${{ github.repository_owner }}|g" SelfUpdater.kt
sed -i "s|\"QuestClassic\"|\"${{ github.event.repository.name }}\"|g" SelfUpdater.kt
```

So a fork gets its own release channel without any code edits. Same reason we keep the constants in a `companion object` (easy to `sed`) rather than a build-config field (would need Gradle to know the owner).

---

_Last reviewed against v0.1.0._
