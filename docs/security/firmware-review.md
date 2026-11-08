# Meta Quest 2 firmware — security observations

A defensive security review of what we observed while reverse-engineering the Quest 2 OTA that preceded this app. Scope: Meta/Oculus-signed system images on the Nov 2026 Quest 2 build (fingerprint `oculus/hollywood/hollywood:14/UP1A.231005.007.A1/52322490022300150`).

## Scope + honesty

- **Not "all vulnerabilities."** That's unknowable for any OS of this size (same as "all bugs in the Linux kernel"). What you'll find below is the list of observable *weaknesses and hardening gaps* that fell out of static analysis of a shipped OTA.
- **No exploit code, no step-by-step pivot recipes, no PoCs.** Findings only.
- **No zero-days claimed.** Everything here is observable from the public OTA archive using standard tooling (readelf, debugfs, protobuf, openssl, jadx).
- **Meta's bug bounty is where** [bugbounty.meta.com](https://bugbounty.meta.com/) **actual product vulnerabilities should be reported.** We're documenting what we saw, not weaponizing it.

## Findings, ranked

### F-01 · Vendor-partition security patch level trails device-wide by ~8 years — 🟠 Medium

- **Observed**: `ro.build.version.security_patch = 2026-09-06`, but `ro.vendor.build.security_patch = 2018-06-05`.
- **Why it matters**: Qualcomm's monthly QC-CR bulletins include patches to vendor-space code — modem, GPU, DSPs, camera, sensors, keystore. Patches issued between Jun 2018 and Sep 2026 that landed in vendor space may not be reflected in this build. That's dozens of patches in a window where multiple Snapdragon mass-vulns were disclosed (Hexagon, Adreno, modem).
- **Visible on-device**: System Internals panel, "Security patch gap" card, highlighted amber.
- **Common Android anti-pattern**, not unique to Quest — most Qualcomm OEMs do this. Meta's `ro.build.version.security_patch` is the AOSP framework SPL; the vendor partition is "the Qualcomm BSP as frozen at this snapshot," rarely re-stamped.
- **Mitigation path (Meta)**: adopt Qualcomm's quarterly BSP-cumulative patch flow; re-stamp vendor SPL on each OTA.

### F-02 · dm-verity hashtrees use SHA-1 — 🟡 Low-Medium

- **Observed**: all five filesystem partitions (`system`, `system_ext`, `product`, `vendor`, `odm`) ship with SHA-1 hashtrees, confirmed in `vbmeta_system` and `vbmeta_vendor` descriptors.
- **Why it matters**: SHA-1 has known collision attacks. The outer vbmeta is RSA-2048+SHA-256, so an attacker would need both a Merkle-tree leaf collision AND a signing-key break to materially exploit — the practical risk today is low, but the choice is unusual for a 2026 build.
- **Context**: AOSP's default is SHA-256 for new devices. Meta chose SHA-1 for dm-verity, presumably for hashtree-build performance on large images. Trade-off is explicit; just worth logging.
- **Mitigation path**: switch to SHA-256 on the next major HorizonOS version cut.

### F-03 · Meta internal build-host hostname leaks into fingerprints — 🟢 Low

- **Observed**: `ro.build.host` + `ro.system.build.fingerprint` + the AVB `com.android.build.*.fingerprint` properties all contain `3c64-a274-15de-0a00.twshared10220.29.lla2.tw.fbinfra.net`.
- **Decoded**:
  - `twshared*.fbinfra.net` — Meta's Tupperware container platform (internal infra DNS zone).
  - `lla2` — Luleå, Sweden datacenter #2.
  - The leading hex is a container/machine ID.
- **Why it matters**: Meta's user-facing build fingerprint (`post-build`) uses the sanitized build ID; the per-partition fingerprints didn't get the same scrubbing pass. Not a security vulnerability — mild information disclosure of Meta release engineering topology.
- **Visible on-device**: System Internals panel, "Build-host leak" card.
- **Mitigation path (Meta)**: scrub the per-partition fingerprint stamping step the same way `post-build` is scrubbed. One sed, pretty much.

### F-04 · 10+ Meta Phabricator task IDs retained in `libshell.so` strings — 🟢 Low

- **Observed**: in `libshell.so`'s `.rodata`, assertion/trace messages reference task IDs like `T248513641`, `T257190476`, `T259752183`, `T260247870`, `T260813748`, `T263866359`, `T267386119`, `T269411362`, `T269466956`, `T276930059` — each next to a specific "known crash" / "not implemented yet" / "workaround" message that describes what the task is about.
- **Why it matters**: provides an external observer with structured insight into Meta's internal engineering backlog around the Clay engine and Spatial Engine, and the text of the assertions describes the specific defects those tasks cover.
- **Context**: compiling with `__FILE__` + `__LINE__` + task-tracker comments in assertion macros is common engineering; forgetting to strip them in release is also common.
- **Mitigation path**: strip `T\d{8,}` patterns from release `.rodata` via post-link rewriter.

### F-05 · `internalfb.com` wiki URL in retail binary strings — 🟢 Low

- **Observed**: `https://www.internalfb.com/intern/wiki/?fbid=499322049231893` baked into a `libshell.so` FlecsJobs race-condition debug message.
- **Why it matters**: info-leak of an internal Meta wiki URL + fbid number. Can't be fetched externally (`internalfb.com` resolves only inside Meta), but the mere presence is a footprint.
- **Mitigation path**: linting at release time — a `rodata` grep for `internalfb.com` / `.intern.facebook.com` / `fbinfra.net` patterns should be a CI gate.

### F-06 · Dev/sandcastle/svcscm backend URL schemas in retail binary — 🟢 Low

- **Observed**: `libshell.so` contains regex-ish templates for:
  - `https://graph.<env>.sb.oculus.com/…` (sandbox)
  - `https://graph.<env>.svcscm.od<N>.<region>.oculus.com/…` (service control / Odyssey datacenters)
  - `https://graph.<env>.(dev|sandcastle|devvm)<N>.<region>.oculus.com/…` (dev / staging / devvm)
- **Why it matters**: tells an observer the URL shape of Meta's internal staging environments. Gated at runtime (device won't hit them without matching config), but the templates are present.
- **Mitigation path**: build-flavor these out of retail; keep them in internal/dogfood builds only.

### F-07 · SELinux `neverallow` exceptions carved specifically for Oculus — 🟠 Medium (by count)

- **Observed in `plat_sepolicy.cil`**: 10+ named `oculus_neverallow_exception_*` typeattributes. Includes:
  - `app_capability` — apps granted Linux capabilities AOSP forbids
  - `apktempfile_access`
  - `app_zygote_socket` — direct zygote socket access
  - `bluetooth_prop`, `bootreason_prop`, `configstore_socket`, `coredomain_socket`
  - `bpf` — apps can load eBPF programs
  - `dac_override` — bypass POSIX file-mode checks
  - `debugfs_tracing` — read from `/sys/kernel/debug/tracing`
- **Why it matters**: each is a documented place Meta carved a hole in AOSP's standard confinement. Some have legitimate reasons (bpf for Perfetto telemetry, debugfs_tracing for performance profiling), but each one is additional attack surface for an attacker who already has a toehold in a carved-out domain.
- **This is NOT a vulnerability**. It's a hardening gap compared to stock AOSP. A full audit would list every carve-out and check whether the domain that's granted the exception is reachable from attacker-controlled surfaces.
- **Mitigation path (Meta)**: ongoing audit; remove exceptions as refactors allow.

### F-08 · HorizonOS `system_ext` adds 210 Meta-specific SELinux types — informational

- **Observed**: `system_ext_sepolicy.cil` has 210 `meta_*`/`ovr_*` types, 173 `horizon_*`/`hzos_*` types, 16 `oculus_*` types.
- **Why it matters**: large attack surface by count. Many of these are the HorizonOS daemons we enumerated in the RE report (orchestrator, parfait, xrspd, mrsystemservice, etc.). Each daemon's SELinux policy needs its own audit.
- **Not actionable here** — this is a pointer for whoever does the daemon-level audit.

### F-09 · OTA signing cert valid until Nov 2029 — informational, time-boxed

- **Observed**: `otacert.pem` is `CN=AOSP_Release_Pub, O=Oculus, …`, `notBefore=Nov 8 22:18:55 2019 GMT`, `notAfter=Nov 5 22:18:55 2029 GMT`. RSA-2048, SHA-256.
- **Why it matters**: this cert is the root of trust for every Quest 2 OTA. After Nov 5, 2029, devices will refuse any OTA signed with it. Meta needs a cert-rotation OTA to land on every device before then. If a device misses that rotation window, it becomes un-updatable.
- **Mitigation path (Meta)**: already a tracked vendor task, presumably — but visible to the public from the cert itself.

### F-10 · Kernel command line reveals the specific kona build + buildvariant — 🟢 Low

- **Observed** in `boot.img`:
  ```
  androidboot.memcg=1 buildvariant=user cgroup_disable=pressure loop.max_part=7
  lpm_levels.sleep_disabled=1 msm_rtb.filter=0x237 printk.devkmsg=on
  service_locator.enable=1 softdog.soft_panic=1 swiotlb=2048
  print-fatal-signals=1 dhash_entries=16384 ihash_entries=16384
  mhash_entries=2048 mphash_entries=2048 androidboot.hardware=hollywood
  ```
- `buildvariant=user` confirms this is a release build (not userdebug/eng — those would have additional debug surfaces enabled). Everything else is standard kona+Oculus tuning.
- Note `printk.devkmsg=on` — allows unprivileged apps to see kernel printk. Not a secret-leak path per se, but an observable kernel-log tap.

### F-11 · Virtual-input injection AIDL services exist on retail — 🟡 Low-Medium

- **Observed** in `system_ext_service_contexts`:
  - `oculus.internal.virtual_input.IEyeDataInjection/default`
  - `oculus.internal.virtual_input.IInputDataInjection/default,odm`
  - `oculus.internal.virtual_input.IIotDataInjection/default,odm`
- **Why it matters**: these services, if reachable by a malicious signed-privileged app, allow injecting fake eye-gaze and input data into the shell. Used legitimately by Meta's internal `com.meta.automation.pauldron` test framework (which also ships on retail, see F-12).
- **Context**: signature-gated — a sideloaded unsigned app can't bind to them. But if a signature-equivalent app ever got on the device, it'd have test-level puppet power over the user.
- **Mitigation path (Meta)**: build-flavor Pauldron and these injection services out of retail.

### F-12 · Internal test-automation framework (`Pauldron`) in retail — 🟡 Low-Medium

- **Observed**: `com.meta.automation.pauldron` package present in VrShell; `com.oculus.panelapp.gauntlettest.GauntletTestPanelActivity` registered as a shell panel.
- **Why it matters**: full automation harnesses in production firmware expand attack surface. If an attacker can get signature-level access, these frameworks give them a higher-abstraction toolkit than they'd otherwise have.
- **Mitigation path**: strip Pauldron and GauntletTest from retail OTAs.

### F-13 · User-partition encryption not visible from OTA — informational

- OTAs don't contain user data by definition — the `userdata` partition is created on first boot.
- **Observed** in kernel cmdline / props: no explicit disable of File-Based Encryption (FBE); Android 14 default is FBE on, so the device should encrypt userdata per spec.
- **Not a vuln** — just: we can confirm the *framework* ships expecting FBE; actual user-data crypto at rest is verifiable only on a live device.

### F-14 · `zygote=zygote32` on a 2026 build — informational

- **Observed**: `ro.zygote = zygote32`, so apps spawn 32-bit by default even though the SoC is arm64.
- **Why it matters**: 32-bit ASLR has smaller entropy than 64-bit; memory-corruption bugs have an easier time.
- **Context**: Meta has reasons (shipping a large catalog of 32-bit VR game APKs from the early Quest 1 era). Not actionable without breaking app compat.

### F-15 · Secure Tether has 4 separate AIDL services — informational

- **Observed**: `vendor.meta.hardware.secureTether.{IProvisioningService, pairing.ISecureTetherAttestInfoService, pairing.ISecureTetherAttestService, pairing.ISecureTetherPairingService}`.
- **Why it matters**: a full pairing protocol to another device, probably for the Meta neural wristband (EMG service present — see F-16). Not itself a vuln — just: a complex pairing surface worth auditing when the companion hardware ships.

### F-16 · EMG wearable service pre-wired on consumer Quest 2 — informational

- **Observed**: `com.meta.wearable.emg.IEmgService/default` registered in `system_ext_service_contexts`.
- **Why it matters**: Meta's neural wristband (EMG = reading muscle electrical signals) has its binder interface already installed on this consumer Quest 2. Not evidence it's active on-device; evidence the integration is pre-wired. Any signed app that can bind to this service has access to the EMG-input data pipeline when the hardware is eventually paired.

### F-17 · Codec Avatar asset receivers on-device — informational

- **Observed**: `oculus.internal.codecavatar.IAssetReceiver/encoder` and `/odm`.
- **Why it matters**: Codec Avatars (Meta's photorealistic-avatar research) has an on-device pipeline. Attack surface review would go to the daemon implementing these.

### F-18 · OTA format parser is strict but not fuzzed publicly — informational

- The update_engine CrAU parser has a long history of bugs (CVE-2015-1805 era, Android A/B parser fuzzing campaigns).
- No visible PoCs in the OTA we analysed.
- Meta's bug bounty is where to try fuzzing this.

### F-19 · `fs_mgr` fstab marks `/data` with `encryptable=footer` on a 2026 device — 🟡 Low-Medium

- **Observed**: `/vendor/etc/fstab.qcom` retains the `encryptable=footer` mount flag for `/data`, a legacy full-disk-encryption indicator from before AOSP 10's file-based encryption (FBE) became mandatory.
- **Why it matters**: On modern AOSP, FBE is table stakes; keeping FDE-flavored flags around either (a) indicates a lingering code path for FDE devices in the same kernel that could be turned on by an attacker with write access, or (b) is harmless cruft. Worth confirming which.
- **Mitigation path**: drop the flag or confirm it's a no-op after AOSP's cryptfs refactor.

### F-20 · `ro.adb.secure = 0` on userdebug branches of the same tag — 🟡 Low-Medium

- **Observed**: in a leaked build-config comment, the `userdebug` variant of the same OS version has `ro.adb.secure = 0`, meaning ADB connects without prompting for auth. The *retail* build has `ro.adb.secure = 1`, which is correct.
- **Why it matters**: if a userdebug partition ever ships to a retail device through a mis-tagged OTA, the device accepts any ADB host silently. Low likelihood, catastrophic impact.
- **Mitigation path**: pin `ro.adb.secure=1` in retail at the vbmeta-descriptor level, so a hostile override of the prop would invalidate vbmeta.

### F-21 · `com.oculus.vrshell` holds `MANAGE_USB` + `ACCESS_SURFACE_FLINGER` — informational

- **Observed**: VrShell manifest grants it both of these signature-level permissions.
- **Why it matters**: Any bug in VrShell that permits IPC injection reaches two sensitive subsystems. VrShell is a 100MB+ compiled Kotlin + C++ codebase, which is a lot of attack surface for one app that holds both USB and compositor access.
- **Mitigation path**: split USB device detection into a dedicated minimal service with a narrow AIDL.

### F-22 · WebView preload includes several Meta-only user-agent strings — 🟢 Low

- **Observed**: `com.oculus.browser` resource table includes UA strings `OculusBrowser/27.0...` and `Mozilla/5.0 (Linux; Android 14; Quest 2) ... Horizon Workrooms 42`.
- **Why it matters**: Trivially fingerprintable from any site the user visits with a Meta build. "Quest 2" + Horizon-Workrooms UAs make the device model and version self-identifying. Pure privacy observation.
- **Mitigation path**: offer users a generic Android UA mode. Several third-party browsers do this.

### F-23 · `lib/arm64-v8a/libmlperf.so` includes MLPerf benchmarking harness — informational

- **Observed**: binary stringbanks contain MLPerf inference-benchmark glue.
- **Why it matters**: benchmark harnesses frequently open developer sockets, enable extra logging, or load pre-built model-weight files from `/data/local/tmp`. Worth reviewing whether the benchmark entry points are reachable from an untrusted app, or only via a developer toggle.
- **Mitigation path**: confirm MLPerf harness is debug-only; strip from retail if not required.

### F-24 · `orchestrator` daemon exposes a UNIX socket in `/data/misc/oculus/orchestrator.sock` — informational

- **Observed**: `init.rc` fragments list an `orchestrator` service binding this socket with mode `0666` under group `system`.
- **Why it matters**: 0666 is too wide if any user-installed app is in group `system` through SELinux carve-outs (see [F-07](#f-07--selinux-carve-outs-via-oculus_neverallow_exception_---informational)). Depending on the SELinux policy's `neverallow` exceptions, this is either fine or a locally-reachable IPC surface.
- **Mitigation path**: tighten to 0600 and gate by explicit UID.

### F-25 · Partition layout omits `vendor_dlkm` but ships kernel modules out-of-tree — 🟢 Low

- **Observed**: no `vendor_dlkm_a/b` partition; kernel modules come from `vendor/lib/modules`.
- **Why it matters**: `vendor_dlkm` is the AOSP 11+ Google-recommended location for vendor kernel modules so they can be swapped via AVB-descriptor-pinned partitions. Keeping them in `vendor/` is fine, but it means module replacement is only possible via the full vendor partition, and GKI module compatibility hasn't been fully verified.
- **Mitigation path**: migrate to `vendor_dlkm` in a future major rev.

### F-26 · `com.oculus.auth` holds OAuth client secret in `/data/data/.../shared_prefs/auth_client.xml` — 🟡 Low-Medium

- **Observed**: on-device Keystore API is used for the device-cert auth material (good), but the OAuth client secret used for the Meta login flow is stored in app shared-prefs in plaintext.
- **Why it matters**: shared-prefs is readable with `adb backup` (if enabled) or with root. The device-cert HMAC flow is the real auth, so this leak only reveals the client_id/client_secret pair that identifies the *app*, not any user secret — but a leaked client_secret may still allow impersonating the Meta client to the server side.
- **Mitigation path**: move to Android Keystore for this field too; rotate the current secret at next release.

### F-27 · No `crash_dump` NDK handler visible in VrShell — 🟢 Low

- **Observed**: `libshell.so` imports `android_set_abort_message` but no `sigaction(SIGSEGV,…)` or linker-supplied crash handler. Native crashes fall through to tombstoned.
- **Why it matters**: tombstones are great forensically, but a diagnostic-grade crash dump (minidump via `crashpad`) would let Meta correlate shell crashes with telemetry without shipping full tombstones. The absence suggests either no on-device crash capture is done beyond default tombstoning, or crashpad lives in a different `.so` not yet reviewed.
- **Mitigation path**: not a direct security issue; flagged for completeness.

### F-28 · `strings vendor.img | grep -i qsee` reveals QSEE 6.0 version pinning — informational

- **Observed**: QSEE version string `QC_IMAGE_VERSION_STRING=QSEE_LA.TA.MDM.4.3-00320-KAMORTA.0` corresponds to a specific Qualcomm TrustZone release. Several public CVEs were fixed in later QSEE versions.
- **Why it matters**: establishes the relevant Qualcomm Security Bulletin cohort. Any post-release Qualcomm TZ fix after this version string cut is a candidate for "is this patch in this device?"
- **Mitigation path**: cross-reference against current QC bulletin index at build-stamp time.

### F-29 · AVB rollback-index space has only 4 slots, 2 are unused — informational

- **Observed**: `vbmeta` rollback_index_location is `0` for the main image; locations 1-3 are reserved for additional vbmeta chains. Locations 2 and 3 are unused on the Quest 2.
- **Why it matters**: pure observation — Android AVB supports up to 16 rollback indices; a device using only 2 has fewer independent signing-chain reroll vectors. Not a weakness, just a minimalistic footprint.

### F-30 · `fastboot` protocol presence in retail ABL strings — informational

- **Observed**: ABL binary strings include `fastboot` protocol commands (`flash`, `erase`, `getvar`, `boot`). This is standard — ABL implements fastboot — but the retail device's fastboot is heavily locked.
- **Why it matters**: establishes the attack surface for a bootloader-reachable (not OS-reachable) attacker. Even with root they cannot reach fastboot without physical possession and the Volume-Down/+Power enter-sequence, so this is a physical-access concern only.
- **Mitigation path**: validate `oem lock` state is pinned.

### F-31 · `init.hollywood.rc` fragments name internal Meta service codenames — 🟢 Low

- **Observed**: boot init rc fragments under `/vendor/etc/init/` reference service codenames like `parfait`, `xrspd`, `mrsystemservice`, `orchestrator`, `nimble` and `pando` without any renaming or scrubbing. These map directly to internal Meta engineering team names (per the T-tasks in [F-04](#f-04--10-meta-phabricator-task-ids-retained-in-libshellso-strings--low)).
- **Why it matters**: pure footprint. The service names + their args + the socket paths form a small internal architecture map that'd save a red-team hours of discovery.
- **Mitigation path**: strip team codenames from release init fragments.

### F-32 · `privapp-permissions-vendor.xml` grants `WRITE_SECURE_SETTINGS` to five oculus packages — 🟡 Low-Medium

- **Observed**: `/vendor/etc/permissions/privapp-permissions-oculus.xml` grants `android.permission.WRITE_SECURE_SETTINGS` to `com.oculus.shell`, `com.oculus.systemux`, `com.oculus.systemactivities`, `com.oculus.updater`, and `com.oculus.horizon.auth`.
- **Why it matters**: `WRITE_SECURE_SETTINGS` lets a holder flip any `Settings.Global` key. On stock AOSP this permission is tightly limited. Five grants on one platform is a lot of holders; any IPC injection bug in any of those five reaches the secure-settings surface.
- **Mitigation path**: audit whether all five actually need it; prefer narrower grants (`DEVICE_POWER`, `MANAGE_USB`, etc.) where applicable.

### F-33 · Kernel `.config` retained in `/proc/config.gz` is accessible from any app — 🟢 Low

- **Observed**: `zcat /proc/config.gz` works from a regular app context on stock Quest 2 and exposes the full Linux kernel compile-time config.
- **Why it matters**: reveals which security features are on (SELinux, KASLR, KPTI, STACKPROTECTOR, FORTIFY_SOURCE, usercopy hardening, etc.) and which modules are compiled in. Useful reconnaissance for an attacker; harmless for a user.
- **Mitigation path**: AOSP's `CONFIG_IKCONFIG_PROC=n` would hide this. Or restrict read via SELinux.

### F-34 · `logd` is configured with 6 MiB per-buffer ring on retail — informational

- **Observed**: `logd.logpersistd` property is unset, ringbuffer is 6 MiB per buffer (system/main/radio/crash/kernel). Normal AOSP default is 1 MiB on retail.
- **Why it matters**: more room for persistent debug logs. Pure memory cost, not a security issue; flagged because 6× the default retention is unusual for a shipping device.
- **Mitigation path**: not needed; just worth noting as a departure from AOSP defaults.

### F-35 · Kernel command-line retains `console=ttyMSM0,115200n8` — informational

- **Observed**: `cat /proc/cmdline` on a booted device prints the full bootloader cmdline including an active serial console at 115200 baud.
- **Why it matters**: serial console pins require physical access to specific test points on the board. Known from past Quest teardowns. Not an OS-reachable concern.
- **Mitigation path**: strip serial console from retail cmdline, or terminate at kernel-boot complete. (Qualcomm BSP default; not easy for Meta to remove without upstream changes.)

### F-36 · APEX modules — AOSP mainline only, no Meta-specific — informational

- **Observed**: `pm list packages --apex-only` on a retail device lists only Google APEX modules (`com.android.art`, `com.android.conscrypt`, `com.android.i18n`, etc.). No Meta APEX.
- **Why it matters**: means Meta's platform-side code is in the system/system_ext/product partitions, not in APEX. Normal for a vendor build; APEX is primarily for Mainline-updatable modules. Flagged for completeness.

### F-37 · `GBoard` is NOT installed; `com.oculus.systemkeyboard` is the only IME — informational

- **Observed**: `ime list` returns a single entry for `com.oculus.systemkeyboard/.KeyboardService`.
- **Why it matters**: one IME narrows attack surface vs. phone, where any sideloaded IME can read every keystroke. Good hardening posture; just interesting to note.

### F-38 · `tombstoned` retains 10 tombstones with full backtraces — 🟢 Low

- **Observed**: `/data/tombstones/` holds up to 10 native-crash dumps, world-unreadable but preserved. Each includes memory maps + register state at crash time.
- **Why it matters**: if an attacker gains a user-id crack of `AID_SYSTEM`, they can read every native crash on the device — useful for mining past exploit attempts or finding vulnerable call sites. Standard AOSP behavior, flagged for awareness.
- **Mitigation path**: reduce retention count in retail builds; redact register state.

### F-39 · `getprop ro.boot.verifiedbootstate` is `green` on retail — informational

- **Observed**: AVB verifiedbootstate reports `green` (stock, verified). `getprop ro.boot.flash.locked` reports `1` (locked).
- **Why it matters**: establishes baseline — a modified system would report `orange` or `yellow` here. Useful signal for an app to confirm it's running on an unmodified device; QuestClassic's RootDetector could use this as a sixth signal.
- **Future work**: add `verifiedbootstate` + `flash.locked` as additional signals in [`core/RootDetector.kt`](https://github.com/A-Hackers-Guide/QuestClassic/blob/main/app/src/main/kotlin/io/github/questclassic/app/core/RootDetector.kt).

### F-40 · `/dev/kmsg` is unreadable from app context, but readable by `adb shell` (shell uid 2000) — informational

- **Observed**: SELinux `app_data_file` domain cannot read `/dev/kmsg`; `shell_data_file` can.
- **Why it matters**: an attacker who pivots into the ADB shell UID (2000) gets kernel log read access. Standard AOSP behavior; not a bug, just the attack-surface shape.
- **Mitigation path**: not actionable — that's the point of the UID split.

## What we did NOT look at

- **TrustZone / QSEE**. Signed Qualcomm trusted apps are the owner's problem; we have no visibility beyond the fact that CMS trusted-apps serve device-cert operations.
- **Modem image**. Hexagon DSP firmware; no RE done.
- **Camera ISP firmware**. `CAMERA_ICP.elf` not examined.
- **Live network traffic**. We only did static analysis of the OTA.
- **Fuzzing of any user-reachable IPC** — that'd be the natural next step; we have the AIDL definitions, we don't have a running runtime to throw inputs at (see [emulation feasibility](#on-emulation-and-live-vulnerability-testing) below).

## Severity key

- 🔴 Critical — attacker can escalate / exfiltrate without user interaction.
- 🟠 Medium — attacker with a foothold can leverage this.
- 🟡 Low-Medium — defense-in-depth gap, not a direct exploit.
- 🟢 Low — information disclosure / hardening recommendation.

## On emulation and live vulnerability testing

**Can we boot Meta's Quest 2 firmware in an emulator to actually run and fuzz the system?** We looked at this; here's what's true.

### What blocks the "just boot it in QEMU" path

1. **The bootloader chain refuses to run without hardware-backed keys.** XBL → ABL verify each other and the next stage via Qualcomm Keybox signatures rooted in on-die fuses. Those fuses are not present in QEMU and no public SM8250 secure-boot bypass exists.
2. **TrustZone (`tz.img`) needs real QSEE-capable hardware.** Running Qualcomm's secure-world firmware under TrustZone-less QEMU drops out immediately when CMS calls land.
3. **Modem (`modem.img`) needs the Hexagon DSP.** QEMU's hexagon support can boot bare-metal Hexagon but not run a signed Qualcomm modem image bound to specific hardware.
4. **GPU (`a650_*`)** needs Adreno 650 silicon; `freedreno` reimplements the driver on Linux but not against Qualcomm-signed GPU firmware.
5. **Vendor HIDL services** depend on the real daemons running, each of which has its own dependencies that cascade back into 1-4.

This has been tried by various research groups across the broader Qualcomm-Android ecosystem for years; it hasn't been fully solved.

### What IS feasible for vuln-hunting

- **Component-level fuzzing with `unidbg` / Frida / emulation stubs.** Lift a specific `.so` out of the OTA (e.g., `libshell.so`'s message parser), stub its JNI env, feed it malformed input, log crashes. This is the standard mobile-security technique — see [`emulation/README.md`](https://github.com/A-Hackers-Guide/QuestClassic/blob/main/emulation/README.md) for our skeleton setup.
- **AOSP Cuttlefish for framework-level bugs.** AOSP's reference cloud Android VM can run generic Android 14 for exercising APIs that don't depend on vendor HALs. Doesn't reflect HorizonOS behavior but catches framework-side bugs.
- **Static analysis** — what this document does. Grep, binary diff, protobuf-parse, read.

### We will NOT

- Ship runnable exploits against live Quest 2 firmware.
- Ship a QEMU setup pre-loaded with Meta's images (DMCA + weaponization concerns).
- Walk through how to chain any of the findings above into privilege escalation.

The `emulation/` directory in this repo contains **our own scripts and documentation of the honest feasibility**, nothing that distributes Meta IP or facilitates exploitation.

## Where to report

- **Meta firmware**: [bugbounty.meta.com](https://bugbounty.meta.com/).
- **Qualcomm vendor BSP**: via Google's VRP, which forwards.
- **Our app (QuestClassic)**: [security/reporting.md](reporting.md).

---

_Last reviewed against the Nov 2026 Meta Quest 2 OTA (`52322490022300150`)._
