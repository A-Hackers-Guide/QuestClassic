# Meta Hosts Audit panel

Resolves + reachability-pings all 24 Meta hosts we identified during the OTA reverse-engineering, grouped into six categories. Educational (shows how Meta's identity / graph / payments fabric stretches across its properties), operational (quick way to spot which Meta service is down), and research (the "Internal" category shouldn't resolve from a consumer device — a surprise there would be investigation-worthy).

## Categories

### AUTH (3)
- `accountscenter.meta.com` — the Meta Accounts Center PWA the AccountsCenter app on-device loads in a WebView.
- `secure.oculus.com` — Oculus login endpoint.
- `graph.facebook-hardware.com` — the device-auth endpoint (challenge/response, hardware-backed; see [reverse-engineering/meta-auth-flow.md](../reverse-engineering/meta-auth-flow.md)).

### GRAPH (7)
Primary GraphQL fabric Meta's apps hit for user data.
- `graph.oculus.com` — Oculus GraphQL (primary).
- `graph.facebook.com` — Facebook main GraphQL — yes, the Oculus apps call it directly, because post-acquisition the account graph is unified.
- `graph.instagram.com` — Instagram GraphQL — only interesting when your Meta account is linked to IG.
- `b-graph.facebook.com` — Facebook bulk GraphQL.
- `meta.graph.meta.com` — Meta-branded GraphQL.
- `graph-fallback.oculus.com` — Oculus GraphQL fallback.
- `realitylabs-graph.oculus.com` — Reality Labs GraphQL.

### GENAI (2)
Meta AI endpoints, baked into retail code paths.
- `genai-graph.facebook.com`
- `genai-graph.instagram.com`

### PAYMENTS (5)
Meta unified-payments GraphQL — same URL template across all Meta properties.
- `payments-graph.oculus.com`
- `payments-graph.facebook.com`
- `payments-graph.instagram.com`
- `payments-graph.whatsapp.com`
- `payments-graph.graph.meta.com`

### CDN (3)
- `scontent.xx.fbcdn.net` — Facebook CDN.
- `securecdn.oculus.com` — Oculus CDN.
- `rupload.facebook.com` — resumable upload.

### INTERNAL (3)
Hosts that appear in the Store/Shell binaries but shouldn't resolve from a consumer device. If one of these ever resolves on YOUR headset, it's worth investigating — it means either Meta's DNS misconfigured, or you're on a Meta-internal network.
- `graph.intern.facebook.com`
- `m.intern.facebook.com`
- `our.intern.facebook.com`

## How probing works

```
InetAddress.getByName(host)          ← DNS resolution, short timeout
addr.isReachable(1500ms)             ← ICMP ping or TCP-connect probe
```

Both run in parallel for all 24 hosts (coroutines + `async`/`awaitAll`). Takes about 2 seconds total on a decent Wi-Fi.

## Reading the results

Each host card shows:

- **IP address** (if DNS resolved).
- **Latency** (if the ping succeeded — typical ~50-200 ms from US Wi-Fi).
- **"unreachable"** (DNS resolved, ping failed — common on networks that block ICMP; the host may still be TCP-443 reachable).
- **DNS fail / error message** (if resolution itself failed — expected for `*.intern.facebook.com`).

The top of each group shows `X/Y reachable`.

## Privacy

The panel only **resolves and pings**. Meta sees the DNS query (at your DNS resolver, not at Meta's edge unless they operate it) and an ICMP echo. No authentication, no account identifier, nothing from your user profile leaves the device via this panel.

## Why 24 hosts specifically

Harvested from the DEX strings of every Meta-online APK and from `libshell.so`'s `.rodata` section during the reverse-engineering (see [reverse-engineering/hosts-glossary.md](../reverse-engineering/hosts-glossary.md)). If Meta adds more endpoints in future builds, we'll learn them from a new OTA analysis and extend the list.

---

_Last reviewed against v0.1.0 + the 2026-09-06 Quest 2 OTA._
