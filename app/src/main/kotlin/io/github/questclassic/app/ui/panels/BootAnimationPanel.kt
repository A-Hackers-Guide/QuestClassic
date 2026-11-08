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
 * "Optionally install a custom start animation, revert it to original non-Meta startup"
 * (option 3 from the design brief).
 *
 * Android's boot animation is a specially-named zip at `/system/media/bootanimation.zip`
 * (sometimes under `/product/media/`). AOSP's bootanimation binary parses a desc.txt and
 * replays PNG frames in named directories inside the zip.
 *
 * On a rooted device the swap is a straightforward `mount -o remount,rw /system` + replace +
 * remount read-only. On stock, the panel shows the plan and the three bundled replacement
 * animations we ship (all original, clean-room art — no Meta IP).
 */
@Composable
fun BootAnimationPanel(container: AppContainer, onClose: () -> Unit) {
    val root = remember { container.rootDetector.status() }
    val options = remember { bundledAnimations() }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Boot Animation", "Swap the Meta splash for something you chose", onClose)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { RootGateCard("writing to /system/media/", root) }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("WHAT HAPPENS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Picked animation's zip is written to /system/media/bootanimation.zip " +
                                "(after a read-write remount). A copy of the original is kept at " +
                                "/data/local/tmp/bootanimation.meta.zip so you can revert.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Dim,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "# mount -o remount,rw /system\n" +
                                "# cp /sdcard/QuestClassic/boot/<choice>.zip /system/media/bootanimation.zip\n" +
                                "# chmod 644 /system/media/bootanimation.zip\n" +
                                "# mount -o remount,ro /system",
                            style = MaterialTheme.typography.labelMedium,
                            color = QCColors.Teal,
                        )
                    }
                }
            }
            items(options) { opt ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(opt.name, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                                Text(opt.description, style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
                            }
                            ChipLabel(opt.license, QCColors.OK)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(opt.filename, style = MaterialTheme.typography.labelMedium, color = QCColors.Teal)
                    }
                }
            }
        }
    }
}

private data class BootAnimOption(
    val name: String,
    val description: String,
    val filename: String,
    val license: String,
)

private fun bundledAnimations(): List<BootAnimOption> = listOf(
    BootAnimOption("Classic Oculus Rift CV1", "Swirling teal orb — fan-made homage, clean-room art", "rift_homage.zip", "MIT"),
    BootAnimOption("Terminal Boot", "ANSI-style boot log that scrolls; green on black", "terminal.zip", "MIT"),
    BootAnimOption("QuestClassic Cyan", "Minimal cyan QC mark, 1.5 s loop", "qc_mark.zip", "MIT"),
    BootAnimOption("Dust Particle Field", "Drifting dust; no text at all", "dustfield.zip", "MIT"),
    BootAnimOption("Revert to Meta original", "Restores /data/local/tmp/bootanimation.meta.zip", "(revert)", "—"),
)
