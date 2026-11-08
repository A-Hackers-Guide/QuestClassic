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
 * "Optionally make developer mode have way too many permissions now" (option 4).
 *
 * Reality: Android's developer-mode settings are already exposed to ADB on a Meta-developer-
 * enabled headset. What this panel does is raise QuestClassic itself to developer-like
 * capabilities — enabling reflection into hidden APIs, lifting the SurfaceFlinger composition
 * trace, flipping system properties — but ONLY on a device that already grants these
 * capabilities (userdebug build, or root).
 *
 * Each toggle names the exact system property / settings key it would flip. On a stock device
 * each toggle is a documentation card. On a rooted device each toggle runs the write.
 */
@Composable
fun DevSuperPanel(container: AppContainer, onClose: () -> Unit) {
    val root = remember { container.rootDetector.status() }
    val toggles = remember { capabilityToggles() }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Developer Super-Mode", "What Android won't let you turn on by default", onClose)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { RootGateCard("writing restricted settings / props", root) }
            items(toggles) { t ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                                Text(t.subtitle, style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
                            }
                            ChipLabel(t.scope, when (t.scope) {
                                "SETTINGS" -> QCColors.Teal
                                "PROP" -> QCColors.Amber
                                else -> QCColors.Warn
                            })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(t.explain, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                        Spacer(Modifier.height(4.dp))
                        Text("$ ${t.command}", style = MaterialTheme.typography.labelMedium, color = QCColors.Teal)
                    }
                }
            }
        }
    }
}

private data class CapToggle(
    val title: String,
    val subtitle: String,
    val scope: String,     // SETTINGS | PROP | PKG
    val explain: String,
    val command: String,
)

private fun capabilityToggles(): List<CapToggle> = listOf(
    CapToggle("Allow sideload from any source", "Stock devices restrict to Meta's developer flow",
        "SETTINGS", "Loosens install-sources restriction.",
        "settings put global package_verifier_enable 0"),
    CapToggle("Full ADB over Wi-Fi without re-pair", "The usual flag needs re-auth every boot",
        "PROP",
        "Persists ADB auth. Convenience; not a sudo-equivalent.",
        "setprop persist.adb.tcp.port 5555"),
    CapToggle("Keep screen awake with charging only", "Compose previews / build demos",
        "SETTINGS", "Stay on while charging.",
        "settings put global stay_on_while_plugged_in 7"),
    CapToggle("Enable layout bounds", "Overlay layout grid on every app",
        "PROP", "Debug view for UI work.",
        "setprop debug.layout true && service call activity 1599295570"),
    CapToggle("Allow mock location providers", "For spoofing OpenXR room-scale anchors in test",
        "SETTINGS", "Dev-only.",
        "settings put global development_settings_enabled 1"),
    CapToggle("Lift SELinux to permissive (temporarily)", "You almost never want this long-term",
        "PROP", "Breaks the retail security policy. Reverse after your debugging session.",
        "setenforce 0"),
    CapToggle("Grant QuestClassic WRITE_SECURE_SETTINGS", "Lets QC flip any 'Global' setting directly",
        "PKG", "Equivalent to a manual `pm grant` after ADB.",
        "pm grant io.github.questclassic.app android.permission.WRITE_SECURE_SETTINGS"),
    CapToggle("Grant QuestClassic WRITE_SYSTEM_SETTINGS", "Lets QC write Settings.System keys",
        "PKG", "Needed for accent-color panels + brightness poke.",
        "pm grant io.github.questclassic.app android.permission.WRITE_SETTINGS"),
    CapToggle("Allow QuestClassic MANAGE_EXTERNAL_STORAGE", "Picking firmware zips from /sdcard/Download",
        "PKG", "Replaces the per-file picker with a directory walk.",
        "appops set io.github.questclassic.app MANAGE_EXTERNAL_STORAGE allow"),
    CapToggle("Disable Meta's device-reset enforcement", "Settings app sometimes nags back",
        "SETTINGS", "Pure-nuisance popup silencer. Can be reverted.",
        "settings put secure oculus_device_reset_warn 0"),
)
