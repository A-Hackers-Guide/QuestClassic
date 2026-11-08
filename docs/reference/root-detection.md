# Root detection — the signals we check

QuestClassic's root detection lives in [`core/RootDetector.kt`](https://github.com/A-Hackers-Guide/QuestClassic/blob/main/app/src/main/kotlin/io/github/questclassic/app/core/RootDetector.kt). It inspects **five** signals on first query and memoises the result.

| Signal | What we look for | Value if hit |
|---|---|---|
| `suOnPath` | `su` binary at any of 10 standard paths: `/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/data/local/su`, `/data/local/bin/su`, `/data/local/xbin/su`, `/system/sd/xbin/su`, `/system/bin/failsafe/su`, `/su/bin/su`, `/vendor/bin/su` | `true` → classical Android root |
| `magiskPathsPresent` | Any of `/data/adb/magisk`, `/data/adb/magisk.db`, `/data/adb/modules`, `/sbin/.magisk`, `/cache/.disable_magisk` | `true` → Magisk installed |
| `systemWritable` | Can create a benign canary file at `/system/.qc_root_probe` | `true` → `/system` has been remounted RW |
| `testKeysBuild` | `Build.TAGS` contains `test-keys` | `true` → signed with developer test keys, not retail |
| `debuggableBuild` | `Build.TYPE` is `userdebug` or `eng` | `true` → non-production build |

## Confidence

```kotlin
val confidence: Confidence
    get() = when {
        suOnPath && magiskPathsPresent -> Confidence.HIGH
        suOnPath || magiskPathsPresent || systemWritable -> Confidence.MEDIUM
        testKeysBuild || debuggableBuild -> Confidence.LOW
        else -> Confidence.NONE
    }
```

- **HIGH** — two independent root mechanisms both visible. Very safe to run privileged actions.
- **MEDIUM** — one strong signal. Safe to run; action may still fail if SELinux is in enforcing mode and blocks a specific syscall.
- **LOW** — the build is developer-flagged but no actual root binary is present. QuestClassic treats this as "not rooted" for action purposes; the panel just shows the dev-flag so you know.
- **NONE** — stock retail. Panels show dry-run only.

## What we DO NOT do

1. **No `su` invocation.** Running `Runtime.exec("su -c ...")` is noisy, triggers device policy, and tells you nothing the file check didn't already tell you.
2. **No SafetyNet / Play Integrity attestation.** That's Google's cloud attestation; it doesn't tell you anything about root, only about Google's trust model.
3. **No QSEE challenge.** That's Meta's business, and we don't impersonate retail apps.
4. **No exploit attempt.** QuestClassic *detects* root; it does not *obtain* root. See [ADR 0009](../adr/0009-root-gating-philosophy.md).

## What the result unlocks

See each panel's user-guide page for the action it enables:

- [Strip Meta](../user-guide/strip-meta.md)
- [Firmware Dump](../user-guide/firmware-dump.md)
- [Boot Animation](../user-guide/boot-animation.md)
- [Developer Super-Mode](../user-guide/dev-super.md)
- [Home Menu Swapper](../user-guide/home-menu-swapper.md)

## Caching

`RootDetector` memoises `status()` after the first call. To re-query (after you've just enabled or disabled root), call `refresh()`.

## Related

- [ADR 0009 — Root gating philosophy](../adr/0009-root-gating-philosophy.md)
- [Firmware review § F-20 — ro.adb.secure on userdebug](../security/firmware-review.md#f-20--roadbsecure--0-on-userdebug-branches-of-the-same-tag--low-medium)
