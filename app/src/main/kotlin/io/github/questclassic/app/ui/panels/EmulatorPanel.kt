package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.EmulatorRegistry
import io.github.questclassic.app.core.EmulatorTarget
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors

/**
 * Emulator Launcher panel — detects installed RetroArch + standalone emulators
 * and routes "play this system" to whichever is present.
 *
 * Nothing is bundled. See [EmulatorRegistry.LicenceNote] for the licence
 * posture that forced this detect-and-launch model instead of a monolithic
 * all-in-one app.
 */
@Composable
fun EmulatorPanel(container: AppContainer, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf<List<Pair<EmulatorTarget, Boolean>>>(emptyList()) }
    var showMissing by remember { mutableStateOf(false) }

    // Refresh on every open — users install/uninstall emulators outside this app.
    LaunchedEffect(Unit) {
        items = EmulatorRegistry.ALL.map { it to EmulatorRegistry.isInstalled(ctx, it) }
    }

    val installed = items.filter { it.second }
    val missing = items.filterNot { it.second }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Emulators",
            subtitle = "${installed.size} installed · ${missing.size} available",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    "DETECT + LAUNCH ONLY",
                    style = MaterialTheme.typography.labelLarge,
                    color = QCColors.Teal,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "This panel detects emulators you've already installed and " +
                        "hands you a launch button for each. Nothing is bundled " +
                        "here — the GPL licences on Dolphin / PCSX2 / PPSSPP / " +
                        "Snes9x / DuckStation would conflict in one binary. " +
                        "Install RetroArch for the bulk of systems; add standalone " +
                        "apps for anything RetroArch's core doesn't cover.",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Soft,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Row {
            Button(
                onClick = { showMissing = !showMissing },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) {
                Text(if (showMissing) "Hide uninstalled" else "Show uninstalled")
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = {
                    items = EmulatorRegistry.ALL.map { it to EmulatorRegistry.isInstalled(ctx, it) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) { Text("Refresh") }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (installed.isEmpty()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "NO EMULATORS INSTALLED YET",
                                style = MaterialTheme.typography.labelLarge,
                                color = QCColors.Dim,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Start with RetroArch (one install → dozens of systems). " +
                                    "Tap 'Show uninstalled' to see the full list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = QCColors.Soft,
                            )
                        }
                    }
                }
            }

            items(installed) { (target, _) ->
                EmulatorRow(target, installed = true) {
                    EmulatorRegistry.launchIntent(ctx, target)?.let { ctx.startActivity(it) }
                }
            }

            if (showMissing) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AVAILABLE TO INSTALL",
                        style = MaterialTheme.typography.labelLarge,
                        color = QCColors.Dim,
                    )
                }
                items(missing) { (target, _) ->
                    EmulatorRow(target, installed = false) {
                        ctx.startActivity(EmulatorRegistry.storeIntent(target))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmulatorRow(
    target: EmulatorTarget,
    installed: Boolean,
    onAction: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    target.system,
                    style = MaterialTheme.typography.bodyLarge,
                    color = QCColors.Soft,
                )
                Text(
                    target.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
                target.preferredCore?.let {
                    Text(
                        "core: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = QCColors.Dim,
                    )
                }
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (installed) QCColors.Teal else QCColors.Edge,
                    contentColor = if (installed) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (installed) "Launch" else "Install") }
        }
    }
}
