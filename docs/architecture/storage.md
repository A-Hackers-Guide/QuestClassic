# On-device storage

Where we put things and why.

## DataStore (Preferences)

One file:

```
/data/data/io.github.questclassic.app/datastore/ai_config.preferences_pb
```

Keys:

| Key | Type | Purpose |
|---|---|---|
| `name` | String | Assistant name |
| `backend` | String | Backend ID (`claude` / `openai` / `ollama`) |
| `model` | String | Optional model override |
| `api_key` | String | **Sensitive.** The AI provider's API key or Ollama base URL. |
| `prompt` | String | System prompt |

Backed by `androidx.datastore.preferences.preferencesDataStore("ai_config")` — same file format as SharedPreferences but with coroutine-safe writes and no cross-process concerns.

### Why DataStore and not SharedPreferences

SharedPreferences has well-known bugs on config change + commit-vs-apply races. DataStore is the modern replacement. Overhead is zero; API is cleaner.

### Why not encrypted storage

- The DataStore file is already in app-private storage (world-unreadable).
- On a rooted headset, encryption-at-rest with a user-derived key would offer marginal protection against someone who already has root.
- Android Keystore-backed encryption is possible (Jetpack Security crypto) and has a cost we didn't want to pay in v0.1.0. [security/known-limits.md](../security/known-limits.md) notes this.

### Excluded from Android cloud backup

`res/xml/data_extraction_rules.xml` lists this file as excluded from both cloud backup and device-transfer:

```xml
<cloud-backup>
    <exclude domain="sharedpref" path="ai_config.preferences_pb" />
</cloud-backup>
<device-transfer>
    <exclude domain="sharedpref" path="ai_config.preferences_pb" />
</device-transfer>
```

So your API key never flows to Google Drive (even though it's unlikely to on a Quest; the Quest doesn't integrate with Android auto-backup the way a phone does).

## Cache dir

`context.cacheDir` — used for:

- **Downloaded APK during self-update** → `cacheDir/questclassic-update.apk`. Replaced on each update; Android purges it under memory pressure, which is fine.
- **Temp payload.bin extraction** during signature verification (streamed from the picked .zip into a temp file, then deleted when verification returns). We use `File.deleteOnExit()` + an explicit `finally { file.delete() }`.

The cache dir is exposed to the FileProvider via `res/xml/file_provider_paths.xml`:

```xml
<cache-path name="updates" path="." />
```

Which means the system's package installer can read `questclassic-update.apk` through a content:// URI without us needing `WRITE_EXTERNAL_STORAGE`.

## Everything else is ephemeral

- **Diagnostics results**: lifetime = one panel visit.
- **AI chat history**: in-memory `SnapshotStateList`. Cleared on Composable nav-away. Intentional — the panel is for quick ad-hoc chats, not sessions.
- **Quick note widget**: in-memory `mutableStateOf("")`. Cleared on Composable nav-away.
- **Meta Hosts Audit results**: in-memory `SnapshotStateList`. Re-runs on panel open.

## Why so little persisted

v0.1.0 is small enough that most "state" is cheap to recompute. We'll add DataStore-backed persistence to the Quick Note widget and to Build Tools widget selection as issues get picked up (see `docs/widget_sdk.md`).

---

_Last reviewed against v0.1.0._
