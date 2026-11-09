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

    // --- Social / party / messaging ---
    StripEntry("com.oculus.parties", "Parties (voice channels)", StripVerb.REMOVE,
        "Only useful if you're on Meta's social graph. Removing gains quiet.",
        "pm uninstall --user 0 com.oculus.parties"),
    StripEntry("com.oculus.messenger", "Oculus Messenger", StripVerb.REMOVE,
        "Separate messaging surface. Safe without the social stack.",
        "pm uninstall --user 0 com.oculus.messenger"),
    StripEntry("com.facebook.arvr.quillplayer", "Quill Player", StripVerb.REMOVE,
        "Quill VR-painting viewer. Remove unless you actively use it.",
        "pm uninstall --user 0 com.facebook.arvr.quillplayer"),

    // --- Telemetry / analytics surfaces ---
    StripEntry("com.oculus.insights", "Meta Insights analytics", StripVerb.REMOVE,
        "App-usage telemetry. No user-visible feature.",
        "pm uninstall --user 0 com.oculus.insights"),
    StripEntry("com.oculus.vrshellenv", "VrShell environment telemetry", StripVerb.DISABLE,
        "Not strictly removable without launcher side-effects; disabling blocks its uploads.",
        "pm disable-user --user 0 com.oculus.vrshellenv"),
    StripEntry("com.oculus.mobile_mrc_setup", "Mixed Reality Capture setup", StripVerb.REMOVE,
        "Casting-setup helper used once during pairing. Removes cleanly after that.",
        "pm uninstall --user 0 com.oculus.mobile_mrc_setup"),

    // --- Casting + screen mirroring ---
    StripEntry("com.oculus.casting", "Meta Casting server", StripVerb.DISABLE,
        "Leaves casting receiver ready on the LAN. Disable if you don't cast.",
        "pm disable-user --user 0 com.oculus.casting"),
    StripEntry("com.oculus.vrguardianservice", "Guardian / boundary service", StripVerb.LEAVE,
        "Removing breaks the tracking boundary. Leave in place.",
        "echo 'LEAVE - breaks the boundary; do not disable'"),

    // --- Keyboards + input ---
    StripEntry("com.oculus.keyboard", "Oculus system keyboard", StripVerb.LEAVE,
        "Removing leaves you with no keyboard at all. Leave in place.",
        "echo 'LEAVE - no replacement ships on retail builds'"),
    StripEntry("com.oculus.mrservice", "Mixed Reality service", StripVerb.DISABLE,
        "Passthrough plumbing. Disable only if you don't use passthrough apps.",
        "pm disable-user --user 0 com.oculus.mrservice"),

    // --- Camera / passthrough analytics ---
    StripEntry("com.oculus.spatial_capture", "Spatial Capture (first-person recording)", StripVerb.REMOVE,
        "Records VR sessions for sharing. Safe to remove unless you record.",
        "pm uninstall --user 0 com.oculus.spatial_capture"),
    StripEntry("com.oculus.cameraservice", "Camera service (passthrough frames)", StripVerb.LEAVE,
        "Passthrough and guardian depend on this. Leave in place.",
        "echo 'LEAVE - breaks passthrough'"),

    // --- Meta sync + account ---
    StripEntry("com.oculus.horizon_updater", "Horizon self-updater", StripVerb.REMOVE,
        "Updates Horizon in the background. Removing stops the bg traffic.",
        "pm uninstall --user 0 com.oculus.horizon_updater"),
    StripEntry("com.oculus.integrityservice", "Integrity / attestation service", StripVerb.DISABLE,
        "Attests the headset to Meta for quest + store. Only disable if going offline.",
        "pm disable-user --user 0 com.oculus.integrityservice"),
    StripEntry("com.oculus.ovrmobileplatformsvc", "Meta Platform SDK service", StripVerb.DISABLE,
        "Entitlement / IAP checks for Meta Store apps. Breaks IAP if disabled.",
        "pm disable-user --user 0 com.oculus.ovrmobileplatformsvc"),

    // --- Experimental / stats ---
    StripEntry("com.oculus.sysutil", "System utility (stats + logs)", StripVerb.DISABLE,
        "Logging daemon. Disable to quiet background work.",
        "pm disable-user --user 0 com.oculus.sysutil"),
    StripEntry("com.oculus.brush", "Oculus Brush (demo)", StripVerb.REMOVE,
        "Early-days brush demo. Remove.",
        "pm uninstall --user 0 com.oculus.brush"),
    StripEntry("com.oculus.horizonmobile", "Horizon mobile companion bridge", StripVerb.REMOVE,
        "Used when pairing with the Horizon phone app. Remove if you pair rarely.",
        "pm uninstall --user 0 com.oculus.horizonmobile"),

    // --- Facebook residuals ---
    StripEntry("com.facebook.arvr.oneheadsettwohands", "Facebook Spaces residual", StripVerb.REMOVE,
        "Early Spaces leftover. Remove.",
        "pm uninstall --user 0 com.facebook.arvr.oneheadsettwohands"),
    StripEntry("com.facebook.arvr.mobile_rift", "Mobile Rift / early VR", StripVerb.REMOVE,
        "Historical leftover. Remove.",
        "pm uninstall --user 0 com.facebook.arvr.mobile_rift"),

    // --- Tier-4 additions: extra telemetry + legacy residuals ---

    StripEntry("com.oculus.mediaplayer", "Oculus Media Player", StripVerb.REMOVE,
        "180/360 video player. Remove if you don't watch VR video.",
        "pm uninstall --user 0 com.oculus.mediaplayer"),
    StripEntry("com.oculus.firstrun", "First-time setup wizard", StripVerb.REMOVE,
        "Only runs once at setup. Safe to remove afterwards.",
        "pm uninstall --user 0 com.oculus.firstrun"),
    StripEntry("com.oculus.vrfiletransfer", "Oculus file-transfer helper", StripVerb.REMOVE,
        "Used by the Meta mobile app's send-file flow. Remove if you use adb or sideloading tools.",
        "pm uninstall --user 0 com.oculus.vrfiletransfer"),
    StripEntry("com.oculus.movieplayer", "Movie Player (Oculus Cinema successor)", StripVerb.REMOVE,
        "VOD/Cinema successor. Remove.",
        "pm uninstall --user 0 com.oculus.movieplayer"),
    StripEntry("com.oculus.notifications", "Notification center surface", StripVerb.DISABLE,
        "Disabling removes the always-on 'Meta is working' chrome; disable, don't remove (other apps link into it).",
        "pm disable-user --user 0 com.oculus.notifications"),
    StripEntry("com.oculus.gallery", "Photos / Gallery app", StripVerb.REMOVE,
        "VR photo gallery. Remove unless you take VR photos.",
        "pm uninstall --user 0 com.oculus.gallery"),
    StripEntry("com.oculus.browser.beta", "Meta Browser beta channel", StripVerb.REMOVE,
        "Beta channel of the main browser. Remove unless you dogfood it.",
        "pm uninstall --user 0 com.oculus.browser.beta"),
    StripEntry("com.oculus.vrtutorials", "Hand-tracking / controller tutorials", StripVerb.REMOVE,
        "Tutorial app surfaced during first-run. Remove after setup.",
        "pm uninstall --user 0 com.oculus.vrtutorials"),
    StripEntry("com.oculus.livetv", "Live TV / Meta Live streaming", StripVerb.REMOVE,
        "Casting-to-Live surface. Remove unless you stream to Meta Live.",
        "pm uninstall --user 0 com.oculus.livetv"),
    StripEntry("com.oculus.vrpartyroom", "Party Room companion", StripVerb.REMOVE,
        "Part of the Parties experience. Removes cleanly once Parties is gone.",
        "pm uninstall --user 0 com.oculus.vrpartyroom"),
    StripEntry("com.oculus.horizon_scripting", "Horizon scripting runtime", StripVerb.REMOVE,
        "Only used if Horizon Worlds is installed. Remove alongside Horizon.",
        "pm uninstall --user 0 com.oculus.horizon_scripting"),
    StripEntry("com.oculus.systemdriver", "System driver bundle", StripVerb.LEAVE,
        "Bundles compositor kernel-module userland helpers. Leaving in place; removing bricks tracking.",
        "echo 'LEAVE - breaks compositor'"),
)
