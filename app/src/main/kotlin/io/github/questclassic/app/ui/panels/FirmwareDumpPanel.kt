package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.ChipLabel
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.RootGateCard
import io.github.questclassic.app.ui.theme.QCColors

/**
 * "Optionally dump all firmware, keys, and more" (option 2 from the design brief).
 *
 * Realistic version: this panel lists every partition / file that a rooted shell could
 * legitimately read off the device, and what you'd get from it.
 *
 * What it will NOT do, even with root:
 *   - Extract the per-device signing / encryption keys from TrustZone — those never leave the
 *     secure world's address space. No rooting technique bypasses that, because the hardware
 *     fuse-rooted keybox is designed specifically against this.
 *   - Dump modem firmware — gated behind QSEE.
 *   - Dump Adreno GPU microcode — signed, loaded by a privileged service, memory never mapped
 *     to userspace.
 *
 * What it WILL do, given root:
 *   - `dd` the readable A/B partitions (boot, vendor_boot, dtbo, system, system_ext, vendor,
 *     product) into `/sdcard/Dump/`.
 *   - Export OEM certs from `/system/etc/security/`.
 *   - Export the public otacert from the standard AOSP path.
 *
 * Every one of these is reversible-engineerable off-device and goes nowhere near Meta's
 * private key material.
 */
@Composable
fun FirmwareDumpPanel(container: AppContainer, onClose: () -> Unit) {
    val root = remember { container.rootDetector.status() }
    val targets = remember { buildDumpTargets() }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Firmware Dump", "What's dumpable, what isn't, and why", onClose)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { RootGateCard("dumping readable partitions", root) }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth(), accent = QCColors.Amber) {
                    Column {
                        Text("NON-EXTRACTABLE (hardware-rooted)", style = MaterialTheme.typography.labelLarge, color = QCColors.Amber)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Meta's signing keys, the per-device AVB keys, the Qualcomm keybox, " +
                                "modem baseband firmware and Adreno microcode cannot be read " +
                                "even with full root. They live in TrustZone / on-die fuses. " +
                                "No amount of 'decompilation' reveals them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Dim,
                        )
                    }
                }
            }
            items(targets) { target ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(target.name, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                                Text(target.contents, style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
                            }
                            ChipLabel(target.verdict, when (target.verdict) {
                                "YES" -> QCColors.OK
                                "PARTIAL" -> QCColors.Amber
                                else -> QCColors.Warn
                            })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("$ ${target.command}", style = MaterialTheme.typography.labelMedium, color = QCColors.Teal)
                    }
                }
            }
        }
    }
}

private data class DumpTarget(
    val name: String,
    val contents: String,
    val verdict: String,   // YES | PARTIAL | NO
    val command: String,
)

private fun buildDumpTargets(): List<DumpTarget> = listOf(
    DumpTarget("boot_a / boot_b", "Linux kernel + ramdisk (AVB-signed, public)",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/boot_a of=/sdcard/Dump/boot_a.img bs=4M"),
    DumpTarget("vendor_boot_a / _b", "Vendor ramdisk + DTB",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/vendor_boot_a of=/sdcard/Dump/vendor_boot_a.img bs=4M"),
    DumpTarget("dtbo_a / _b", "Device-tree overlay for Quest 2 (hollywood)",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/dtbo_a of=/sdcard/Dump/dtbo_a.img bs=4M"),
    DumpTarget("system_a / _b (mounted)", "Android framework + oculus system apps, read-only",
        "YES",
        "tar cf /sdcard/Dump/system.tar /system"),
    DumpTarget("vendor_a / _b", "vendor HALs, vendor_boot.config, firmware/*",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/vendor_a of=/sdcard/Dump/vendor_a.img bs=4M"),
    DumpTarget("product_a / _b", "Meta-specific extras (store app, horizon)",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/product_a of=/sdcard/Dump/product_a.img bs=4M"),
    DumpTarget("system_ext_a / _b", "VrShell + oculus system_ext packages",
        "YES",
        "dd if=/dev/block/bootdevice/by-name/system_ext_a of=/sdcard/Dump/system_ext_a.img bs=4M"),
    DumpTarget("otacert (public cert)", "Meta's AOSP_Release_Pub-derived OTA cert",
        "YES",
        "cp /system/etc/security/otacerts.zip /sdcard/Dump/"),
    DumpTarget("AVB public keys", "Chain roots used by vbmeta",
        "YES",
        "cp /vendor/etc/avb/*.avbpubkey /sdcard/Dump/"),
    DumpTarget("modem_a / _b", "Qualcomm baseband firmware",
        "PARTIAL",
        "# raw partition dump reads; the running image lives in QSEE. Use for reversing, not patching."),
    DumpTarget("tz_a / _b", "TrustZone (QSEE) binary image",
        "PARTIAL",
        "# partition is readable; QSEE won't execute an unsigned replacement"),
    DumpTarget("abl_a / _b (bootloader)", "Android Bootloader (Little Kernel)",
        "PARTIAL",
        "# readable, not modifiable without fuse-blow"),
    DumpTarget("OEM PK (per-device)", "Private OEM signing key",
        "NO",
        "# fuse-rooted; not reachable from software"),
    DumpTarget("Keybox", "Qualcomm attestation keybox",
        "NO",
        "# in TrustZone, no software exfil"),
    DumpTarget("Avatar / biometric templates", "User biometric features",
        "NO",
        "# protected by Android's BiometricPrompt + QSEE"),
)
