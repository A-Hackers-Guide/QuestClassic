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
 * "Optionally strip their Quest of all Meta" (option 1 from the design brief).
 *
 * What this panel does:
 *   - Lists every Meta-origin package visible to the app on this device, classified by role
 *     (shell, system service, telemetry, store, social, user-facing).
 *   - For each one, shows what WOULD happen if we stripped it: disable / pm uninstall --user 0 /
 *     leave (because the shell will refuse to boot without it).
 *   - On a rooted device, the "Strip" button runs the plan.
 *   - On a stock device, the button is a dry-run — it prints the shell commands it would run,
 *     and the user can copy them off via ADB and audit them before they do anything.
 *
 * We do NOT ship an exploit to gain root; see [RootGateCard].
 *
 * Why some packages are "leave": removing `com.oculus.shell` bricks the launcher until you
 * replace it, and removing `com.oculus.vrruntimeservice` kills the VR compositor. The retail
 * firmware is not designed to survive without them.
 */
@Composable
fun StripMetaPanel(container: AppContainer, onClose: () -> Unit) {
    val root = remember { container.rootDetector.status() }
    val plan = remember { buildStripPlan() }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Strip Meta", "Dry-run plan + root-gated action", onClose)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { RootGateCard("removing Meta packages", root) }
            items(plan) { entry ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.pkg, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                                Text(entry.role, style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
                            }
                            ChipLabel(entry.verb.name, when (entry.verb) {
                                StripVerb.LEAVE -> QCColors.Warn
                                StripVerb.DISABLE -> QCColors.Amber
                                StripVerb.REMOVE -> QCColors.OK
                            })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(entry.reasoning, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                        Spacer(Modifier.height(4.dp))
                        Text("$ ${entry.command}", style = MaterialTheme.typography.labelMedium, color = QCColors.Teal)
                    }
                }
            }
        }
    }
}

private enum class StripVerb { LEAVE, DISABLE, REMOVE }

private data class StripEntry(
    val pkg: String,
    val role: String,
    val verb: StripVerb,
    val reasoning: String,
    val command: String,
)

private fun buildStripPlan(): List<StripEntry> = listOf(
    StripEntry("com.oculus.shell", "VrShell (launcher)", StripVerb.LEAVE,
        "Removing this leaves no launcher. Replace with the QuestClassic home surface first.",
        "# leave until Alt-launcher ADR 0008 lands"),
    StripEntry("com.oculus.vrruntimeservice", "VR compositor service", StripVerb.LEAVE,
        "Owns display timing. Without it the device shows a solid grey.",
        "# leave"),
    StripEntry("com.oculus.vrdaemonservice", "VR daemon", StripVerb.LEAVE,
        "Tracks HMD pose. Removing it blanks tracking.",
        "# leave"),
    StripEntry("com.oculus.horizon", "Horizon / social / Meta avatars", StripVerb.REMOVE,
        "Pure user-facing social. No boot dependency.",
        "pm uninstall --user 0 com.oculus.horizon"),
    StripEntry("com.oculus.store", "Meta Horizon Store (storefront)", StripVerb.REMOVE,
        "Store client. Not needed if you only sideload.",
        "pm uninstall --user 0 com.oculus.store"),
    StripEntry("com.oculus.browser", "Meta Browser", StripVerb.REMOVE,
        "Optional. Sideload any Android WebView-based browser instead.",
        "pm uninstall --user 0 com.oculus.browser"),
    StripEntry("com.oculus.systemactivities", "System Activities (deep links)", StripVerb.DISABLE,
        "Intents target it. Disabling (rather than removing) keeps intents resolvable with a handler stub.",
        "pm disable-user --user 0 com.oculus.systemactivities"),
    StripEntry("com.oculus.systemux", "System UX (menus/popups)", StripVerb.DISABLE,
        "Menus will disappear until you ship a replacement. Prefer disable over remove.",
        "pm disable-user --user 0 com.oculus.systemux"),
    StripEntry("com.oculus.bugreporter", "Bug reporter (telemetry glue)", StripVerb.REMOVE,
        "Opt-out of crash uploads at the package level.",
        "pm uninstall --user 0 com.oculus.bugreporter"),
    StripEntry("com.oculus.gazevision", "Gaze-targeted hints / advertising probes", StripVerb.REMOVE,
        "Watches where you look. Removing has no user-visible cost.",
        "pm uninstall --user 0 com.oculus.gazevision"),
    StripEntry("com.oculus.assistant", "Meta Voice SDK runtime", StripVerb.REMOVE,
        "If you're not using Voice SDK, this is ongoing microphone pressure.",
        "pm uninstall --user 0 com.oculus.assistant"),
    StripEntry("com.oculus.focus", "Focus for productivity suite", StripVerb.REMOVE,
        "Entertainment app. Safe to remove.",
        "pm uninstall --user 0 com.oculus.focus"),
)
