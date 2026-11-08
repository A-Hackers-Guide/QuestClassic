# API — `os` package

Read-only device introspection. Everything here uses `android.os.Build` + reflection into `android.os.SystemProperties` — same values Settings → About and `adb shell getprop` return.

## `DeviceIntrospection`

```kotlin
class DeviceIntrospection(context: Context) {
    data class Prop(val key: String, val value: String?)

    fun summary(): List<Prop>
    val isQuest: Boolean
}
```

Powers the About panel. Returns a curated list of 13 properties, in display order, for the summary card.

## `SystemPropertyProbe`

```kotlin
class SystemPropertyProbe {
    fun get(key: String): String?
    val summary: String
}
```

Powers the System Internals panel. Lets a Composable call `probe.get("ro.build.branch")` for any system property; returns `null` if the property doesn't exist or is empty.

## Why reflection

`android.os.SystemProperties` is a hidden API — public SDK stubs don't expose it. Both classes here reach it via:

```kotlin
val method = Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
val value  = method.invoke(null, key) as? String
```

This is permitted on Android 11+ without the warn-logs-and-eventually-blocks hidden-API restrictions because `SystemProperties.get(String)` is in the "greylist-max-o" (unrestricted) bucket. We do NOT read non-public setters or restricted properties.

## What we never read

- Properties under `persist.vendor.*` that could include device secrets (we read none of them).
- Properties under `ro.serialno` or `ril.serialnumber` (we don't need the device serial for anything).
- Nothing that requires elevated capabilities or hidden API bypass flags.

## Caveats

- On some Android versions `SystemProperties.get` for a non-existent key returns `""` (empty string); we normalize to `null`.
- Properties can change between reads (e.g. `ro.vendor.build.security_patch` is immutable, but `persist.*` ones are mutable). Our panels snapshot once per open.

---

_Last reviewed against v0.1.0._
