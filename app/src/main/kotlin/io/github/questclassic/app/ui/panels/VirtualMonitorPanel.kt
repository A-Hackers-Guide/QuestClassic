package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.VirtualMonitorEstimator
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors

/**
 * Virtual Monitors panel (M0 skeleton).
 *
 * Headset-side surface for the QuestClassic Companion desktop app. Today it
 * only shows status + a brief explanation; once the companion lands its M1
 * milestone (LAN discovery + handshake), this panel will:
 *
 *  1. Scan mDNS for `_questclassic._tcp` companions on the LAN
 *  2. Show a connect card per companion found, with hostname + OS
 *  3. After connecting, show per-display thumbnails + add/remove controls
 *  4. Route Quest controller rays to the companion as mouse_move INPUT events
 *
 * The protocol spec lives in `companion/PROTOCOL.md`. The per-OS display
 * drivers and input injectors are tracked as individual follow-up issues.
 */
@Composable
fun VirtualMonitorPanel(container: AppContainer, onClose: () -> Unit) {
    // TODO(M1): replace with a real mDNS scanner that returns Companion rows.
    val companions = remember { mutableStateOf(emptyList<CompanionEntry>()) }
    var scanning by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Virtual Monitors",
            subtitle = "M0 skeleton · companion required",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "OVERVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    color = QCColors.Teal,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stream up to 4 virtual monitors wirelessly to a paired " +
                        "Mac / Windows / Linux / FreeBSD host running the " +
                        "QuestClassic Companion app. The headset handles " +
                        "the window geometry; the companion owns the " +
                        "per-OS virtual-display driver and routes your " +
                        "controller as mouse + keyboard input.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QCColors.Soft,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Install the companion first: see companion/README.md " +
                        "in the repo. Then come back here and tap Scan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Per-SoC estimate — shows the user what this headset can realistically hold.
        val estimate = remember {
            VirtualMonitorEstimator.estimate(android.os.Build.HARDWARE, android.os.Build.BOARD)
        }
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "YOUR HEADSET",
                    style = MaterialTheme.typography.labelLarge,
                    color = QCColors.Teal,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tier ${estimate.tier.name} · realistic ${estimate.realistic} · cap ${estimate.hardCap}",
                    style = MaterialTheme.typography.titleMedium,
                    color = QCColors.Soft,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    estimate.humanNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { scanning = true /* TODO(M1): run mDNS scan */ },
                enabled = !scanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Teal,
                    contentColor = QCColors.Space,
                ),
            ) { Text(if (scanning) "Scanning…" else "Scan LAN") }

            Button(
                onClick = { companions.value = emptyList(); scanning = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) { Text("Clear") }
        }
        Spacer(Modifier.height(16.dp))

        if (companions.value.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        "NO COMPANION FOUND",
                        style = MaterialTheme.typography.labelLarge,
                        color = QCColors.Dim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "This is expected on the current skeleton build. " +
                            "mDNS scanning lands in the companion's M1 " +
                            "milestone. Track it in the GitHub issue linked " +
                            "from companion/ROADMAP.md.",
                        style = MaterialTheme.typography.bodySmall,
                        color = QCColors.Soft,
                    )
                }
            }
        } else {
            companions.value.forEach { c ->
                Spacer(Modifier.height(8.dp))
                CompanionRow(c)
            }
        }
    }
}

/**
 * A discovered QuestClassic Companion on the LAN. Populated by the mDNS
 * scanner in M1.
 */
data class CompanionEntry(
    val hostname: String,
    val address: String,
    val port: Int,
    val hostOs: String,
    val maxDisplays: Int,
)

@Composable
private fun CompanionRow(entry: CompanionEntry) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(entry.hostname, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft)
            Text(
                "${entry.hostOs} · ${entry.address}:${entry.port} · up to ${entry.maxDisplays} displays",
                style = MaterialTheme.typography.bodySmall,
                color = QCColors.Dim,
            )
        }
    }
}
