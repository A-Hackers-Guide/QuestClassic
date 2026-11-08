# System Internals panel

The deep-reading version of the About panel. Surfaces, live on the headset, every fingerprint and quirk our reverse-engineering turned up — categorized, cross-linked, and highlighted when something's worth paying attention to.

## Cards

### OS lineage

Pulled from `getprop`:

- `ro.product.system.model` + `ro.product.system.manufacturer`
- `ro.product.system.device` (codename — `hollywood` on Quest 2)
- `ro.build.branch` — the real HorizonOS version, like `releases-oculus-14.0-v209`. Android's version string (14.0) is just the AOSP base, not the HorizonOS release.
- `ro.build.version.release`, `.sdk`, `ro.product.first_api_level`
- `ro.vndk.version`
- `ro.oculus.reality_mode` — `mr` on Quest 2 regardless of whether MR is active

### Security patch gap

Compares `ro.build.version.security_patch` against `ro.vendor.build.security_patch`. If they differ, the panel highlights the row in amber and explains: common Qualcomm-vendor anti-pattern — the vendor partition carries its own SPL which is often months-to-years behind the user-facing patch level. Qualcomm BSP patches between those dates may not be reflected in this build.

On the Quest 2 OTA we analysed in 2026:

- device-wide SPL: `2026-09-06`
- vendor partition SPL: `2018-06-05`

An 8-year gap.

### Build-host leak

`ro.build.host` and `ro.system.build.fingerprint` sometimes carry internal build-server hostnames that Meta's release pipeline didn't scrub. On the Nov 2026 Quest 2 build:

```
3c64-a274-15de-0a00.twshared10220.29.lla2.tw.fbinfra.net
```

Decoded:

- `twshared` — Meta's Tupperware (internal container platform)
- `lla2` — Luleå, Sweden datacenter 2
- `fbinfra.net` — Facebook/Meta internal infra DNS zone
- The hex-looking first segment is the container/machine ID

If this string is present on your device, the panel flags it amber. It's not a security vulnerability — it's a soft information leak Meta likely intended to scrub.

### SoC + GPU

- `ro.soc.manufacturer`, `ro.soc.model` (Qualcomm SM8250 on Quest 2 — the base for Snapdragon XR2 Gen 1)
- `ro.hardware.egl`, `ro.hardware.vulkan` (Adreno on Quest)
- `ro.bionic.cpu_variant` (`cortex-a55` — the lowest-common across SM8250's big.LITTLE cluster)
- `ro.product.cpu.abilist`
- A/B + dynamic partitions + virtual A/B flags

### USB + recovery

- `ro.recovery.usb.vid` — `2833` is Meta's USB vendor ID
- `ro.recovery.usb.adb.pid`, `.fastboot.pid` — Quest 2 PIDs are `0086` and `0081`
- `ro.zygote` — Quest 2 is still `zygote32`, meaning apps run 32-bit by default (!)

## Why these specific highlights

Each one came out of the OTA reverse-engineering done before we wrote this app. The full write-up is in [reverse-engineering/overview.md](../reverse-engineering/overview.md) and the parent project's RE report.

## What's intentionally NOT here

- Nothing that requires elevated permissions (we don't have them).
- Nothing from vendor HALs (SELinux-gated; see [Diagnostics § Why we don't talk to vendor HALs](diagnostics.md#why-we-dont-talk-to-vendor-hals)).
- Nothing that would require executing a binary with elevated privileges.

---

_Last reviewed against v0.1.0 + the 2026-09-06 Quest 2 OTA._
