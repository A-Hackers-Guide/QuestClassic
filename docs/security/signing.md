# Signing

How to set up a persistent release signing key so our APKs install over each other across versions.

## Why this matters

Android identifies an app by `applicationId` + signing-certificate. Two APKs with the same `applicationId` but different signers refuse to install over each other — you have to uninstall first, which wipes user data.

For QuestClassic to self-update smoothly across versions:

- Every release APK must be signed with the same key.
- We publish the key's SHA-256 fingerprint so users can verify a given APK was really signed by us.

v0.1.0 ships `app-release-unsigned.apk` (debug-signed by Gradle); this doc is the plan for getting to signed releases.

## Generating the keystore

On a trusted workstation (not CI):

```bash
keytool -genkey -v \
  -keystore questclassic-release.jks \
  -keyalg RSA -keysize 4096 -validity 36500 \
  -alias questclassic \
  -storepass '<a strong passphrase>' \
  -keypass '<ideally the same passphrase>' \
  -dname "CN=QuestClassic Releases, O=A-Hackers-Guide, L=, ST=, C="
```

- **`-keysize 4096`** — overkill but cheap.
- **`-validity 36500`** — 100 years. Once an app is signed with this key, you must keep the key for the life of every install. Losing it means forking the app.
- **Store the keystore offline**, encrypted. Two backups minimum (e.g. Bitwarden + a USB in a drawer).

## Uploading to GitHub

Four GitHub Secrets on the repo (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `QUESTCLASSIC_KEYSTORE_B64` | `base64 < questclassic-release.jks` output |
| `QUESTCLASSIC_KEYSTORE_PASSWORD` | the passphrase |
| `QUESTCLASSIC_KEY_ALIAS` | `questclassic` |
| `QUESTCLASSIC_KEY_PASSWORD` | the key passphrase (same as store, usually) |

Encode locally:

```bash
base64 < questclassic-release.jks | pbcopy       # macOS
# or:
base64 -w0 < questclassic-release.jks            # Linux
```

Paste into the `QUESTCLASSIC_KEYSTORE_B64` secret.

## What `release.yml` does with them

```yaml
- name: Decode signing keystore (optional)
  if: ${{ env.KEYSTORE_B64 != '' }}
  env:
    KEYSTORE_B64: ${{ secrets.QUESTCLASSIC_KEYSTORE_B64 }}
  run: |
    echo "$KEYSTORE_B64" | base64 -d > release.jks
    echo "QUESTCLASSIC_KEYSTORE=$(pwd)/release.jks" >> $GITHUB_ENV

- name: Build release APK
  env:
    QUESTCLASSIC_KEYSTORE_PASSWORD: ${{ secrets.QUESTCLASSIC_KEYSTORE_PASSWORD }}
    QUESTCLASSIC_KEY_ALIAS: ${{ secrets.QUESTCLASSIC_KEY_ALIAS }}
    QUESTCLASSIC_KEY_PASSWORD: ${{ secrets.QUESTCLASSIC_KEY_PASSWORD }}
  run: ./gradlew :app:assembleRelease
```

`app/build.gradle.kts` wires these into `signingConfigs`:

```kotlin
signingConfigs {
    create("release") {
        val keystoreFile = System.getenv("QUESTCLASSIC_KEYSTORE")
        if (keystoreFile != null) {
            storeFile = file(keystoreFile)
            storePassword = System.getenv("QUESTCLASSIC_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("QUESTCLASSIC_KEY_ALIAS")
            keyPassword = System.getenv("QUESTCLASSIC_KEY_PASSWORD")
        }
    }
}
```

If any secret is missing, the build falls back to debug-signed. That's what v0.1.0 did.

## Publishing the fingerprint

Once you've signed a release, publish its fingerprint so users can verify:

```bash
keytool -list -v -keystore questclassic-release.jks \
  -storepass '<passphrase>' \
  -alias questclassic | grep SHA-256
```

Put the fingerprint in the repo's README, in the release notes, and in `docs/security/signing.md` (this doc).

## If you lose the keystore

- Fork the app (new `applicationId`), start fresh.
- Everyone who had the old one must uninstall before they can install the new one.
- Communicate loudly in the release notes.

This is a non-recoverable situation. Make backups.

## Related

- [Issue #11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11) — track completing this setup for v0.2.
- [reference/build-flavors.md](../reference/build-flavors.md) — how debug vs release differ.

---

_Last reviewed against v0.1.0._
