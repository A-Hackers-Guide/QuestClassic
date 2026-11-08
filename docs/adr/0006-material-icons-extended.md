# ADR 0006 — Depend on `material-icons-extended`

- **Status**: Accepted
- **Date**: 2026-11-07
- **Supersedes**: n/a
- **Related**: this ADR was prompted by a CI failure where the base `material-icons-core` was too narrow for our palette.

## Context

Jetpack Compose ships two icon libraries:

- `material-icons-core` — a few dozen commonly-used icons.
- `material-icons-extended` — the full Material Icons set, ~2000 icons, adds ~250 KB to the APK.

We use `Icons.Outlined.{Apps, Chat, MonitorHeart, CloudDownload, Info, Verified, Policy, Public, ViewInAr, Build}`. Several of those live only in `extended`.

## Decision

Depend on `material-icons-extended`. One line in `app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

## Rationale

- Icon choice is a UX decision; forcing the designer to use only the core set would bloat panels with awkward icon choices OR require shipping vector drawables ourselves (which costs the same APK bytes).
- Extended is first-party; it's not an extra dependency source.
- 250 KB is a rounding error on an app that'll land around 2.5 MB total.

## Consequences

- +: unrestricted icon choice.
- +: future panels don't have to think about which bucket an icon lives in.
- -: 250 KB APK bloat. Acceptable.
