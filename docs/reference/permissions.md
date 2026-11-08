# Manifest permissions

Every `uses-permission` in `app/src/main/AndroidManifest.xml`, what we use it for, and whether it prompts the user.

## Normal permissions (granted at install)

| Permission | Used by | What for |
|---|---|---|
| `android.permission.INTERNET` | AI Companion, Updates, Meta Hosts Audit, AppList (if ever) | HTTPS out |
| `android.permission.ACCESS_NETWORK_STATE` | Diagnostics, Updates | know whether we have a validated connection |
| `android.permission.ACCESS_WIFI_STATE` | Diagnostics (link-speed field) | read Wi-Fi link speed |
| `android.permission.VIBRATE` | ControllerHaptics | play buzz patterns |
| `android.permission.QUERY_ALL_PACKAGES` | AppListRepository | enumerate launchable apps on Android 11+ |
| `android.permission.FOREGROUND_SERVICE` | reserved for future | — |
| `android.permission.POST_NOTIFICATIONS` | reserved for future notifications | — |
| `android.permission.RECEIVE_BOOT_COMPLETED` | reserved; not currently wired | — |
| `android.permission.WAKE_LOCK` | Updates (during download/install) | keep CPU awake across a slow Wi-Fi install |

## Permissions the user grants after install

| Permission | Used by | Prompt |
|---|---|---|
| `android.permission.REQUEST_INSTALL_PACKAGES` | Updates (first install of a newer APK) | Settings → Apps → Install unknown apps → QuestClassic |

## Oculus / HorizonOS permissions (declared; reality is: gated)

These are in the manifest so the app behaves correctly where they ARE available — on a device where they're signature-granted, the behavior improves — but on a retail Quest 2 our APK is signed by our own key, so none of these are actually granted to us.

| Permission | What it would gate |
|---|---|
| `com.oculus.permission.USE_SCENE` | Scene API access (room geometry) |
| `horizonos.permission.USE_SCENE` | same, modern namespace |
| `com.oculus.permission.HAND_TRACKING` | hand-tracking cursors |
| `horizonos.permission.HAND_TRACKING` | same, modern namespace |

On retail: silent-denied. No crash; features gracefully degrade or are just unavailable (the AR Home preview doesn't actually track hands yet).

## Features (soft-required)

```xml
<uses-feature android:name="android.hardware.vr.headtracking" android:required="false" />
<uses-feature android:name="com.oculus.feature.PASSTHROUGH" android:required="false" />
<uses-feature android:name="oculus.software.handtracking" android:required="false" />
```

All `required="false"` — the app installs on any Android device. Features that need VR extensions degrade gracefully.

## Supported devices allow-list

```xml
<meta-data android:name="com.oculus.supportedDevices"
    android:value="hollywood|monterey|seacliff|eureka|panther" />
```

| Codename | Device |
|---|---|
| `hollywood` | Quest 2 |
| `monterey` | Quest Pro |
| `seacliff` | (reserved) |
| `eureka` | Quest 3 |
| `panther` | Quest 3S |

On Meta's Library the app surfaces for these device codenames. On non-Quest Android it still runs; just doesn't appear in the Quest-specific Library tab.

## Why we don't request `CAMERA`, `RECORD_AUDIO`, `READ_CONTACTS`, `FINE_LOCATION`, etc.

Because we don't need them and we promise not to have them. See [security/model.md](../security/model.md) for the privacy posture doc.

---

_Last reviewed against v0.1.0._
