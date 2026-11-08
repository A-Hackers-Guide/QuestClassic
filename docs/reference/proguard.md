# ProGuard / R8 rules

Our rules file is small and focused. In `app/proguard-rules.pro`:

```proguard
-keep class io.github.questclassic.app.ai.** { *; }
-keepclassmembers class io.github.questclassic.app.update.GithubRelease { *; }
-keepclassmembers class io.github.questclassic.app.update.GithubAsset { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
```

## What each line does

- **`-keep class io.github.questclassic.app.ai.** { *; }`**
  Keeps every class under `app.ai.*` with all members. R8 could otherwise rename `ClaudeBackend` → `a$b$c` and break the `kotlinx.serialization` wire types that use reflection-ish property discovery at runtime. Since we have three backends and the AI panel's picker lists them via reflection-friendly `backendById`, we keep them visible.

- **`-keepclassmembers class …GithubRelease { *; }` + `GithubAsset`**
  Keeps the field names of the two JSON wire types `kotlinx.serialization` reads from GitHub. If R8 renamed `browser_download_url` to `a`, deserialization would fail at runtime.

- **`-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement`**
  Suppresses a benign warning emitted by OkHttp's internal use of a shaded animal_sniffer annotation that's not on our classpath.

## Why so little

- Our serialization is `kotlinx.serialization` with `@Serializable` — codegen-based, not reflection-based, so most classes don't need `-keep`.
- Our DI container is hand-wired — no reflection discovery to protect.
- Compose compiler generates its own metadata and handles its own keeps.

## When to add a rule

- You're using a new library that fails at runtime in a release build.
- You added a class that's instantiated by name (reflection or JNI).
- Adding a `@Keep` annotation to the class works too and localizes the rule; prefer that where possible.

## Testing R8 output

```bash
./gradlew :app:assembleRelease
# Inspect:
./gradlew :app:analyzeReleaseBundle    # if bundle analyzer is wired
unzip -l app/build/outputs/apk/release/app-release.apk | head
```

If something crashes in release but works in debug, R8 almost certainly stripped something; look in `app/build/outputs/mapping/release/` for the mapping file and reverse the trace.

---

_Last reviewed against v0.1.0._
