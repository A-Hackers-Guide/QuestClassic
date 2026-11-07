package io.github.questclassic.app.ui.panels

import android.app.ActivityManager
import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.BuzzPattern
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "Build Tools" HUD. Minimizable overlay that lets the user compose panels out of widgets.
 * Each widget is a small Composable here (or under ui/panels/widgets/ when it grows). Toggling
 * a widget on/off re-renders the HUD stack below.
 */
enum class Widget(val title: String, val render: @Composable (AppContainer) -> Unit) {
    CLOCK("Clock", { ClockWidget() }),
    DEVICE_FP("Fingerprint", { FingerprintWidget() }),
    FREE_MEM("Free memory", { FreeMemoryWidget() }),
    NET_PING("Network ping", { NetworkPingWidget() }),
    QUICK_NOTE("Quick note", { QuickNoteWidget() }),
    CONTROLLER_HAPTIC("Haptic test", { c -> HapticTestWidget(c) }),
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
                        w.render(container)
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
        while (true) { now = fmt.format(Date()); delay(500) }
    }
    Text(now, style = MaterialTheme.typography.displayMedium, color = QCColors.Soft)
}

@Composable
private fun FingerprintWidget() {
    Text(android.os.Build.FINGERPRINT, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun FreeMemoryWidget() {
    val ctx = LocalContext.current
    var line by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val freeMb = mi.availMem / 1_048_576
            val totalMb = mi.totalMem / 1_048_576
            val pct = if (mi.totalMem > 0) (mi.availMem * 100.0 / mi.totalMem) else 0.0
            line = "%d / %d MiB free · %.1f%%".format(freeMb, totalMb, pct)
            delay(1500)
        }
    }
    Text(line, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun NetworkPingWidget() {
    var host by remember { mutableStateOf("1.1.1.1") }
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QCColors.Soft,
                    unfocusedTextColor = QCColors.Soft,
                    focusedBorderColor = QCColors.Teal,
                    unfocusedBorderColor = QCColors.Edge,
                ),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                enabled = !busy,
                onClick = {
                    busy = true; result = null
                    CoroutineScope(Dispatchers.IO).launch {
                        val start = System.nanoTime()
                        val r = runCatching {
                            val addr = InetAddress.getByName(host)
                            val reachable = addr.isReachable(2000)
                            val ms = (System.nanoTime() - start) / 1_000_000
                            if (reachable) "$addr · ${ms}ms" else "$addr · unreachable"
                        }.getOrElse { it.message ?: "resolve failed" }
                        withContext(Dispatchers.Main) {
                            result = r; busy = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text(if (busy) "…" else "Ping") }
        }
        Spacer(Modifier.height(6.dp))
        Text(result ?: "Not run yet.", style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
    }
}

@Composable
private fun QuickNoteWidget() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ephemeral scratch pad…", color = QCColors.Dim) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = QCColors.Soft,
            unfocusedTextColor = QCColors.Soft,
            focusedBorderColor = QCColors.Teal,
            unfocusedBorderColor = QCColors.Edge,
        ),
    )
}

@Composable
private fun HapticTestWidget(container: AppContainer) {
    val patterns = listOf(
        BuzzPattern.OK, BuzzPattern.BATTERY_LOW, BuzzPattern.STORAGE_FULL,
        BuzzPattern.WIFI_DOWN, BuzzPattern.OTA_CHECK_FAIL, BuzzPattern.THERMAL,
    )
    Column {
        Text("Tap a pattern to feel it on the controllers.", style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
        Spacer(Modifier.height(6.dp))
        patterns.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text("${p.label} — ${p.codeLetters}", style = MaterialTheme.typography.bodyMedium, color = QCColors.Soft, modifier = Modifier.weight(1f))
                Button(
                    onClick = { container.haptics.playPattern(p) },
                    colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
                ) { Text("Buzz") }
            }
        }
    }
}

