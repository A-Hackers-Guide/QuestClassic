package io.github.questclassic.app.ui.nostalgic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.RootDetector
import io.github.questclassic.app.ui.theme.QCColors

/**
 * Honest "this requires root" explainer card.
 *
 * QuestClassic never ships an exploit. The user obtained root through some external means
 * (userdebug ROM, a signed vendor build, a Magisk install they performed themselves after
 * manually unlocking their bootloader on an older firmware). We *detect* that root, then unlock
 * the gated action — we don't *gain* root.
 *
 * The card is deliberately verbose about this. Users deserve to know the device constraint,
 * not get a grayed-out button.
 */
@Composable
fun RootGateCard(
    requiredFor: String,
    status: RootDetector.RootStatus,
    modifier: Modifier = Modifier,
) {
    val rooted = status.isRooted
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        accent = if (rooted) QCColors.OK else QCColors.Amber,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (rooted) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    null,
                    tint = if (rooted) QCColors.OK else QCColors.Amber,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (rooted) "Root detected — action unlocked"
                    else "Root required · action is a dry-run",
                    style = MaterialTheme.typography.titleMedium,
                    color = QCColors.Soft,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "This feature ($requiredFor) only acts when QuestClassic is running on a device " +
                    "where root has been enabled by other means — a userdebug build, a signed " +
                    "vendor image, or a Magisk install. QuestClassic does not jailbreak your " +
                    "headset; it uses privileges the device already grants.",
                style = MaterialTheme.typography.bodyMedium,
                color = QCColors.Dim,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Detection signals — su on PATH: ${status.suOnPath} · " +
                    "magisk paths: ${status.magiskPathsPresent} · " +
                    "system writable: ${status.systemWritable} · " +
                    "test-keys build: ${status.testKeysBuild} · " +
                    "userdebug/eng: ${status.debuggableBuild}",
                style = MaterialTheme.typography.labelMedium,
                color = QCColors.Dim,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Confidence: ${status.confidence.name}",
                style = MaterialTheme.typography.labelLarge,
                color = if (rooted) QCColors.OK else QCColors.Amber,
            )
        }
    }
}
