# Known security limits

Things QuestClassic deliberately does NOT protect against, in one place so you can see them all at once.

## API keys are at rest unencrypted in app-private storage

- The AI Companion's API key lives in `DataStore (Preferences)` at `/data/data/io.github.questclassic.app/datastore/ai_config.preferences_pb`.
- On an **unrooted** headset, app-private storage is unreadable to other apps — the Android sandbox + SELinux enforce this.
- On a **rooted** headset, anything with root can read it.
- We don't use Android Keystore-backed encryption at rest yet. Tracked for v0.2 (see [issue #11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11) peripherally).

**Mitigation**: don't use QuestClassic on a rooted headset you don't trust.

## No certificate pinning

- All HTTPS calls trust the Android system CA store.
- If that store is compromised (malicious MDM, user-installed MITM cert accepted, etc.), traffic can be intercepted.
- Pinning our two hosts (`api.github.com` + `github.com`) is a reasonable future upgrade; pinning the AI backends is not, because users pick which backend they're hitting.

**Mitigation**: don't accept user-installed CAs on headsets you're serious about.

## Our release APK is only sometimes signed

- v0.1.0 ships as `app-release-unsigned.apk` — it installs via ADB but is only debug-signed.
- Once the signing keystore is provisioned in GitHub Secrets per [signing.md](signing.md), releases will be signed with a persistent release key.
- Mixing signed and unsigned installs breaks update continuity: if you install the unsigned v0.1.0 and then try to install the signed v0.2.0, Android refuses (different signer).

**Mitigation**: once v0.2.0 ships signed, uninstall the unsigned v0.1.0 cleanly before installing.

## The Self-Updater trusts GitHub

- We poll `api.github.com` for the latest release and install whatever APK is attached.
- If an attacker compromised our GitHub account they could push a malicious APK. Mitigation on that side: 2FA on the owner account + branch protection.
- If GitHub itself were compromised, everyone's trust chain breaks, not just ours.

**Mitigation**: verify the SHA-256 of the APK against the release page if you're paranoid.

## No in-app secrets scrubbing

- The AI Companion sends your system prompt and chat history to the configured backend. If you type a secret into the chat, that secret flows to the backend.
- No scrubbing step. No "oh I didn't mean to send that."

**Mitigation**: don't type secrets into chat. Use the Ollama backend for sensitive conversations.

## System-property reads are necessarily blabby

- The System Internals panel reads `ro.*` properties widely. Anything readable by `adb shell getprop` is readable by us.
- Some of those (like the internal Meta build-host hostname) are information another process on the same headset could also read.

**Mitigation**: this is an Android-platform limitation; the whole point of `ro.*` properties is readable-by-anyone.

## The Signature Verifier only verifies what Meta put in the OTA

- We check that the OTA was signed by the holder of the cert IN the archive.
- We don't verify the cert against a hardcoded trusted-root fingerprint.
- An attacker could craft a self-signed OTA with their own cert and we'd call it "valid" (meaning "cert in archive signed the payload").

**Mitigation**: compare the cert's SHA-256 fingerprint against Meta's canonical one: `B8:C9:D2:8A:61:6F:83:14:85:C2:3F:13:6D:AB:91:B0:91:C9:9A:A0:3D:CA:57:08:1A:25:D1:4F:3B:C1:9F:A1`. We display the fingerprint in the result card precisely so you can do this.

**Fix tracked**: add an optional "strict mode" that refuses to call a signature valid unless the cert matches a known-good fingerprint. Good first-ish issue.

---

_Last reviewed against v0.1.0._
