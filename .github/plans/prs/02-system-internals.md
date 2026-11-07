## What

Adds a **System Internals** panel that surfaces, live on the headset, every quirk and fingerprint the OTA reverse-engineering turned up. Everything is read from `android.os.Build` + `SystemProperties.get` — the same values Settings → About and `adb shell getprop` return — so nothing privileged is required.

## Why

The stock Settings app shows a tidied summary; what's actually on your device is messier and more interesting. This panel foregrounds the five highlights worth caring about:

1. **Vendor SPL gap** — `ro.build.version.security_patch` vs `ro.vendor.build.security_patch`. On Qualcomm-vendor devices the vendor-partition SPL often trails the user-facing one by years; worth seeing.
2. **Build-host leak** — `ro.build.host` + the per-partition fingerprints retain Meta's internal Tupperware/Lulea build-host name (e.g. `*.twshared*.fbinfra.net`). Mild information disclosure that didn't get scrubbed in the release pipeline.
3. **HorizonOS lineage** — `ro.build.branch` is the real OS version (`releases-oculus-14.0-vN`); Android's version string is just the AOSP base.
4. **SoC + GPU identity** — shows the Snapdragon SM8250 (Quest 2) / newer SoC on 3+ devices, Adreno GPU driver string, CPU variant, ABIs.
5. **USB / recovery** — Meta's USB VID (`0x2833`) plus the ADB and Fastboot PIDs.

## How

- `os/SystemPropertyProbe.kt` — reflection-wrapped `SystemProperties.get(String)`, with a small `summary` convenience string.
- `ui/panels/SystemInternalsPanel.kt` — Compose panel with one glass card per theme (OS lineage, SPL gap, build-host leak, SoC+GPU, USB+recovery). Highlights SPL gap and host leak in amber when they fire.

## Test plan

- Build + sideload.
- Open "System Internals" on a Quest 2.
- Expected on the build we analysed: vendor SPL `2018-06-05` vs device SPL `2026-09-06` (amber), host leak warning present, HorizonOS branch `releases-oculus-14.0-v209`.

## Related

Parent project's `REVERSE_ENGINEERING_REPORT.md` §2, §9, §10 for each surfaced field.
