## What

Adds a **Meta Hosts Audit** panel that resolves + reachability-pings every Meta-owned host this build is wired to talk to, grouped into six categories (Auth / GraphQL / GenAI / Payments / CDN / Internal). All 24 hosts came out of the reverse-engineering (DEX strings + `libshell.so` strings).

## Why

Three audiences:

1. **Educational**: visible map of how Meta's identity / graph / payments fabric stretches across Facebook / Instagram / WhatsApp / Oculus.
2. **Operational**: quick way to see which Meta service is up/down from your network when the Store misbehaves.
3. **Research**: the Internal category (`*.intern.facebook.com`) *shouldn't* resolve from a consumer device; if it ever does, that's a signal worth investigating.

## How

- `core/MetaHostsAudit.kt` — host list + categories + probe fn (`InetAddress.getByName` + `isReachable`). Zero deps.
- `ui/panels/MetaHostsAuditPanel.kt` — Compose UI, groups by category, color-codes per group, shows count of reachable per group.

## Test plan

- Build + sideload.
- Open on a Quest with internet.
- Expected: AUTH / GRAPH / GENAI / PAYMENTS / CDN hosts resolve and most are reachable (ICMP + TCP port-443 on Meta edges usually responds).
- INTERNAL hosts: DNS fail (public resolvers don't know those names), which is the correct behavior.

## Privacy note

The panel only *resolves and pings* — it doesn't send any data to these endpoints. Meta sees the DNS query (at your resolver) and an ICMP echo. No account/session info leaves the device from this panel.
