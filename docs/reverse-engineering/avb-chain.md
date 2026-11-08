# AVB chain of trust

Android Verified Boot 2.0 as it runs on Meta Quest 2, end to end.

## The chain

```
Qualcomm on-die fuses (Meta OEM fuse set)
  └─ PBL verifies XBL
      └─ XBL verifies ABL (signed by `abl_signing_keys:release`)
          └─ ABL loads `boot.img` and `vbmeta`
              └─ vbmeta verifies:
                  ├─ boot.img                (hash descriptor)
                  ├─ recovery.img            (hash descriptor)
                  ├─ dtbo.img                (hash descriptor)
                  ├─ chain → vbmeta_system   (rollback_index_location=1)
                  │   ├─ hashtree(system)
                  │   ├─ hashtree(system_ext)
                  │   └─ hashtree(product)
                  └─ chain → vbmeta_vendor   (rollback_index_location=2)
                      ├─ hashtree(vendor)
                      └─ hashtree(odm)
```

Every signature is RSA-2048+SHA-256 against Meta's `AOSP_Release_Pub` cert (or a key chained from it). The cert is in `META-INF/com/android/otacert` in every OTA; its fingerprint is burned into the device during manufacturing.

## The root cert

- Subject: `CN=AOSP_Release_Pub, O=Oculus, OU=Miramar, L=Menlo Park, ST=CA, C=US, emailAddress=security@oculus.com`.
- Issuer: `CN=AOSP_Int_Pub` (Meta's internal intermediate CA).
- Validity: `2019-11-08 → 2029-11-05`. Non-trivial: Meta needs to roll this before Nov 2029.
- Public key: RSA-2048.
- Serial: 6.
- Signature algorithm: `sha256WithRSAEncryption`.
- SHA-256 fingerprint: `B8:C9:D2:8A:61:6F:83:14:85:C2:3F:13:6D:AB:91:B0:91:C9:9A:A0:3D:CA:57:08:1A:25:D1:4F:3B:C1:9F:A1`.

## vbmeta descriptors

Root `vbmeta` carries:

| Descriptor | For | Digest |
|---|---|---|
| hash | boot (34,209,792 B) | `4240e18b03c55579eba e94f4332509c6…` |
| hash | recovery (48,070,656 B) | `e36aba8719a1df2353 eba4e18ff090df…` |
| hash | dtbo (1,181,768 B) | `b02825c0571676fac2 95b5875db0e9fe…` |
| chain_partition | vbmeta_system (rb_loc=1) | separate RSA-2048 pubkey |
| chain_partition | vbmeta_vendor (rb_loc=2) | separate RSA-2048 pubkey |

Plus a stack of `property` descriptors with `com.android.build.*.os_version`, `.fingerprint`, `.security_patch` — these are where the **internal Meta build-host hostname leaks into AVB-signed metadata** (F-03 in the firmware review).

`vbmeta_system` and `vbmeta_vendor` have their own public keys (signed by the root) and each carries `hashtree` descriptors for their partitions. Hashtree digests are SHA-1 (the F-02 in the firmware review).

## Rollback index

Each vbmeta has a `rollback_index` value (`1788652800` on our build = early 2027 timestamp). The device stores a per-slot rollback counter in a tamper-evident area; it refuses to boot an OTA whose rollback_index is lower than what the device has seen. Prevents downgrading to a known-vulnerable older build.

## What breaks the chain

- Modifying any byte of any verified partition → dm-verity hashtree mismatch → vbmeta fails → bootloader refuses.
- Replacing a partition with a differently-signed one → vbmeta chain_partition cert doesn't match → fail.
- Flashing an older OTA → rollback_index check fails → refuses.
- Replacing `vbmeta` itself with a different signer's → root RSA check fails against the fused cert → fail.

## What you cannot modify at all

- The on-die fuses (they're OTP — one-time-programmable).
- The PBL (in ROM).
- XBL and ABL without a Meta signing key.
- Any of the verified partitions without breaking dm-verity.

## Where the bootloader-unlock conversation ends

Meta's bootloader on retail Quest 2 has **no `fastboot flashing unlock` equivalent**. Even on dev-mode-enabled accounts, there's no path to tell the bootloader "trust my new vbmeta." Historical Quest jailbreak work targeted specific Qualcomm/Oculus vulnerabilities in older firmware; the current build has those patched.

So: a sideloaded APK (like QuestClassic) coexists with Meta's signed system; a *custom ROM* would need either (a) a new unpatched exploit chain or (b) Meta's signing key. Neither is in scope for this project.

## See also

- [reverse-engineering/ota-format.md](ota-format.md) — the layer this verifies.
- [security/firmware-review.md](../security/firmware-review.md) — F-02 and F-09 live here.

---

_Last reviewed against the Nov 2026 Quest 2 OTA._
