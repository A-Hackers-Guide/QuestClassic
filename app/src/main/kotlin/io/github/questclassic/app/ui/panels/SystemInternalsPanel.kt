package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.StatRow
import io.github.questclassic.app.ui.theme.QCColors

/**
 * Surfaces every post-Meta-acquisition fingerprint and quirk our reverse-engineering turned up,
 * **live** on the headset. All reads are via android.os.Build + SystemProperties (getprop) — no
 * elevated privileges. This panel is the "look under the hood" view the stock Settings app never
 * exposes.
 *
 * Findings this panel foregrounds (see the parent project's REVERSE_ENGINEERING_REPORT.md):
 *   1. Per-partition `ro.*.build.fingerprint` strings still carry Meta's internal build-host
 *      name (e.g. `3c64-a274-….twshared10220.29.lla2.tw.fbinfra.net`), even though the top-line
 *      `post-build` fingerprint is scrubbed.
 *   2. `ro.vendor.build.security_patch` is often years behind the device-wide patch level —
 *      common Qualcomm-vendor anti-pattern, but worth flagging.
 *   3. HorizonOS branch (`ro.build.branch = releases-oculus-14.0-vN`) is the real OS version;
 *      Android's version field is just the AOSP base.
 *   4. `ro.oculus.reality_mode = mr` is set to `mr` on Quest 2 (mixed reality) regardless of
 *      whether MR is active.
 *   5. `ro.recovery.usb.vid = 2833` is Meta's USB VID.
 */
@Composable
fun SystemInternalsPanel(container: AppContainer, onClose: () -> Unit) {
    val probe = container.systemProbe

    Column(Modifier.fillMaxSize()) {
        PanelClose(title = "System Internals", subtitle = probe.summary, onClose = onClose)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("OS LINEAGE", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        StatRow("product model", probe.get("ro.product.system.model") ?: probe.get("ro.product.model") ?: "?")
                        StatRow("manufacturer", probe.get("ro.product.system.manufacturer") ?: "?")
                        StatRow("codename", probe.get("ro.product.system.device") ?: "?")
                        StatRow("HorizonOS branch", probe.get("ro.build.branch") ?: "?")
                        StatRow("Android base", probe.get("ro.build.version.release") ?: "?")
                        StatRow("SDK", probe.get("ro.build.version.sdk") ?: "?")
                        StatRow("first API level", probe.get("ro.product.first_api_level") ?: "?")
                        StatRow("VNDK version", probe.get("ro.vndk.version") ?: "?")
                        StatRow("reality mode", probe.get("ro.oculus.reality_mode") ?: "—")
                    }
                }
            }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("SECURITY PATCH GAP", style = MaterialTheme.typography.labelLarge, color = QCColors.Amber)
                        Spacer(Modifier.height(6.dp))
                        val devicePatch = probe.get("ro.build.version.security_patch") ?: "?"
                        val vendorPatch = probe.get("ro.vendor.build.security_patch") ?: "?"
                        val gap = devicePatch != vendorPatch
                        StatRow("device-wide SPL", devicePatch, ok = !gap)
                        StatRow("vendor partition SPL", vendorPatch, ok = !gap)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (gap)
                                "⚠ Vendor SPL is older than the user-facing one. Qualcomm BSP patches between those dates may not be in this build."
                            else
                                "Vendor and device SPLs match — no visible gap.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (gap) QCColors.Amber else QCColors.Dim,
                        )
                    }
                }
            }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("BUILD-HOST LEAK", style = MaterialTheme.typography.labelLarge, color = QCColors.Amber)
                        Spacer(Modifier.height(6.dp))
                        val host = probe.get("ro.build.host") ?: "—"
                        val fp = probe.get("ro.system.build.fingerprint") ?: probe.get("ro.build.fingerprint") ?: "—"
                        val leaked = host.contains("fbinfra") || host.contains("twshared") || fp.contains("fbinfra")
                        StatRow("ro.build.host", host, ok = !leaked)
                        Spacer(Modifier.height(6.dp))
                        Text("ro.system.build.fingerprint", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                        Text(fp, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
                        Spacer(Modifier.height(6.dp))
                        if (leaked) Text(
                            "⚠ Internal Meta build-host leaked into the fingerprint — see RE report §2.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Amber,
                        )
                    }
                }
            }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("SOC + GPU", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        StatRow("SoC manufacturer", probe.get("ro.soc.manufacturer") ?: "?")
                        StatRow("SoC model", probe.get("ro.soc.model") ?: "?")
                        StatRow("hardware.egl", probe.get("ro.hardware.egl") ?: "?")
                        StatRow("hardware.vulkan", probe.get("ro.hardware.vulkan") ?: "?")
                        StatRow("CPU variant", probe.get("ro.bionic.cpu_variant") ?: "?")
                        StatRow("ABIs", probe.get("ro.product.cpu.abilist") ?: "?")
                        StatRow("A/B updates", probe.get("ro.build.ab_update") ?: "?")
                        StatRow("dynamic partitions", probe.get("ro.boot.dynamic_partitions") ?: "?")
                        StatRow("virtual A/B", probe.get("ro.virtual_ab.enabled") ?: "?")
                    }
                }
            }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("USB + RECOVERY", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        StatRow("USB VID", probe.get("ro.recovery.usb.vid") ?: "—")
                        StatRow("ADB PID", probe.get("ro.recovery.usb.adb.pid") ?: "—")
                        StatRow("Fastboot PID", probe.get("ro.recovery.usb.fastboot.pid") ?: "—")
                        StatRow("Zygote", probe.get("ro.zygote") ?: "?")
                    }
                }
            }
        }
    }
}
