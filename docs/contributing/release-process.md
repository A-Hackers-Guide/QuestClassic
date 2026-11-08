# Release process

How we cut a version. Takes about 5 minutes.

## Prerequisites

- You're on `main`, up-to-date, clean.
- CI on `main`'s latest commit is green.
- `CHANGELOG.md` has a "Unreleased" section with the changes you're about to ship.

## Steps

### 1. Decide the version

Semver-adjacent:

- **MAJOR** (`v1.0.0`) — breaking change the user has to deal with (keystore change, removed feature, etc.).
- **MINOR** (`v0.2.0`) — new features, no breaking changes.
- **PATCH** (`v0.1.1`) — bug fixes only.

Pre-release: `v0.2.0-rc.1`. The CI's `prerelease: contains(ref_name, '-')` marks these correctly.

### 2. Promote Unreleased → new version in CHANGELOG

```markdown
## [0.2.0] - 2026-12-01
### Added
- Gemini AI backend (#17)
- Free memory widget (#8)
...
```

Commit:

```bash
git commit -am "chore: release v0.2.0"
git push origin main
```

### 3. Tag

```bash
git tag v0.2.0 -m "QuestClassic v0.2.0"
git push origin v0.2.0
```

### 4. Watch CI

`release.yml` fires on tag push. It:

1. Checks out the tagged commit.
2. `sed`s the self-updater to point at the right repo.
3. Decodes the signing keystore (if `QUESTCLASSIC_KEYSTORE_B64` secret is set).
4. Runs `./gradlew :app:assembleRelease`.
5. Creates a GitHub Release with auto-generated notes.
6. Attaches `app/build/outputs/apk/release/*.apk`.

Typical runtime: 2-3 minutes.

### 5. Verify

- Open the release on GitHub, confirm the APK is attached.
- Download it, SHA-256 it, note the fingerprint.
- Install on your headset (`adb install -r -d app-release.apk`).
- Open the Updates panel and verify "You're on the latest."

### 6. Announce

- Pin the release note in the project Discussions, if you've enabled them.
- Update any downstream docs that reference a version.

## If something's wrong

- **CI failed**: fix on `main`, delete the bad tag locally and remotely, re-tag:
  ```bash
  git tag -d v0.2.0
  git push origin :refs/tags/v0.2.0
  gh release delete v0.2.0 --yes
  # fix, commit, push
  git tag v0.2.0 && git push origin v0.2.0
  ```
- **APK attached but broken**: don't try to patch in place. Cut `v0.2.1`.

## When to cut a prerelease

- You want to test the release pipeline itself (e.g. after changing release.yml).
- You're about to ship a big feature and want testers first.
- Tag it `v0.2.0-rc.1`; the CI marks it `prerelease: true`.

The Updates panel ignores prereleases by default — if a `-` appears in the tag, `isNewer` still flags it but the release itself is marked prerelease on GitHub, which by convention consumers filter out.

---

_Last reviewed against v0.1.0._
