# Toolchain uplift plan (tracking [#28](https://github.com/A-Hackers-Guide/QuestClassic/issues/28))

Dependabot opened five Gradle-dep PRs that all failed CI because they're major-version jumps that depend on a newer JVM, Gradle, Kotlin, or Android SDK. Merging them piecemeal would thrash the build. This doc is the coordinated uplift plan we'll execute in one PR before Dependabot's next sweep.

## Target baseline

| Thing | Current | Target | Why |
|---|---|---|---|
| JDK | 17 | 21 (LTS) | AGP 9.x hard-requires 21. |
| Gradle wrapper | 8.10.2 | 9.0.0+ | AGP 9.x requires Gradle 9. |
| Kotlin | 2.0.21 | 2.1.0+ | Needed by compose-bom 2026.09 and the kotlinx-serialization 1.12 release train. |
| AGP (com.android.application) | 8.6.1 | 9.5.1 | Current latest; brings the modern lint rules + non-transitive R classes default. |
| compileSdk | 35 | 36 | Needed by datastore-preferences 1.3 and navigation-compose 2.10. |
| targetSdk | 35 | 36 | Match compileSdk. |
| minSdk | 29 | 29 | **Unchanged** — see [ADR 0007](../adr/0007-min-sdk-29.md); dropping Quest 2 users is a non-goal. |
| JUnit | 4.13.2 | 4.13.2 | **Unchanged**; our tests are JVM-only. |

## The piecemeal Dependabot PRs waiting on this

| PR | Bump | Blocker |
|---|---|---|
| #18 (closed) | kotlinx-serialization 1.7.3 → 1.12.0 | Kotlin 2.1+ |
| #20 (closed) | datastore-preferences 1.1.1 → 1.3.0 | compileSdk 36 |
| #21 (closed) | navigation-compose 2.8.4 → 2.10.2 | compileSdk 36 |
| #22 (closed) | compose-bom 2024.10.01 → 2026.09.00 | Kotlin 2.1+ |
| #23 (closed) | AGP 8.6.1 → 9.5.1 | JDK 21, Gradle 9 |

All five were closed with a pointer to #28. Dependabot re-proposes the same bumps on its next weekly cycle, which gives us one week to land the uplift before they pile up again.

## Execution order

1. **CI: bump JDK 17 → 21** in both `build.yml` and `release.yml`. One commit. Verify CI still passes.
2. **Gradle wrapper**: `./gradlew wrapper --gradle-version 9.0.0 --distribution-type bin` and commit `gradle/wrapper/*`. CI run.
3. **Kotlin 2.0.21 → 2.1.0** in `libs.versions.toml` under `kotlin` ref. Run `./gradlew :app:compileDebugKotlin` locally; may surface deprecations.
4. **AGP 8.6.1 → 9.5.1** in `libs.versions.toml` under `agp`. Expect AGP 9 to tighten several lint rules; address them in the same PR.
5. **compileSdk + targetSdk 35 → 36** in `app/build.gradle.kts`. One file.
6. **The 4 held-back deps** (`compose-bom`, `navigation-compose`, `datastore-preferences`, `kotlinx-serialization`) — bump each ref in `libs.versions.toml`.
7. **Lint pass** — run `./gradlew :app:lintDebug` and fix anything new.

All 7 steps in one PR titled `chore: toolchain uplift (JDK 21, Gradle 9, Kotlin 2.1, AGP 9.5, compileSdk 36)`.

## Risks

- **AGP 9's namespace enforcement.** If any legacy R class path exists it will break. We don't have any, but AGP 9 may still surface strict warnings.
- **Compose BOM 2026.09.** New Compose adds a lot of API deprecations around Modifier.foundation and `Text` overloads. Expect 10-20 small call-site changes.
- **Kotlin 2.1 K2 fallout.** The new K2 compiler frontend is default; some of our edgier type inference calls may need explicit types.
- **Gradle 9's configuration cache.** AGP 9 enables it by default; a task that doesn't support it will be loud.

## Fallback

If the uplift PR takes more than ~3 hours of hand-holding, revert the AGP bump (back to 8.7.x which is still 2024-current) and keep JDK 21 + Kotlin 2.1 + compileSdk 36 as the baseline. That still unblocks 3 of the 5 Dependabot PRs.

## Who's assigned

Open — pick up [#28](https://github.com/A-Hackers-Guide/QuestClassic/issues/28) if you want to drive it.
