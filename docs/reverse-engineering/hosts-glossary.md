# Meta hosts glossary

Every Meta-owned host we identified during the OTA reverse-engineering, what it does, and where it showed up.

## AUTH (3)

| Host | Role | Found in |
|---|---|---|
| `accountscenter.meta.com` | Meta Accounts Center PWA (loaded in WebView on-device) | AccountsCenterPWA.apk |
| `secure.oculus.com` | Oculus login | DEX strings, multiple apps |
| `graph.facebook-hardware.com` | Device-auth challenge/response | DeviceAuthServer.apk — only URL present there |

## GRAPH (7)

Primary GraphQL fabric.

| Host | Found in |
|---|---|
| `graph.oculus.com` | Every Meta-online APK's DEX strings |
| `graph.facebook.com` | DEX strings in Store, SocialPlatform, OCMS, VrShell's libshell.so |
| `graph.instagram.com` | SocialPlatform, Store (cross-linked account queries) |
| `b-graph.facebook.com` | SocialPlatform (bulk GraphQL) |
| `meta.graph.meta.com` | OCMS, newer Meta-branded alias |
| `graph-fallback.oculus.com` | OCMS, SocialPlatform |
| `realitylabs-graph.oculus.com` | Store |

## GENAI (2)

Meta AI endpoints, baked into retail code paths.

| Host | Found in |
|---|---|
| `genai-graph.facebook.com` | libshell.so strings |
| `genai-graph.instagram.com` | libshell.so strings |

## PAYMENTS (5)

Meta unified-billing GraphQL — same URL template across all Meta properties.

| Host | Found in |
|---|---|
| `payments-graph.oculus.com` | Store |
| `payments-graph.facebook.com` | Store |
| `payments-graph.instagram.com` | Store |
| `payments-graph.whatsapp.com` | Store (!) |
| `payments-graph.graph.meta.com` | Store |

## CDN (3)

| Host | Found in |
|---|---|
| `scontent.xx.fbcdn.net` | Every Meta-online APK |
| `securecdn.oculus.com` | OCMS |
| `rupload.facebook.com` | Resumable upload (SocialPlatform) |

## Developer + support (6, not probed in-app)

| Host | Role |
|---|---|
| `developer.oculus.com` | Dev portal |
| `developers.facebook.com` | Facebook dev portal |
| `developers.meta.com` | Meta dev portal |
| `forwork.meta.com`, `work.meta.com` | Meta for Work (enterprise Quest) |
| `support.oculus.com` | Support |
| `horizon.meta.com` | Horizon web portal |

## INTERNAL (4) — SHOULD NOT resolve from a consumer device

If one of these ever resolves on YOUR headset, worth investigating.

| Host | Role |
|---|---|
| `graph.intern.facebook.com` | Internal FB graph |
| `m.intern.facebook.com` | Internal FB mobile |
| `our.intern.facebook.com` | "Our" internal portal |
| `shortwave.facebook.com` | Internal Facebook service (unknown precise role) |

Plus the dev/staging URL schemas baked into `libshell.so`:

- `graph.<cluster>.sb.oculus.com/…` — sandbox
- `graph.<cluster>.svcscm.od<N>.<region>.oculus.com/…` — ServiceSCM / Odyssey datacenters
- `graph.<env>.(dev|sandcastle|devvm)<N>.<region>.oculus.com/…` — dev / staging / devvm

These are regex templates in `.rodata`, not static hostnames. They expand at runtime with build-flavor variables.

## How to see them live

- The on-device **Meta Hosts Audit panel** resolves + pings the 24 we considered highest-signal.
- The full list above is a superset — the audit panel can be extended via a PR against `core/MetaHostsAudit.kt`.

## Why this inventory matters

Three audiences:

1. **Users** — "where does my headset phone home?" Visible map.
2. **Operators** — quick way to tell which Meta service is down.
3. **Researchers** — surprises in the Internal category are investigation-worthy; the sandbox URL schemas are a reminder that retail firmware carries dev-flavor URL templates.

---

_Last reviewed against the Nov 2026 Quest 2 OTA._
