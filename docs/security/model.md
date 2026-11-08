# Threat model

Who could hurt whom through QuestClassic, and what we do (and don't) defend against.

## Assets we care about

1. **Your AI-provider API key** — stored in on-device DataStore, excluded from Android cloud backup. We never send it anywhere except to the backend you configured.
2. **The headset's identity** — build fingerprints, device model, HorizonOS branch. Readable via public Android APIs; not sensitive in itself, but a user may not want it broadcast.
3. **The signatures we display** — users relying on our Signature Verifier panel to confirm a Meta OTA is legitimate before they install it elsewhere.
4. **The APK integrity** — our own release APK being what you think it is.

## Adversaries and what they could try

### 1. A malicious app installed alongside QuestClassic on the same headset

- Could try to **read our DataStore**. Mitigation: Android's per-app storage isolation. They can't; stock Android enforces this at the filesystem level on top of SELinux.
- Could try to **intercept our network calls**. Mitigation: HTTPS everywhere, certificate pinning would be an upgrade (see [issue tracker](https://github.com/A-Hackers-Guide/QuestClassic/issues) for a future PR).
- Could **spoof a Signature Verifier report** by shipping a lookalike app and tricking you. Mitigation: you install us; nothing can replace a running APK with another one without your consent.

### 2. Someone with physical access to the headset

- Can read your DataStore if they root the device (which requires an exploit; see [known-limits.md](known-limits.md)).
- Can replace our APK with a trojaned version if they install via ADB. Mitigation: once we start signing releases with a persistent key, Android refuses to install a differently-signed APK over ours.

### 3. A network adversary on your Wi-Fi

- Can see that you're talking to `api.github.com`, `api.anthropic.com`, etc. (TLS SNI reveals the host.)
- Cannot see request contents — TLS.
- Cannot inject responses — TLS.
- Can block us. Mitigation: we fail-soft (update panel says "couldn't reach", chat says "error").

### 4. A hostile Meta OTA archive handed to the Signature Verifier

- A crafted .zip could try to exploit our zip parser or our varint parser.
- Mitigation: `java.util.zip.ZipInputStream` is battle-tested; our varint parser is <40 lines of code, reviewed, and handles length/overflow checks.
- Worst case: a crash in the verify panel. The app's state is preserved; nothing escalates outside the panel.

### 5. A compromised AI provider

- Could serve responses that trick you into doing something.
- Mitigation: this is a general LLM risk, not ours to solve. Don't put secrets in your system prompt.

## What we actively do

- **Always HTTPS.** Every network call.
- **No telemetry.** We don't know when you use the app or what you do with it.
- **No shared backends.** No service of ours; only hosts you supply (AI backend) or GitHub for our own updates.
- **Minimum permissions.** [reference/permissions.md](../reference/permissions.md) — not a long list.
- **Signature Verifier is deterministic.** Same input, same output, no network touched.
- **Exclude sensitive files from Android cloud backup.** `data_extraction_rules.xml`.
- **FileProvider for the one file we hand to the system installer.** Scoped grant, read-only, our update APK only.

## What we explicitly don't defend against

- **A rooted headset.** If someone has root, your DataStore is readable. (On-device encryption with a user passphrase is tracked as a possible v0.2 feature.)
- **A malicious network path that MITMs TLS** — if your CA store is poisoned, we lose. We don't do certificate pinning yet.
- **An unsigned build being tampered with before install.** If you ADB-install a QuestClassic APK that someone else modified, we have no way to tell. Verify against our GitHub release SHA-256 if in doubt.
- **A compromised AI provider.** Not our scope.

## Network endpoints

Full list of hosts the app can reach, grouped by panel:

| Panel | Hosts |
|---|---|
| Updates | `api.github.com`, `github.com` |
| AI Companion — Claude | `api.anthropic.com` |
| AI Companion — OpenAI | `api.openai.com` |
| AI Companion — Ollama | `<your LAN address>` |
| Meta Hosts Audit | Resolves + ICMP-pings the 24 hosts in [reverse-engineering/hosts-glossary.md](../reverse-engineering/hosts-glossary.md). Does NOT auth, POST, or send any data. |

That's all of it. No analytics. No ping. No "anonymous usage telemetry."

## Reporting a vulnerability

See [reporting.md](reporting.md).

---

_Last reviewed against v0.1.0._
