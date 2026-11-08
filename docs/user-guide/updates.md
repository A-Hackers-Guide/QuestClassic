# Updates panel

Polls this repo's GitHub Releases for a newer version, offers to download and install the newer APK through Android's standard `PackageInstaller`.

## How it works

```
onOpen:
  GET https://api.github.com/repos/A-Hackers-Guide/QuestClassic/releases/latest
    ← JSON {tag_name, name, assets: [{name, browser_download_url, size}]}
  compare tag_name vs BuildConfig.VERSION_NAME (semver-ish)
  if newer AND release has an .apk asset:
    show "Install" button
  else:
    show "You're on the latest."
```

Tap "Install":

```
download asset → cacheDir/questclassic-update.apk
launch ACTION_VIEW with the FileProvider URI
  → Android PackageInstaller UI
  → user confirms → app updates, re-opens on new version
```

## First install needs one permission

`REQUEST_INSTALL_PACKAGES`. The first time you tap Install, Android pops the system "Allow Install unknown apps" setting; grant it to QuestClassic in Settings → Apps → Install unknown apps → QuestClassic. One-time.

## What's hard-coded vs rewritten at release time

`SelfUpdater.kt` ships with:

```kotlin
const val OWNER = "REPLACE_WITH_OWNER"
const val REPO = "QuestClassic"
```

`release.yml` runs `sed` to replace both with the actual owner/repo at build time. So a fork of this project gets its own update channel for free — it polls its own release feed, not ours.

## Why GitHub, not Meta's OTA pipeline

- We can't sign a Meta OTA. The signing chain is rooted in a key in Meta's HSM; see [reverse-engineering/avb-chain.md](../reverse-engineering/avb-chain.md).
- Even if we could, pushing to Meta's OTA pipeline requires Meta infrastructure.
- Meta's OTAs don't wipe user-installed APKs on A/B updates, so our channel coexists with theirs peacefully. The headset's HorizonOS gets updated by Meta; QuestClassic gets updated by us; neither interferes.

See [ADR 0005](../adr/0005-github-releases-as-update-channel.md) for the full reasoning.

## Semver comparison

`SelfUpdater.compareSemver` strips a leading `v`, splits on `.` and `-`, and compares component-by-component. So `v0.2.0-rc.1 > v0.1.0` and `v1.0.0 > v0.99.0`. Non-numeric components are treated as 0 (e.g. `v1.0.0-alpha` and `v1.0.0-beta` compare equal — we don't currently distinguish). Good enough for v0; [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12) has a note to add a real semver comparator with tests.

## Network footprint

Two hosts in total:

- `api.github.com` — one HTTPS GET per panel open.
- `github.com` — one HTTPS GET when downloading an asset.

No analytics. No ping. No "anonymous usage telemetry." See [security/model.md](../security/model.md) for the complete list of network endpoints the app can reach.

---

_Last reviewed against v0.1.0._
