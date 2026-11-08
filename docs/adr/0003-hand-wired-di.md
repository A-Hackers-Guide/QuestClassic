# ADR 0003 — Hand-wired DI, no runtime DI library

- **Status**: Accepted
- **Date**: 2026-11-07

## Context

We need a way to construct and share repositories (AppList, Haptics, Diagnostics, PayloadVerifier, AI, Update, …) across panels. Options:

1. Hilt (Dagger wrapper for Android).
2. Dagger (plain).
3. Koin (DSL-based service locator).
4. Anvil (Dagger + Compose-friendly).
5. Hand-wired: one class that holds all the dependencies as `val`s.

## Decision

**Hand-wired**: `app/src/main/kotlin/io/github/questclassic/app/core/AppContainer.kt`, a single class constructed in `QuestClassicApplication.onCreate()`.

## Rationale

- The dependency graph is tiny. 9 repositories, no scoping concerns, no per-Activity-or-Fragment dependencies.
- Hilt/Dagger would add ~1 s of build-time annotation processing (KAPT) or KSP overhead, plus generated-code indirection when reading.
- Koin would add a runtime service-locator pattern where missing bindings fail at first-read at runtime instead of compile-time.
- Hand-wired is compile-time-verified, instant to read ("what exists? look at `AppContainer`"), zero-overhead at runtime, and has no toolchain footprint.

## When to revisit

We'll reconsider adopting Hilt when any of these happens:

- The graph passes 20 bindings.
- We need per-user or per-Activity scoping (real AR mode with a `SpatialActivity` session might trigger this).
- We split into multiple Gradle modules.

## Consequences

- +: faster build (no annotation processing).
- +: new contributors read one Kotlin class and know the whole graph.
- +: no runtime surprises (compile errors beat crashes-on-first-read).
- -: if we outgrow it, the migration to Hilt is a day of work. We accept the one-time cost later for the ongoing simplicity now.
