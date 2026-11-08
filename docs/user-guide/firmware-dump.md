# Firmware Dump panel

Lists every partition + file a rooted shell could legitimately read off the device, and what you'd get from it. Firmly draws the line at what ISN'T dumpable even with root, and explains why.

## Dumpable (given root)

| Target | Contents |
|---|---|
| `boot_a/_b` | Linux kernel + ramdisk |
| `vendor_boot_a/_b` | Vendor ramdisk + DTB |
| `dtbo_a/_b` | Device-tree overlay |
| `system_a/_b` | AOSP framework + oculus system apps |
| `vendor_a/_b` | vendor HALs, firmware blobs |
| `product_a/_b` | Meta-specific extras |
| `system_ext_a/_b` | VrShell + oculus system_ext packages |
| `otacert` | Meta's AOSP_Release_Pub-derived OTA cert (public) |
| `AVB public keys` | Chain roots used by vbmeta |

## Partially dumpable

| Target | Why only partial |
|---|---|
| `modem_a/_b` | Raw partition reads; running image lives in QSEE |
| `tz_a/_b` | Partition readable; QSEE won't execute unsigned replacement |
| `abl_a/_b` | Readable, not modifiable without fuse-blow |

## NOT dumpable

| Target | Why |
|---|---|
| OEM PK (per-device) | Fuse-rooted; not reachable from software |
| Qualcomm Keybox | In TrustZone, no software exfil |
| User biometrics | Protected by BiometricPrompt + QSEE |

## What a dump costs

- `dd` of an A/B system image is ~2 GB on Quest 2. Storage check first.
- Partition-level dumps write to `/sdcard/Dump/` so they're easy to `adb pull` off.
- Dumps are reversible-engineerable off-device; the device needs no further privileges to hand them over.

## Why we can't dump the keys

Meta's signing keys, the Qualcomm Keybox, modem firmware, and Adreno GPU microcode all live in silicon fuses or the TrustZone secure world. The hardware is designed specifically to prevent extracting them from software — rooting the normal Android world doesn't change that. See [ADR 0009](../adr/0009-root-gating-philosophy.md) and [firmware-review.md](../security/firmware-review.md) for details.

## Related

- [Signature Verifier panel](signature-verifier.md) — verify dumps against their advertised signatures
- [Firmware review](../security/firmware-review.md) — what's in the dump
