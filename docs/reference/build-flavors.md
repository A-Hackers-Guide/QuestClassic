# Build flavors & signing

QuestClassic has one Gradle module and two build types.

## Build types

### `debug`

- `applicationIdSuffix = ".debug"` — so debug and release can coexist on the same device.
- `isMinifyEnabled = false`.
- Signed with Android's debug keystore (`~/.android/debug.keystore`).
- Used for `./gradlew :app:assembleDebug` and the CI build workflow.

### `release`

- `applicationId = io.github.questclassic.app` (no suffix).
- `isMinifyEnabled = true` + `isShrinkResources = true`.
- ProGuard rules at `app/proguard-rules.pro` keep the AI backend classes and GitHub release types reflectable.
- Signed with the release keystore ONLY if the four `QUESTCLASSIC_KEYSTORE*` environment variables are set. Otherwise falls back to debug-signed.

## Signing

See [security/signing.md](../security/signing.md) for the full flow. The four env vars:

| Env var | Source in CI |
|---|---|
| `QUESTCLASSIC_KEYSTORE` | Path written by the "Decode signing keystore" step |
| `QUESTCLASSIC_KEYSTORE_PASSWORD` | `secrets.QUESTCLASSIC_KEYSTORE_PASSWORD` |
| `QUESTCLASSIC_KEY_ALIAS` | `secrets.QUESTCLASSIC_KEY_ALIAS` |
| `QUESTCLASSIC_KEY_PASSWORD` | `secrets.QUESTCLASSIC_KEY_PASSWORD` |

## Why no flavors

We don't ship `free` vs `premium`, `prod` vs `staging`, `openSource` vs `proprietary`, or any other axis. One flavor keeps the mental model simple and the APK count small.

If v0.2 ships a developer build (unlocked logging, extra panels) it'll be a flavor with its own `applicationIdSuffix`, e.g. `io.github.questclassic.app.dev`.

---

_Last reviewed against v0.1.0._
