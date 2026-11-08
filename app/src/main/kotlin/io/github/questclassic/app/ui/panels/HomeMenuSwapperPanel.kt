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
 * "Optionally add new home menus" (option 5).
 *
 * Android's launcher resolution is `android.intent.category.HOME`. On a non-Meta Android device
 * the user picks a default launcher at first long-press on "home"; on retail Quest 2 the setting
 * is hard-wired to `com.oculus.shell`. On a rooted device, overriding the preferred-home resolver
 * is a single `cmd package set-home-activity` call.
 *
 * This panel lists the candidate "home surfaces" — QuestClassic's own nostalgic home, bundled
 * alternates, and any third-party HOME-category activity the package manager discovers.
 */
@Composable
fun HomeMenuSwapperPanel(container: AppContainer, onClose: () -> Unit) {
    val root = remember { container.rootDetector.status() }
    val candidates = remember { candidateHomes() }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Home Menu Swapper", "Pick what the headset lands on when you take it off standby", onClose)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { RootGateCard("setting the system HOME activity", root) }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("HOW IT WORKS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Android resolves the `android.intent.category.HOME` intent to decide " +
                                "what runs when the compositor wakes. Meta's shell is normally " +
                                "the only resolver that passes their filter. QuestClassic " +
                                "registers one too (manifest: LAUNCHER + oculus.intent.category.VR). " +
                                "With root, `cmd package set-home-activity` changes the resolution.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Dim,
                        )
                    }
                }
            }
            items(candidates) { c ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                                Text(c.pkg, style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
                            }
                            ChipLabel(c.tag, when (c.tag) {
                                "QC" -> QCColors.Teal
                                "META" -> QCColors.Amber
                                else -> QCColors.OK
                            })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(c.rationale, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                        Spacer(Modifier.height(4.dp))
                        Text("$ ${c.command}", style = MaterialTheme.typography.labelMedium, color = QCColors.Teal)
                    }
                }
            }
        }
    }
}

private data class HomeCandidate(
    val title: String,
    val pkg: String,
    val tag: String,          // QC | META | 3P
    val rationale: String,
    val command: String,
)

private fun candidateHomes(): List<HomeCandidate> = listOf(
    HomeCandidate("QuestClassic — nostalgic home", "io.github.questclassic.app/.MainActivity", "QC",
        "This app's own launcher surface. Launches instantly, no network.",
        "cmd package set-home-activity io.github.questclassic.app/.MainActivity"),
    HomeCandidate("QuestClassic — AR home", "io.github.questclassic.app/.ar.ARHomeActivity", "QC",
        "Launches straight into passthrough with the semicircle hand menu.",
        "cmd package set-home-activity io.github.questclassic.app/.ar.ARHomeActivity"),
    HomeCandidate("Meta VrShell", "com.oculus.shell/.phoenix.ShellActivity", "META",
        "Stock shell (requires network for the horizon feed).",
        "cmd package set-home-activity com.oculus.shell/.phoenix.ShellActivity"),
    HomeCandidate("Quill (if installed)", "com.oculus.quillplayer", "3P",
        "Launch into Quill when the headset wakes — 'always-on canvas' setup.",
        "cmd package set-home-activity com.oculus.quillplayer/.MainActivity"),
    HomeCandidate("sideloaded ArcBrowser launcher", "com.arcturus.arclauncher", "3P",
        "Any user-installed HOME-category activity is a candidate here.",
        "cmd package set-home-activity com.arcturus.arclauncher/.HomeActivity"),
)
