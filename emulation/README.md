# Emulation feasibility notes

**TL;DR**: you cannot boot Meta's Quest 2 firmware end-to-end in an emulator. You *can* run specific pieces of it in isolation for security analysis. This directory holds our honest setup for the second thing, not a lie about the first.

## Why end-to-end emulation is a multi-person-year project

The Quest 2 Android build we reverse-engineered boots like this (confirmed from the OTA):

```
PBL (Qualcomm on-die ROM, signed by Qualcomm fuses)
  └─ XBL (signed by Qualcomm + Meta) ← verifies next
      └─ ABL (signed by Meta `AOSP_Release_Pub` descendant) ← verifies next
          └─ Linux kernel (`boot.img`)
              └─ init → AOSP + HorizonOS user-space
                  └─ TrustZone (QSEE, parallel) ← signed, serves `tz.img`
                  └─ Modem (Hexagon DSP, parallel) ← signed, serves `modem.img`
                  └─ GPU firmware (Adreno 650) ← signed, needs silicon
```

Each stage verifies the next against keys rooted in physical fuses + a Qualcomm keybox installed at manufacture. QEMU has no such fuses. No public SM8250 secure-boot bypass exists; even the old Quest-jailbreak scene worked around specific vulnerabilities in older firmware, not a full emulation path.

TrustZone in particular is a wall: `tz.img` on this device expects QSEE-capable hardware (ARM TrustZone with specific secure-world services). Running it under TrustZone-less QEMU segfaults as soon as the shell calls into CMS (the trusted app that owns the device auth key).

So: **booting the full system in QEMU is not a thing anyone can currently do on this device**, and that's a hardware-rooted fact, not a software limitation we can engineer around in a weekend.

## What IS feasible

Three layered approaches, from most to least ambitious:

### 1. AOSP Cuttlefish for framework-level work

Cuttlefish is Google's reference cloud Android VM. You can run generic AOSP Android 14 (same base as HorizonOS v209) and test API behaviors, Compose UI, your own app's code paths. It does NOT simulate HorizonOS daemons, Meta's `orchestrator`, Parfait, or any vendor HALs.

```bash
# Rough shape; see cuttlefish/README.md for details.
cuttlefish/start.sh                      # starts a Cuttlefish Android 14 instance
cuttlefish/install-questclassic.sh      # ADB-installs our APK into it
```

Useful for: testing QuestClassic itself without needing a real headset. Not useful for Quest-vendor-specific research.

### 2. `unidbg` / `qiling` for component-level fuzzing

`unidbg` is a user-space ARM emulator that can load a shared library (`.so`) and call its JNI exports with a stubbed-out Android environment. `qiling` is the same idea at a different abstraction. Both let you:

- Lift a specific native library out of the OTA.
- Run it in isolation with controlled inputs.
- Fuzz the entry points.
- Log crashes and malformed-input behaviors.

This is the standard mobile-security technique. Our `unidbg/` directory has a minimal setup pointed at ***your own copy*** of `libshell.so` (we don't redistribute Meta IP).

**What you'd legitimately use this for**: understanding how a parser you control behaves; checking reachability of specific functions; reporting findings to Meta's bug bounty if you find real bugs.

**What we won't help with**: developing exploits against live consumer devices.

### 3. Static-only analysis

What the parent reverse-engineering project did:

- Read the manifest, decode the protos.
- `objdump` / `nm` / `strings` / `readelf`.
- Mount ext4 images with `debugfs`, grep for interesting files.
- `jadx` on APKs for readable Java.

No code runs. Everything here is reversible, legal, and well-established.

The parent project's `REVERSE_ENGINEERING_REPORT.md` is the output of this approach; [`docs/security/firmware-review.md`](../docs/security/firmware-review.md) is the defensive-security distillation.

## What's in this directory

```
emulation/
├── README.md             this file
├── scripts/
│   ├── setup-unidbg.sh   installs unidbg + prepares its environment
│   └── stub-env.kt       JNI-env stubs for unidbg loading libshell.so
├── unidbg/
│   └── README.md         how to point unidbg at your own extracted .so
└── cuttlefish/
    └── README.md         pointer to Google's Cuttlefish docs + our install script
```

The scripts assume **you** supply the Meta firmware files. We do not include them.

## What's not here, on purpose

- **No Meta firmware images or binaries.**
- **No pre-built QEMU VM claiming to be "the Quest OS."**
- **No exploit code.**
- **No "automatic vulnerability scanner" for live firmware.**

If one of those existed and worked, it'd be a bug-bounty submission to Meta, not a GitHub tool. We'd rather be quietly useful for honest research than loudly useless for anything else.

## If you want to go deeper

Suggested reading:

- [AOSP's A/B update_engine source](https://cs.android.com/android/platform/superproject/main/+/main:system/update_engine/) — same CrAU parser we ported to Kotlin.
- [Qualcomm's secure-boot overview](https://www.qualcomm.com/content/dam/qcomm-martech/dm-assets/documents/secure-boot-and-image-authentication-technical-overview-v2.0.pdf) — explains the fuse/key chain.
- [Project Zero's past Quest/Oculus writeups](https://googleprojectzero.blogspot.com/) — see search for "Oculus" + "Quest".
- [Meta's bug bounty scope](https://bugbounty.meta.com/) — the right venue for anything that walks.

---

_Last reviewed Nov 2026._
