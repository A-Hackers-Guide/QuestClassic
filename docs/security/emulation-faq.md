# Emulation FAQ — "why can't you just run it in QEMU?"

Short answer: because the Quest 2 refuses to boot anything that isn't signed by keys that aren't in any emulator and can't be pulled out of the device.

Long answer below, in plain English, then in detail.

---

## In one paragraph

A modern phone-class Qualcomm chip boots in stages. The very first stage is burned into the silicon itself — not firmware you can replace, actual gates in the die. That first stage refuses to hand control to anything that isn't signed by Qualcomm. Qualcomm, in turn, signs a loader that only runs a Meta-signed loader. Meta's loader only runs a Meta-signed kernel. Each stage verifies the next against a key, and those keys ultimately trace back to fuses that were blown at the factory. An emulator has no fuses. Which means no emulator has the "I am a real Quest 2" credential the next stage is looking for. End of story — until someone finds a bug in one of the stages, which is what the entire mobile-security research community does for a living, and which stays patched when they succeed.

## Details

### Why the first stage won't run under QEMU

The Snapdragon XR2 Gen 1 (platform codename `kona`, the same base chip in the Quest 2) has what Qualcomm calls a **Primary Boot Loader** (PBL) in on-die ROM. Its job is to:

1. Sanity-check the device state.
2. Look at the `boot` partition and find a Qualcomm-signed loader (XBL).
3. Verify XBL's signature against a **root hash stored in fuses** on the die.
4. If verification succeeds, jump to XBL. If it fails, halt.

A fuse is a one-time-programmable element in the silicon. After the fuse is blown at factory, its state is physically embedded in the chip. QEMU has no fuses. You can emulate a Snapdragon's instruction set — ARMv8-A — but you can't emulate "pretend these fuses are set to Qualcomm's root hash." The fuses aren't a register you flip; they're state the chip reads out of itself.

Which means: even step 1 of the boot chain refuses to proceed on anything that isn't the real silicon.

### Why the next stages don't help

Suppose someone did find a way to emulate the fuses (they haven't, publicly, on the SM8250 family). The next stages are:

- **XBL** (eXtensible Boot Loader) — verifies the **Android Boot Loader** (ABL) against Meta's signing keys.
- **ABL** — verifies the **Linux kernel** and the **Android Verified Boot** metadata against Meta's keys.
- Everything downstream — verifies against AVB.

Meta's private signing keys live in Meta's offline Luleå HSM infrastructure (we can tell, because the public certificate chain ends in `AOSP_Release_Pub` and the build-host leaks from the firmware all point at `twshared*.fbinfra.net`). Those keys are not in the firmware. They are not in any emulator image. They are not on any device, Meta's or anyone else's. They live in a hardware security module.

So even with a hypothetical fused-emulated XBL, the emulated bootloader would be asked to prove it was signed by Meta. It can't. And no software-side bug lets you ask the HSM to sign a Meta-authentication challenge for you.

### Why extracting the keys doesn't work

The Meta signing keys are **outside the device.** You cannot extract them, because they aren't there. The *public* key is embedded in the AVB descriptors; the public key alone verifies signatures but doesn't create them. (If public keys could create signatures, nothing in all of computer security would work.)

Meta's **device-cert key** — the per-device key that identifies "I am Quest 2 serial number XYZ" to Meta's cloud — IS on the device. But it lives in the Qualcomm Secure Execution Environment (QSEE), i.e. TrustZone. TrustZone is a hardware feature of the ARM chip that partitions the CPU into secure and non-secure worlds; code running in the non-secure world (your apps, even root) cannot read memory that belongs to the secure world. QSEE holds the per-device key and signs attestations with it on request; it never emits the raw key material. Even with full root you cannot ask "please give me the key"; QSEE will refuse. The hardware is specifically designed against that request.

### What about TrustZone bugs?

Yes — if there's a bug in QSEE on the Quest 2's QSEE version (string pins at `QSEE_LA.TA.MDM.4.3-00320-KAMORTA.0`, see firmware-review F-28), an attacker with root can sometimes get QSEE to leak or misbehave. This is the kind of thing Project Zero publishes a couple times a year across the whole Qualcomm ecosystem.

Two things make that not a QuestClassic feature:

1. **It's a moving target.** Known QSEE bugs are patched within a Qualcomm Security Bulletin cycle. Shipping a "QSEE exploit" in-app means the app bricks itself on every security patch and we spend our time chasing the latest TrustZone CVE instead of making a nice launcher.
2. **It's the kind of research that belongs with Meta's bounty.** If we find one, we send it to Meta. We don't weaponize it in a consumer app.

### What about modem and GPU firmware?

- **Modem** (`modem.img`) runs on the **Hexagon DSP**, a VLIW DSP in the Snapdragon. QEMU has rudimentary Hexagon support (bare-metal), but not enough to run a signed Qualcomm cellular modem with its full HAL stack. The modem image is also keyed; see "why signatures are the problem."
- **GPU firmware** (`a650_sqe.fw` + `a650_zap.mbn`) runs on the Adreno 650 GPU. Needs the actual Adreno silicon. `freedreno` (Mesa's reverse-engineered Adreno driver) works on Linux without Qualcomm firmware, but that's an entirely different driver that doesn't help us run Meta's build.

### So what DO people actually do?

Three things, in order of ambition:

#### 1. AOSP Cuttlefish — framework-level work

Google's Cuttlefish is an AOSP reference cloud Android VM. It runs vanilla AOSP 14 on a Linux host. You can `adb install` QuestClassic into it and poke at the parts of the app that don't depend on HorizonOS daemons. It doesn't simulate anything Quest-specific — no VrShell, no `orchestrator`, no hand tracking, no passthrough. But it does run our own app's code paths, so it's great for CI'ing framework-side bugs without a headset.

See [`emulation/cuttlefish/README.md`](../../emulation/cuttlefish/README.md).

#### 2. `unidbg` / `qiling` — component-level fuzzing

Lift one `.so` out of a Quest 2 OTA (you provide the OTA; we don't redistribute Meta IP). Load it under `unidbg`, which emulates a stubbed Android userland environment. Feed the JNI entry points malformed input. Log crashes. Classic mobile-security technique.

Great candidates in `libshell.so`:

- `Java_com_oculus_vrshell_ShellApplication_nativeDumpsys`
- `Java_com_oculus_vrshell_ShellApplication_nativeInjectPanelIpc`
- `Java_com_oculus_vrshell_ShellApplication_nativeFrameCommand`

Each is a reachable JNI entry that takes Parcels/strings in and parses them in C++ — classic parser-bug territory. If you find one, report it to Meta.

See [`emulation/unidbg/README.md`](../../emulation/unidbg/README.md).

#### 3. Static analysis

`objdump`, `nm`, `readelf`, `strings`, `jadx`, `debugfs`. No code runs. Everything is reversible, legal, and well-established. This is what produced the 30 findings in [firmware-review.md](firmware-review.md).

### Where to report what you find

- **Meta firmware** (shell, horizon, VR services) → [bugbounty.meta.com](https://bugbounty.meta.com/).
- **Qualcomm vendor BSP** (QSEE, modem firmware, Adreno, Hexagon) → Google's Vulnerability Reward Program forwards into Qualcomm.
- **AOSP framework** (upstream Android code) → Google ASR.
- **QuestClassic** (our own app) → [security/reporting.md](reporting.md).

## Sidebar: "but what if I just get the keys from a decapped chip?"

You can, in principle, send chips to a lab that will etch them open and read the fuses with an electron microscope. Known cost: tens to hundreds of thousands of USD, destructive to the chip, and recovers *that one chip's* keys. Meta's signing key is not on any chip. So this doesn't help even if you had the budget and were willing to physically destroy a device. Fun as a thought experiment, not relevant.

## Sidebar: "but what about [Quest 2 jailbreak X] from 2021?"

Several old Quest 2 jailbreaks existed, targeting specific vulnerabilities in older firmware. They were patched. They don't apply to a 2026 OTA. We're not going to re-weaponize them; we're going to write a nice launcher.

---

_Last reviewed Nov 2026._
