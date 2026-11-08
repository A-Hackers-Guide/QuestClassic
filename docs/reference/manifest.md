# AndroidManifest.xml — line by line

Our full manifest is ~70 lines. Here's each block, annotated.

## Permissions

See [permissions.md](permissions.md).

## Features

```xml
<uses-feature android:name="android.hardware.vr.headtracking" android:required="false" />
<uses-feature android:name="com.oculus.feature.PASSTHROUGH" android:required="false" />
<uses-feature android:name="oculus.software.handtracking" android:required="false" />
```

- `required="false"` on all three — the app installs regardless; features without the backing capability degrade.
- `vr.headtracking` — standard Android feature flag for head-tracked devices.
- `com.oculus.feature.PASSTHROUGH` — true on Quest 2+ in MR-capable modes.
- `oculus.software.handtracking` — true if hand-tracking is on.

## Supported-devices

```xml
<meta-data android:name="com.oculus.supportedDevices"
    android:value="hollywood|monterey|seacliff|eureka|panther" />
```

Appears twice — once on `<application>` and once on `<activity>` for `MainActivity`. Meta's Library uses whichever it finds first; duplicating is defensive.

## Application element

```xml
<application
    android:name=".QuestClassicApplication"
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="false"
    ...
```

- `allowBackup="false"` + `dataExtractionRules` — belt-and-suspenders against automated cloud backup picking up the AI key DataStore. See [architecture/storage.md](../architecture/storage.md).
- `fullBackupContent="false"` — same reason, for the legacy full-backup path.
- `android:icon="@mipmap/ic_launcher"` — adaptive icon (vector drawable background + foreground).

## MainActivity

```xml
<activity android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|uiMode"
    android:theme="@style/Theme.QuestClassic">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
        <category android:name="com.oculus.intent.category.VR" />
    </intent-filter>
</activity>
```

- `exported="true"` — needed on Android 12+ for any activity that has an `<intent-filter>`.
- `launchMode="singleTask"` — the app only ever has one task; subsequent launches reuse it.
- `configChanges=…` — we handle these ourselves (we're Compose, no need to recreate on a keyboard open).
- Dual LAUNCHER + VR categories — surfaces in both standard Android launchers and Meta's Library.

## ARHomeActivity

```xml
<activity android:name=".ar.ARHomeActivity"
    android:exported="false"
    android:launchMode="singleTask"
    android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|uiMode|density"
    android:theme="@style/Theme.QuestClassic"
    android:resizeableActivity="false" />
```

- `exported="false"` — only MainActivity launches it.
- `resizeableActivity="false"` — when we add real passthrough, we don't want the system packing us into a 2D panel window.
- `density` in configChanges — handling display-density changes without recreating is cheap and smooth.

## FileProvider (for Updates)

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.updates"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

- `authorities = "${applicationId}.updates"` — namespaced by package ID so debug and release builds don't collide (`applicationIdSuffix=".debug"` in debug build).
- `grantUriPermissions="true"` — needed to hand the downloaded APK URI to the system package installer.
- `res/xml/file_provider_paths.xml` grants only `cache-path name="updates"` — the installer can read our update APK, nothing else.

## AndroidX Startup

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge" />
```

Pulled in transitively by WorkManager / lifecycle-runtime even when we don't use the AppStartup DSL. Harmless; required for those libs' init on some Android versions.

---

_Last reviewed against v0.1.0._
