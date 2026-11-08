# ADR 0009 — Root gating: detect, don't elevate

Status: Accepted · 2026-11-07

## Context

Several QuestClassic features (strip-Meta, firmware dump, boot-animation swap, dev-super-mode, home-menu swapper) are only possible on a device where root has been enabled by some external means — a userdebug/eng build, a signed vendor image, or a Magisk install the user performed after unlocking an older firmware's bootloader.

We had to decide:

1. **Ship the exploit ourselves.** Build a root-exploit chain into the APK so any installer gets root for free. **Rejected**: see below.
2. **Refuse to show the feature at all unless rooted.** Hide the UI. **Rejected**: users deserve to see what's possible and why it's gated.
3. **Show the UI; detect root at runtime; only execute the privileged action on rooted devices, with a verbose explanation on stock.** ✅ **Accepted.**

## Decision

Every privileged panel is built with:

- A `RootDetector` runtime probe that returns a confidence rating from NONE to HIGH.
- A `RootGateCard` composable that explains, in plain terms:
  - Whether root was detected (and which signals fired).
  - What this panel would do if root were available.
  - What the honest `adb`/`shell` commands look like.
- The destructive action is only enabled when `RootStatus.isRooted == true`.
- On a stock device the panel is a *dry-run*. You can read the plan, copy the shell commands, audit them, and run them manually via ADB on your own authority.

## Why not ship the exploit

1. **Physical impossibility for the keys that matter.** Signing/encryption keys live in Qualcomm silicon fuses + QSEE TrustZone. No userspace exploit retrieves them, because the hardware fuse-rooted keybox is designed specifically against this.
2. **Legal risk.** Distributing a rooting tool targeting a consumer device with signed boot likely violates DMCA 1201 anti-circumvention. The security-research exemption doesn't cover tools built primarily to circumvent.
3. **Downstream harm.** A rooting tool on GitHub targets anyone's Quest, not just the installer's. Resold devices, kids' devices, stolen devices — the tool doesn't know.
4. **Brittleness.** Public Quest-2 root chains have historically relied on specific old-firmware vulns that Meta patched. Shipping one in-app would mean the app bricks itself on any updated device.

## Why not hide the UI

1. **Transparency.** Users want to know what's possible on their device even if they can't act today.
2. **Documentation value.** The dry-run panels are living documentation of what an advanced user *could* do with root + ADB.
3. **Education.** A visible "this requires root, here's why, here's the exact `adb` you'd run" is more useful to the ecosystem than a hidden feature.

## Consequences

- Every root-gated panel in QuestClassic carries `RootGateCard` prominently.
- The panels' plans (shell commands) are visible to the user; nothing is hidden.
- On a rooted device, actions run; on a stock device, nothing writes.
- Users on stock devices who want more are directed to responsible sources (XDA, Magisk forums) rather than the app itself.

## Related ADRs

- [0004 · No telemetry](0004-no-telemetry.md) — the privileges we DO have we don't abuse.
- [0005 · GitHub Releases as update channel](0005-github-releases-as-update-channel.md) — no Meta cloud involved.

## Future revisits

- If a public, non-weaponizable root unlock becomes available for Quest 2 and the ecosystem has moved on (devices long-EOL), we could revisit linking to external root tooling from the app. We will not bundle it.
