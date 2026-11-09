package io.github.questclassic.app.ui.nostalgic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.panels.AIAssistantPanel
import io.github.questclassic.app.ui.panels.ARSemicirclePanel
import io.github.questclassic.app.ui.panels.BootAnimationPanel
import io.github.questclassic.app.ui.panels.BuildToolsPanel
import io.github.questclassic.app.ui.panels.DevSuperPanel
import io.github.questclassic.app.ui.panels.DiagnosticsPanel
import io.github.questclassic.app.ui.panels.FirmwareDumpPanel
import io.github.questclassic.app.ui.panels.HomeMenuSwapperPanel
import io.github.questclassic.app.ui.panels.LauncherPanel
import io.github.questclassic.app.ui.panels.MetaHostsAuditPanel
import io.github.questclassic.app.ui.panels.RestorePanel
import io.github.questclassic.app.ui.panels.SettingsPanel
import io.github.questclassic.app.ui.panels.SignatureVerifierPanel
import io.github.questclassic.app.ui.panels.StripMetaPanel
import io.github.questclassic.app.ui.panels.SystemInternalsPanel
import io.github.questclassic.app.ui.panels.UpdatePanel
import io.github.questclassic.app.ui.theme.QCColors

/**
 * The home surface. Six "tile" panels — tap one to open; close returns here (back button or
 * a tile close affordance). Deliberately a flat navigation model so the UX feels snappy on the
 * head.
 */
enum class HomeScreen {
    HOME, LAUNCHER, DIAGNOSTICS, AI, BUILD, UPDATE, SETTINGS, SIG_VERIFY, INTERNALS, META_HOSTS,
    STRIP_META, FIRMWARE_DUMP, BOOT_ANIM, DEV_SUPER, HOME_SWAP, AR_SEMICIRCLE, RESTORE,
    VIRTUAL_MONITORS,
}

@Composable
fun NostalgicHomeScreen(container: AppContainer) {
    var screen by remember { mutableStateOf(HomeScreen.HOME) }
    val bg = Brush.verticalGradient(listOf(QCColors.Space, QCColors.Deep))
    Box(Modifier.fillMaxSize().background(bg).padding(24.dp)) {
        when (screen) {
            HomeScreen.HOME -> HomeGrid(onOpen = { screen = it })
            HomeScreen.LAUNCHER -> LauncherPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.DIAGNOSTICS -> DiagnosticsPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.AI -> AIAssistantPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.BUILD -> BuildToolsPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.UPDATE -> UpdatePanel(container) { screen = HomeScreen.HOME }
            HomeScreen.SETTINGS -> SettingsPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.SIG_VERIFY -> SignatureVerifierPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.INTERNALS -> SystemInternalsPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.META_HOSTS -> MetaHostsAuditPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.STRIP_META -> StripMetaPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.FIRMWARE_DUMP -> FirmwareDumpPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.BOOT_ANIM -> BootAnimationPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.DEV_SUPER -> DevSuperPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.HOME_SWAP -> HomeMenuSwapperPanel(container) { screen = HomeScreen.HOME }
            HomeScreen.AR_SEMICIRCLE -> ARSemicirclePanel(container) { screen = HomeScreen.HOME }
            HomeScreen.RESTORE -> RestorePanel(container) { screen = HomeScreen.HOME }
            HomeScreen.VIRTUAL_MONITORS -> VirtualMonitorPanel(container) { screen = HomeScreen.HOME }
        }
    }
}

@Composable
private fun HomeGrid(onOpen: (HomeScreen) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // Masthead
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("QUESTCLASSIC", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Shelf",
                    style = MaterialTheme.typography.displayLarge,
                    color = QCColors.Soft,
                )
            }
            ChipLabel("v0.1.0", QCColors.Amber)
        }
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(HomeTile.ALL) { tile ->
                TileButton(
                    icon = tile.icon,
                    title = tile.title,
                    description = tile.desc,
                    onClick = { onOpen(tile.screen) },
                )
            }
        }
    }
}

private data class HomeTile(val screen: HomeScreen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val desc: String) {
    companion object {
        val ALL = listOf(
            HomeTile(HomeScreen.LAUNCHER, Icons.Outlined.Apps, "Launcher", "Every app on the headset, retro grid."),
            HomeTile(HomeScreen.DIAGNOSTICS, Icons.Outlined.MonitorHeart, "Diagnostics", "Self-tests. Fails buzz the controllers in code."),
            HomeTile(HomeScreen.AI, Icons.Outlined.Chat, "AI Companion", "Name it. Pick a model. Give it a mission."),
            HomeTile(HomeScreen.BUILD, Icons.Outlined.Build, "Build Tools", "Compose your own HUD out of widgets."),
            HomeTile(HomeScreen.UPDATE, Icons.Outlined.CloudDownload, "Updates", "Self-update from our own release channel."),
            HomeTile(HomeScreen.SETTINGS, Icons.Outlined.Info, "About / System", "What the headset thinks it is."),
            HomeTile(HomeScreen.SIG_VERIFY, Icons.Outlined.Verified, "Signature Verifier", "Verify a Meta OTA .zip against its own cert, on-device."),
            HomeTile(HomeScreen.INTERNALS, Icons.Outlined.Policy, "System Internals", "Build host, vendor SPL gap, HorizonOS branch, SoC, all the quirks."),
            HomeTile(HomeScreen.META_HOSTS, Icons.Outlined.Public, "Meta Hosts Audit", "Resolve + ping every Meta host this build talks to."),
            HomeTile(HomeScreen.STRIP_META, Icons.Outlined.DeleteSweep, "Strip Meta", "Dry-run plan for debloating. Root-gated for the destructive action."),
            HomeTile(HomeScreen.FIRMWARE_DUMP, Icons.Outlined.DownloadForOffline, "Firmware Dump", "What's dumpable, what isn't, and why the keys stay put."),
            HomeTile(HomeScreen.BOOT_ANIM, Icons.Outlined.Movie, "Boot Animation", "Swap the Meta splash for a nostalgia animation. Root-gated."),
            HomeTile(HomeScreen.DEV_SUPER, Icons.Outlined.Rocket, "Dev Super-Mode", "Flip the switches Android normally hides. Root-gated."),
            HomeTile(HomeScreen.HOME_SWAP, Icons.Outlined.Home, "Home Swapper", "Choose what launches when you take off standby. Root-gated."),
            HomeTile(HomeScreen.AR_SEMICIRCLE, Icons.Outlined.ViewInAr, "AR Semicircle", "The 90° hand menu — flat preview + 2D fallback."),
            HomeTile(HomeScreen.RESTORE, Icons.Outlined.Healing, "Restore", "The 30s 'anything wrong?' timer and escalation ladder."),
            HomeTile(HomeScreen.VIRTUAL_MONITORS, Icons.Outlined.DesktopWindows, "Virtual Monitors", "Pair a desktop and stream virtual monitors to it."),
        )
    }
}
