---
title: "Add unit tests for PayloadVerifier"
labels: [testing]
---
`PayloadVerifier` is the one piece of the app where "silently wrong" would be really bad (we tell users "signatures valid" — we'd better be sure). It's also pure-ish: it's a function of a `payload.bin` + an `otacert.pem` to a `Report`.

### Needed

- `app/src/test/kotlin/io/github/questclassic/app/core/PayloadVerifierTest.kt`.
- Test fixtures:
  - A known-good tiny OTA synthesized by our Python tools (we can generate one on the fly in CI with `tools/`).
  - A tampered version (flip one bit in the data section); expect `finalSignatureValid = false`, metadata still valid.
  - A tampered metadata version; expect `metadataSignatureValid = false`.

### Done when

- [ ] Test added; green on CI.
- [ ] Snapshot fixture .zip under `app/src/test/resources/` (small — synthesized, not pulled from Meta).
