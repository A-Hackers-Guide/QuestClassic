# Coding standards

Short because Kotlin + Compose give us most of this for free.

## Kotlin

- `kotlin.code.style=official` is in `gradle.properties`. 4-space indent. Tabs are banned.
- Max line length: ~120. Nobody's rejecting a PR for 125; nobody's writing 200-column lines either.
- `val` by default; `var` when you mean "this changes."
- Prefer `expression body` for short functions:
  ```kotlin
  fun double(x: Int): Int = x * 2
  ```
- Null safety: use `?.`, `?:`, `let`, `runCatching`. Avoid `!!` unless the invariant is literally impossible to violate.
- Return `Result<T>` from suspending functions that can fail in expected ways (network, parse, permission).
- One class per file where the class has substance; multiple short enums/data-classes per file is fine.

## Compose

- Composable names: `PascalCase`.
- Composables take `modifier: Modifier = Modifier` as the FIRST modifier param, not last.
- State lives in the Composable that owns it unless lifting is justified.
- Stateless Composables are preferred — they take data in, call back out.
- Private Composables (file-scoped helpers) are `private fun`, not `internal`.
- Never block the main thread. If something might: `Dispatchers.IO` via `withContext`.
- Preview functions optional; the headset is the real preview.

## Naming

- **Repositories** end in `Repository` (`AppListRepository`, `AIRepository`).
- **Engines** end in `Engine` (`DiagnosticsEngine`).
- **Panels** end in `Panel` (`LauncherPanel`, `AIAssistantPanel`).
- **Activities** end in `Activity`.
- **Interfaces** have no `I` prefix. We're Kotlin, not AIDL.

## Dependencies

- New deps go through [security/dependencies.md](../security/dependencies.md)' review bar.
- Pin versions in `gradle/libs.versions.toml`. No floating versions.
- No transitive forcing without a reason in the PR.

## Comments

Default to writing none. Add a comment only when the "why" is non-obvious. See the project-wide guidance in [CONTRIBUTING.md](../../CONTRIBUTING.md).

Prefer:

```kotlin
// AOSP ships this field in little-endian on A/B v2+; big-endian on legacy. We're A/B-only.
val version = struct.unpack("<Q", header)
```

Not:

```kotlin
// Parse the version
val version = struct.unpack("<Q", header)
```

## Formatting

Run Android Studio's "Reformat Code" on changed files. Don't reformat files you're not touching. The project doesn't have ktlint or detekt wired up yet; low on the priority list but a good first PR candidate.

---

_Last reviewed against v0.1.0._
