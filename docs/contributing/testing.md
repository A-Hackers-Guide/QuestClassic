# Testing

v0.1.0 has no formal test suite. First tests land for `PayloadVerifier` (see [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12)).

## What we test today

- **Manual on-device testing.** Every PR gets sideloaded and the touched panel exercised.
- **CI compiles.** `./gradlew :app:assembleDebug` (build.yml) + `:app:assembleRelease` (release.yml) on every push and PR.
- **Signature Verifier** is cross-checked against `tools/verify_payload.py` — if the Kotlin port and the Python reference ever disagree, that's a bug.

## What to test if you add a feature

- **New panel**: open, close, open again (state reset). Rotate (shouldn't apply on Quest; still try on Cuttlefish). Kill the app, reopen.
- **New AI backend**: correct backend selected in the picker. Invalid API key returns a clean error message (not a crash). Valid key + minimal prompt returns a reply.
- **New diagnostic**: fire the condition (unplug Wi-Fi, fill storage, etc.) and verify the pattern fires on the controllers.
- **New widget**: toggle on + off. Verify it stops ticking when off.
- **Anything crypto / parser** (`PayloadVerifier`, `pbdump`, etc.): add fuzz-style tests. Even quick ones.

## Running existing tests (none yet)

```bash
./gradlew :app:testDebugUnitTest
```

Returns success because nothing's there. First PR to add a test should also:

- Enable `testImplementation(libs.junit.jupiter)` or `kotlin.test` in `app/build.gradle.kts`.
- Create `app/src/test/kotlin/...`.
- Run the test locally once.
- Make sure CI picks it up (should Just Work with `testDebugUnitTest`).

## Instrumented tests

Not v0.1.0. Would need Android Test Orchestrator + emulator hosts in CI. Tracked loosely for after v0.2.

---

_Last reviewed against v0.1.0._
