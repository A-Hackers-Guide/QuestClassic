# ADR 0005 — GitHub Releases as the update channel

- **Status**: Accepted
- **Date**: 2026-11-07

## Context

We need to ship updates to installed users. On a sideloaded Android app we can't go through Google Play, we can't go through Meta's Store (Oculus/Horizon Store requires Meta vendor status), and we can't piggyback on Meta's OTA (requires a key we don't have and never will — see [reverse-engineering/avb-chain.md](../reverse-engineering/avb-chain.md)).

Options:

1. Host the APK on a server we run; the app polls a URL for new versions.
2. Use F-Droid.
3. Use GitHub Releases.
4. Don't ship updates; tell users to manually redownload.

## Decision

**GitHub Releases** as the primary update channel for v0.1.0.

## Rationale

- Free. We'd otherwise need a static server.
- Reliable. GitHub's CDN is what pretty much every piece of open-source infra relies on.
- Discoverable. Users already go to the repo for everything else.
- One `api.github.com/repos/$OWNER/$REPO/releases/latest` call gets us version + asset list in one trip.
- Signing is orthogonal — we can self-sign the APK (see [security/signing.md](../security/signing.md)) and attach it to the release.
- Forks work for free — the CI workflow sed-rewrites owner/repo at release time, so a fork gets its own channel.

## When to revisit

- When a maintainer wants F-Droid listing. Not blocked by us; they can set it up alongside GitHub Releases.
- When GitHub's rate limits start affecting us (unlikely at any imaginable scale for this app).
- If GitHub ever changes its terms in a way that makes this unworkable. We'd mirror.

## Consequences

- +: free, reliable, discoverable, self-service for forks.
- +: no infra to run.
- -: we don't currently publish release signatures in a way that's cryptographically verifiable beyond GitHub's own HTTPS. Signing the APK itself is the fix (tracked in [issue #11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11)).
- -: GitHub may rate-limit or geo-block. We fail-soft: the Updates panel says "couldn't reach" and that's it.
