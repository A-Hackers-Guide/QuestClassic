# Reverse-engineering notes — overview

QuestClassic was designed after a full reverse-engineering pass on a shipped Meta Quest 2 OTA. These docs summarise what that pass found. The parent project's `REVERSE_ENGINEERING_REPORT.md` has the full unabridged version; these pages distill the parts that directly informed the app.

## The archive

`q2_52322490022300150.zip` — a 1.1 GB signed A/B OTA for Meta Quest 2 (codename `hollywood`) running Android 14 base build `UP1A.231005.007.A1`, HorizonOS release `releases-oculus-14.0-v209`, security patch `2026-09-06`.

## Headline findings

Each is linked to the panel that surfaces it live, where possible.

- **AVB chain is complete and valid.** We re-verified the two RSA-2048+SHA-256 signatures against the OTA's own cert; both pass. [reverse-engineering/avb-chain.md](avb-chain.md). Surfaced in [Signature Verifier panel](../user-guide/signature-verifier.md).
- **Meta's device-auth is a TrustZone-signed challenge/response.** The key never leaves QSEE; the device proves possession to `graph.facebook-hardware.com` without ever exposing the key to Android. [reverse-engineering/meta-auth-flow.md](meta-auth-flow.md).
- **24 Meta hosts** across Auth / GraphQL / GenAI / Payments / CDN / Internal. [reverse-engineering/hosts-glossary.md](hosts-glossary.md). Surfaced in [Meta Hosts Audit panel](../user-guide/meta-hosts-audit.md).
- **Vendor SPL is 8 years stale** (`2018-06-05`). Documented in [security/firmware-review.md § F-01](../security/firmware-review.md#f-01--vendor-partition-security-patch-level-trails-device-wide-by-8-years---medium). Surfaced in [System Internals panel](../user-guide/system-internals.md).
- **Internal Meta build-host leaked** into partition fingerprints. [security/firmware-review.md § F-03](../security/firmware-review.md#f-03--meta-internal-build-host-hostname-leaks-into-fingerprints---low). Surfaced in System Internals.
- **dm-verity uses SHA-1** on the five dynamic partitions. [security/firmware-review.md § F-02](../security/firmware-review.md#f-02--dm-verity-hashtrees-use-sha-1---low-medium).
- **10+ Meta Phabricator task IDs** retained in `libshell.so` strings. [security/firmware-review.md § F-04](../security/firmware-review.md#f-04--10-meta-phabricator-task-ids-retained-in-libshellso-strings---low).
- **HorizonOS has a visible service tree** — `orchestrator` is the binary that runs the whole thing; 20+ daemons under it. Overview in the parent RE report.

## What we used

- `unzip -l`, `unzip -p` for peeking.
- Custom zero-dep Python protobuf parsers (`tools/pbdump.py`, `tools/payload_summary.py`, `tools/payload_extract.py`, `tools/inspect_images.py`).
- `debugfs` (`brew install e2fsprogs`) to mount ext4 partitions read-only.
- `jadx` for APK → Java decompile.
- `objdump`, `nm`, `strings`, `openssl x509` for native + crypto inspection.
- `apktool` for binary AndroidManifest decoding.

All five Python tools ship in `tools/` of this repo, MIT-licensed. Use them against your own OTAs.

## What we did NOT do

- Not published Meta firmware images or binaries to this repo. See the [README's "Legal / IP posture"](https://github.com/A-Hackers-Guide/QuestClassic/blob/main/README.md#legal--ip-posture).
- Not developed working exploits.
- Not decompiled every Meta APK — just the Meta-proprietary ones that were relevant to understanding the system (OSUpdater, DeviceAuthServer, IdentityManagement, VrShell, Guardian, …).
- Not fuzzed live Meta firmware.

## Why this is in the app's docs

Because the panels we shipped (Signature Verifier, System Internals, Meta Hosts Audit) are direct products of this RE pass. Users who want to know *why* the app shows what it shows, follow the links from each panel's user-guide page to the matching RE note.

---

_Last reviewed Nov 2026._
