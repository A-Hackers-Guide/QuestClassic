package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.BuildConfig
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import io.github.questclassic.app.update.GithubRelease
import kotlinx.coroutines.launch

@Composable
fun UpdatePanel(container: AppContainer, onClose: () -> Unit) {
    var latest by remember { mutableStateOf<GithubRelease?>(null) }
    var checking by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        checking = true
        latest = container.updater.checkLatest()
        checking = false
        status = when {
            latest == null -> "Couldn't reach GitHub."
            latest?.let { container.updater.isNewer(it) } == true -> "Update available."
            else -> "You're on the latest."
        }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Updates",
            subtitle = "Current: ${BuildConfig.VERSION_NAME}",
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("RELEASE CHANNEL", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(6.dp))
                Text(status, style = MaterialTheme.typography.titleLarge, color = QCColors.Soft)
                Spacer(Modifier.height(10.dp))
                latest?.let { r ->
                    Text("Tag: ${r.tag_name}", style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                    r.name?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft) }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val canUpdate = container.updater.isNewer(r) && container.updater.pickApk(r) != null
                        Button(
                            enabled = canUpdate && !downloading,
                            onClick = {
                                val asset = container.updater.pickApk(r) ?: return@Button
                                scope.launch {
                                    downloading = true
                                    status = "Downloading…"
                                    val file = container.updater.downloadApk(asset)
                                    downloading = false
                                    if (file != null) {
                                        status = "Launching installer…"
                                        container.updater.install(file)
                                    } else {
                                        status = "Download failed."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
                        ) { Text(if (downloading) "…" else "Install") }
                    }
                }
                if (checking) {
                    Spacer(Modifier.height(10.dp))
                    Text("Checking…", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WHY THIS WORKS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Meta's A/B OTAs leave user-installed APKs intact, so QuestClassic survives every system update automatically. We don't touch Meta's OTA pipeline — we ride alongside it on our own GitHub release channel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QCColors.Dim,
                )
            }
        }
    }
}
