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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "Build Tools" HUD. In the full vision this is a minimizable overlay that lets the user
 * compose panels out of widgets. In this first version it ships several ready-made widgets and
 * a settings mechanism to pick which ones are visible. Widget "code" means a Kotlin class — more
 * widgets land as PRs (see docs/widget_sdk.md).
 */
enum class Widget(val title: String, val render: @Composable () -> Unit) {
    CLOCK("Clock", @Composable { ClockWidget() }),
    DEVICE_FP("Fingerprint", @Composable { FingerprintWidget() }),
    FREE_MEM("Free memory", @Composable { Text("ⓘ widget — fills with live data when enabled on the headset", color = QCColors.Dim) }),
    NET_PING("Network ping", @Composable { Text("ⓘ widget — fills with live data when enabled on the headset", color = QCColors.Dim) }),
    QUICK_NOTE("Quick note", @Composable { Text("ⓘ widget — fills with live data when enabled on the headset", color = QCColors.Dim) }),
    CONTROLLER_HAPTIC("Haptic test", @Composable { Text("ⓘ widget — fills with live data when enabled on the headset", color = QCColors.Dim) }),
}

@Composable
fun BuildToolsPanel(container: AppContainer, onClose: () -> Unit) {
    val enabled = remember { mutableStateListOf<Widget>().apply { addAll(Widget.entries.take(2)) } }

    Column(Modifier.fillMaxSize()) {
        PanelClose(title = "Build Tools", subtitle = "${enabled.size}/${Widget.entries.size} widgets active", onClose = onClose)
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WIDGETS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(8.dp))
                Widget.entries.forEach { w ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(w.title, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft, modifier = Modifier.weight(1f))
                        Button(
                            onClick = { if (w in enabled) enabled.remove(w) else enabled.add(w) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (w in enabled) QCColors.Teal else QCColors.Edge,
                                contentColor = if (w in enabled) QCColors.Space else QCColors.Soft,
                            ),
                        ) { Text(if (w in enabled) "On" else "Off") }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("LIVE HUD", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(enabled.toList()) { w ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(w.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        w.render()
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockWidget() {
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("EEEE HH:mm:ss", Locale.getDefault())
        while (true) { now = fmt.format(Date()); kotlinx.coroutines.delay(500) }
    }
    Text(now, style = MaterialTheme.typography.displayMedium, color = QCColors.Soft)
}

@Composable
private fun FingerprintWidget() {
    Text(android.os.Build.FINGERPRINT, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}
