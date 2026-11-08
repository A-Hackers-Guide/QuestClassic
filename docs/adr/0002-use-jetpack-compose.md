# ADR 0002 — Use Jetpack Compose for all UI

- **Status**: Accepted
- **Date**: 2026-11-07

## Context

We're building a 2D panel app for Quest that runs as a sideloaded APK. Options:

1. Classic Android Views (XML layouts + `View`/`ViewGroup`).
2. Jetpack Compose.
3. Unity (overkill — this is a 2D panel app, not a VR game).
4. Flutter (great cross-platform story we don't need; adds a Dart runtime).
5. React Native (adds a JS runtime + bridge; complexity we don't need).

## Decision

**Jetpack Compose** for 100% of UI. No XML layouts, no Views, no Fragments beyond what Compose uses internally.

## Rationale

- First-party Android UI toolkit, actively developed.
- The declarative model fits our "single `HomeScreen` enum + `when` dispatch" navigation perfectly.
- Interop with the Android platform is excellent — Activities, Intents, PackageManager work exactly the same.
- No bridge / no runtime beyond what we already need (Kotlin stdlib + coroutines).
- The nostalgic Oculus look is easy to implement with Compose primitives (gradients, Canvas, custom shapes) and would be painful to retrofit from XML.

## Consequences

- +: fast UI iteration, hot-reload via Compose Preview when we want it.
- +: smaller APK than Compose + XML + Views stack.
- +: Compose Multiplatform story (if we ever want desktop/iOS) is open.
- -: Compose has a learning curve for Android developers who've only done XML.
- -: Some rare Android widgets don't have Compose equivalents yet (we haven't hit one).
