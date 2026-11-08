# ADR 0004 — No telemetry, ever

- **Status**: Accepted
- **Date**: 2026-11-07

## Context

Modern Android apps routinely bundle analytics (Firebase, Mixpanel, Amplitude, Google Analytics), crash reporting (Crashlytics, Sentry), remote config (Firebase Remote Config), and more. Each one phones home.

QuestClassic's entire point is to be a tool its users trust — sitting next to Meta's heavily-telemetered stack, running on a device whose user may have specifically sought us out because they don't want more analytics on their headset.

## Decision

**Zero telemetry.** No analytics SDK. No crash-reporting SDK that phones home by default. No remote config. No "anonymous usage metrics." No A/B testing harness.

The app phones home to exactly two hosts, and only when the user engages with specific features:

| Host | Trigger | Content |
|---|---|---|
| `api.github.com` | Open Updates panel | GET /releases/latest |
| `github.com` | Tap "Install" in Updates | GET the release's APK |

Plus whichever AI provider the user configured, if any. And DNS-lookups + ICMP pings to the 24 Meta hosts if the user opens the Meta Hosts Audit panel — but nothing is POSTed to those.

## Rationale

- Users who install a sideloaded app sometimes do so BECAUSE they want less telemetry.
- Our features don't need analytics to improve — we read PRs and issues.
- Crash reporting we don't have is handled by users reporting bugs with the buzz pattern that fired + a logcat excerpt.
- Trust is cheap to lose and expensive to earn back.

## What's NOT banned

- Opt-in crash reporting (Sentry, Bugsnag, etc.) — some day, if a user explicitly turns it on, and it's clearly disclosed. Not on by default. Not v0.1.0.
- Local logging (our own stuff written to `/data/data/*/files`) — fine; doesn't leave the device.

## Consequences

- +: easy promise to make ("we don't phone home"); easy to verify (grep the manifest for INTERNET-using classes, there aren't many).
- +: smaller APK, faster start, no init-chain surprises.
- +: this ADR exists so no one accidentally adds Firebase without noticing it's a policy break.
- -: we won't have built-in feature-usage data. We'll have to rely on PRs and issues for signal. We accept this.
