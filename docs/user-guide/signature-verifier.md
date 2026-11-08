# Signature Verifier panel

Pick a Meta OTA .zip from Downloads; the panel reproduces the exact two signature checks the Quest bootloader does at install time, offline and on-device.

## Why

So that any user can confirm, from their own headset, that an OTA archive was really signed by Meta and hasn't been modified. The bootloader does this before applying; we do it on demand so you can verify an archive you got from somewhere other than Meta's official update server.

## What it checks

An AOSP A/B `update_engine` payload (the `payload.bin` inside the OTA) has two independent RSA-2048 + SHA-256 signatures:

1. **Metadata signature** — covers bytes `[0 .. 24 + manifest_size)` (the CrAU header + the DeltaArchiveManifest).
2. **Final payload signature** — covers `[0 .. 24 + manifest_size) ∪ [data_blobs_start .. final_sig_at)` — header + manifest + data blobs. **The metadata signature bytes are skipped** — they aren't persisted to disk during install (AOSP quirk; this trips up most first-time verifiers).

Both signatures are RSA-2048 over a PKCS#1 v1.5 + DigestInfo-wrapped SHA-256. Verified with Java's `Signature.getInstance("NONEwithRSA")` against the public key extracted from the OTA's own `META-INF/com/android/otacert`.

## How to use

1. Tap **Pick OTA**.
2. Android's file picker opens — navigate to Downloads, pick the `.zip`.
3. The app reads just `payload.bin` and `otacert.pem` from the ZIP (streamed — no full extraction even on a 1 GB payload), computes the two digests, verifies.
4. Result card tells you valid/invalid for each signature plus:
   - Payload header values (size, manifest size, metadata-sig size, final-sig offset)
   - Both SHA-256 digests (so you can compare against a separate reference, e.g. `tools/verify_payload.py`)
   - OTA cert subject, issuer, not-after, SHA-256 fingerprint

## What a passing verify proves

- The archive was signed by the holder of the private key that pairs with the public cert in the archive.
- Not a single byte of `payload.bin` has been modified since the signer signed it.

It does NOT prove:

- That the signer is actually Meta (unless you compare the cert fingerprint against Meta's known one — the canonical SHA-256 is `B8:C9:D2:8A:61:6F:83:14:85:C2:3F:13:6D:AB:91:B0:91:C9:9A:A0:3D:CA:57:08:1A:25:D1:4F:3B:C1:9F:A1`, matching `CN=AOSP_Release_Pub, O=Oculus`).
- That the OTA will install on your device (anti-rollback / device allow-list are separate checks the bootloader makes).

## Native port of the Python tool

This panel is a straight Kotlin reimplementation of `tools/verify_payload.py`. The Kotlin version is behaviorally identical — same byte ranges, same PKCS#1 v1.5 handling, same manifest-skipping trick for the final signature — and shares zero code with the Python version (the Python is pure stdlib; Kotlin uses Java crypto).

If a verification mysteriously succeeds on one and fails on the other, that's a bug; open an issue with both outputs attached.

## Related

- [reference/api/core.md § PayloadVerifier](../reference/api/core.md#payloadverifier) — the Kotlin class.
- `tools/verify_payload.py` — the Python reference.
- [reverse-engineering/ota-format.md](../reverse-engineering/ota-format.md) — background on the CrAU format.
- [reverse-engineering/avb-chain.md](../reverse-engineering/avb-chain.md) — how this fits into the Verified Boot chain.

---

_Last reviewed against v0.1.0._
