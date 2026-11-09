package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.MetaHostsAudit
import io.github.questclassic.app.ui.nostalgic.ChipLabel
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.StatRow
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun MetaHostsAuditPanel(container: AppContainer, onClose: () -> Unit) {
    val probes = remember { mutableStateListOf<MetaHostsAudit.Probe>() }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run() {
        running = true; probes.clear()
        scope.launch {
            val results = MetaHostsAudit.all.map { host ->
                async { MetaHostsAudit.probe(host) }
            }.awaitAll()
            probes.addAll(results)
            running = false
        }
    }
    LaunchedEffect(Unit) { run() }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Meta Hosts Audit",
            subtitle = if (running) "Resolving…"
                else "${probes.count { it.ip != null }} resolved · ${probes.count { it.reachable }} reachable",
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { run() }, enabled = !running,
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text(if (running) "Scanning…" else "Rescan") }
        }
        Spacer(Modifier.height(16.dp))

        val grouped = probes.groupBy { it.host.category }.toSortedMap()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (cat, list) ->
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = when (cat) {
                            MetaHostsAudit.Category.AUTH -> QCColors.Teal
                            MetaHostsAudit.Category.GRAPH -> QCColors.Soft
                            MetaHostsAudit.Category.GENAI -> QCColors.Amber
                            MetaHostsAudit.Category.PAYMENTS -> QCColors.OK
                            MetaHostsAudit.Category.CDN -> QCColors.Dim
                            MetaHostsAudit.Category.INTERNAL -> QCColors.Warn
                            MetaHostsAudit.Category.TELEMETRY -> QCColors.Warn
                            MetaHostsAudit.Category.OTA -> QCColors.OK
                        },
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.name, style = MaterialTheme.typography.labelLarge, color = QCColors.Teal, modifier = Modifier.weight(1f))
                                ChipLabel("${list.count { it.reachable }}/${list.size} reachable")
                            }
                            Spacer(Modifier.height(8.dp))
                            list.forEach { p ->
                                val value = buildString {
                                    append(p.ip ?: "DNS fail")
                                    if (p.reachable) append(" · ${p.latencyMs}ms")
                                    if (!p.reachable && p.ip != null) append(" · unreachable")
                                    if (p.error != null) append(" · ${p.error}")
                                }
                                StatRow(p.host.name, value, ok = p.reachable)
                                if (p.host.note.isNotEmpty()) {
                                    Text(p.host.note, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
