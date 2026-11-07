---
title: "Document the signed-release keystore flow"
labels: [infrastructure, documentation]
---
`release.yml` already supports a signing keystore via four GitHub Secrets. Document the end-to-end flow in `docs/signing.md` so a new maintainer can roll a signed release without reading the YAML.

### Content

- How to generate a keystore locally (`keytool -genkey -v …`).
- How to base64 it and paste into `QUESTCLASSIC_KEYSTORE_B64`.
- The four secret names and what each is.
- Why we sign even debug releases for distribution (so the next update installs over the previous one — Android requires same signer).
- Backup recommendations: once a release is signed and shipped, losing the key means you can never again publish an update that installs over the current one.

### Done when

- [ ] `docs/signing.md` written.
- [ ] A test release works via Actions.
- [ ] README's install section links to it.
