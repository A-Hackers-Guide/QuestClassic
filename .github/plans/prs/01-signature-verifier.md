## What

Adds a **Signature Verifier** panel that does, natively in the app, what `tools/verify_payload.py` does from the command line: given a Meta OTA .zip the user picks via Android's system file picker, it reproduces the two RSA-2048+SHA-256 signature checks the Quest bootloader performs at install time, and shows the result on screen.

## Why

It closes the "proof loop" started in the parent project's reverse-engineering report (sections 15 and 19). Any user can now hand the app an OTA zip they downloaded from Meta and confirm, with their own device's cert as the trust anchor, that it was really signed by Meta and hasn't been modified in flight.

## How

- `core/PayloadVerifier.kt` — zero-dep (stdlib + Java crypto) port of `tools/verify_payload.py`. Pure-Kotlin implementation of the Signatures protobuf parser and the "digest over `[0..24+manifest_size) ∪ [data_blobs_start..final_sig_at)`" arrangement that trips everyone up the first time they try to verify an AOSP OTA.
- `ui/panels/SignatureVerifierPanel.kt` — Compose UI that picks the .zip via `ActivityResultContracts.OpenDocument`, shows the per-field result (payload header, digests, cert subject/issuer/fingerprint, both signatures' valid/invalid).
- Wiring in `AppContainer`, `NostalgicHomeScreen`.

## Test plan

- Run `./gradlew :app:assembleDebug`.
- Sideload, open "Signature Verifier", pick a Meta OTA .zip from Downloads.
- Expected: both signatures report "✓ valid"; the cert subject shows `CN=AOSP_Release_Pub, O=Oculus`; the SHA-256 fingerprint matches the one our tools printed.

## Related

- `tools/verify_payload.py` — the reference implementation this port mirrors.
- Parent project's `REVERSE_ENGINEERING_REPORT.md` §15 (signature bundle) and §19 (auth flow) for the AOSP-specific digest-range arcana.
