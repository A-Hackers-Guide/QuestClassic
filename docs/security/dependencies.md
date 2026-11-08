# Dependency policy

What we pull in, how we review it, and how we keep it safe.

## Current direct deps

See `gradle/libs.versions.toml`. In rough order of install-time blast radius:

| Dependency | Version | Why |
|---|---|---|
| Kotlin stdlib + coroutines | 2.0.21 / 1.9.0 | Language runtime |
| Jetpack Compose BOM | 2024.10.01 | Compose version alignment |
| Compose Material3 | 1.3.1 | UI widgets |
| Compose Material icons extended | BOM-managed | Icon set |
| AndroidX Core KTX | 1.15.0 | Platform-glue |
| AndroidX Lifecycle (runtime, viewmodel-compose) | 2.8.7 | Lifecycle-aware Composables |
| AndroidX Activity Compose | 1.9.3 | Compose host activity |
| AndroidX Navigation Compose | 2.8.4 | (planned future use — currently unused, candidate for drop) |
| AndroidX DataStore Preferences | 1.1.1 | AI config persistence |
| OkHttp | 4.12.0 | HTTP client (all three AI backends + updater) |
| kotlinx.serialization-json | 1.7.3 | JSON for the four wire types we define |

Build-time only:

| Dependency | Version |
|---|---|
| Android Gradle Plugin | 8.6.1 |
| Kotlin Gradle Plugin | 2.0.21 |
| Compose Kotlin Compiler Plugin | 2.0.21 (follows Kotlin) |

## What we deliberately don't pull in

- **No Firebase.** Nothing from Google's consumer stack. We don't need analytics, crashlytics, remote config, or FCM.
- **No Hilt / Dagger / Koin / Anvil.** See [ADR 0003](../adr/0003-hand-wired-di.md).
- **No Gson / Moshi.** `kotlinx.serialization` covers us.
- **No Retrofit.** OkHttp is enough for 4 endpoints.
- **No Coil / Glide / Fresco.** We don't load remote images; app icons come from `BitmapDrawable`.
- **No Timber.** `android.util.Log` is fine; we don't have that much logging.

## Review bar for adding a dependency

A PR that adds a new dependency gets more scrutiny than one that adds code. Questions the PR must answer:

1. **Is this a maintained library?** First-party AndroidX, JetBrains, Square, Google — yes. One-person GitHub repos — only if trivially auditable.
2. **What's its APK-size impact?** Run `./gradlew :app:assembleRelease` before and after; note the diff.
3. **What's its transitive-dep footprint?** `./gradlew :app:dependencies` diff.
4. **Does it do any network calls at startup?** No. If yes, hard block unless there's an opt-out.
5. **Does it embed native libraries?** If yes, which architectures, how big?
6. **Does it do reflection on hidden Android APIs?** If yes, needs manual review per Android version.
7. **License?** Apache 2.0 / MIT / BSD-3-Clause — fine. GPL — case-by-case; LGPL only if we can `.so`-link (we're Kotlin, so this matters less).

## Patching schedule

- **Security CVEs** in direct deps: update within the week.
- **Security CVEs** in transitive deps: evaluate; often blocked by upstream, in which case we document the window.
- **Feature updates**: batched, roughly monthly.

Dependabot runs on the repo (will be enabled in the first follow-up PR); PRs get reviewed like any other.

## SBOM

We publish a software bill of materials with each release:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > sbom.txt
```

Attached to the GitHub release as `sbom.txt`. [Issue #11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11) peripherally tracks formalizing this.

---

_Last reviewed against v0.1.0._
