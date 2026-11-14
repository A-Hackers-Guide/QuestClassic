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
import androidx.compose.material3.Switch
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
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.ExperimentalFlag
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Compose panel exposing the [ExperimentalFlag] registry (#119).
 *
 * Each flag renders as a GlassPanel row with the summary + a Switch.
 * Defaults off; graduated flags simply drop out of the enum and
 * disappear from this panel on the next build.
 */
@Composable
fun ExperimentalFlagsPanel(container: AppContainer, onClose: () -> Unit) {
    var enabled by remember { mutableStateOf<Set<ExperimentalFlag>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        enabled = container.experimentalFlags.enabledSet()
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Experimental Flags",
            subtitle = "${enabled.size}/${ExperimentalFlag.entries.size} on",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                "These toggles change behaviour that isn't ready for the " +
                    "main path yet. Expect rough edges. Graduated flags " +
                    "disappear from this list on the next build.",
                style = MaterialTheme.typography.bodySmall,
                color = QCColors.Dim,
            )
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(ExperimentalFlag.entries.toList()) { flag ->
                val on = flag in enabled
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(flag.name, style = MaterialTheme.typography.titleSmall, color = QCColors.Soft)
                            Spacer(Modifier.height(2.dp))
                            Text(flag.summary, style = MaterialTheme.typography.bodySmall, color = QCColors.Dim)
                        }
                        Switch(
                            checked = on,
                            onCheckedChange = { want ->
                                scope.launch {
                                    container.experimentalFlags.set(flag, want)
                                    enabled = container.experimentalFlags.enabledSet()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
