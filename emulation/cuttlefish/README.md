# Cuttlefish — AOSP reference cloud Android VM

Not a Quest-OS emulator. Google's Cuttlefish runs generic AOSP Android on a Linux host as a cloud-ready VM. Useful for exercising QuestClassic *itself* without a physical Quest handy, and for testing framework-level API behavior on AOSP Android 14.

## Doesn't simulate

- HorizonOS daemons (`orchestrator`, Parfait, `xrspd`, `mrsystemservice`, etc.).
- Vendor HALs (`vendor.oculus.*`, `vendor.meta.*`).
- Passthrough / hand-tracking / spatial APIs.
- Meta's auth stack.
- The nostalgic look will render, but anything Quest-specific gracefully degrades (as it should, per `uses-feature required="false"`).

## When this is useful

- CI-ing our APK on a device the way GitHub Actions can't — real Android runtime, not just compile-checks.
- Testing new panel UIs before loading onto a headset.
- Reproducing bugs reported by users where "I don't have a Quest handy" matters.

## Setup

See the [Cuttlefish quickstart](https://source.android.com/docs/setup/create/cuttlefish). Basic flow:

```bash
# On a Debian/Ubuntu host with KVM:
sudo apt install -y git devscripts equivs config-package-dev debhelper-compat golang curl
git clone https://github.com/google/android-cuttlefish
cd android-cuttlefish
tools/buildutils/build_packages.sh
sudo dpkg -i ./cuttlefish-base_*_*64.deb || sudo apt-get install -f
sudo usermod -aG kvm,cvdnetwork,render $USER
# ... then reboot

# Download a prebuilt AOSP 14 (vanilla) image:
mkdir cf && cd cf
curl -L -O https://ci.android.com/builds/submitted/<buildid>/aosp_cf_x86_64_phone-userdebug/latest/aosp_cf_x86_64_phone-img-<buildid>.zip
unzip aosp_cf_x86_64_phone-img-*.zip

# Launch:
launch_cvd
```

Then from your workstation:

```bash
adb connect 0.0.0.0:6520
adb install app-release-unsigned.apk
```

Our APK runs. Panels that depend on Quest-specific things degrade to no-ops or "Non-Quest Android device" labels.

## What this is NOT

- Not a way to run Meta's firmware.
- Not a way to reproduce HorizonOS-specific behavior.
- Not a security-research environment for Quest vulnerabilities.

For actual Quest-vendor vuln research, see [`../unidbg/`](../unidbg/README.md).

---

_Last reviewed Nov 2026._
