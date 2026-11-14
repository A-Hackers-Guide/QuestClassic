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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.NotificationCenter
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reader for the in-process [NotificationCenter] feed (from #120).
 *
 * Shows every notice posted since the app started: injection-flag warnings
 * from [AIAssistantPanel], transcript-export confirmations, update checks,
 * security events. Not Android system notifications — those are
 * out-of-process.
 */
@Composable
fun NotificationsPanel(container: AppContainer, onClose: () -> Unit) {
    val notices by container.notifications.notices.collectAsState()
    val unread = notices.count { !it.read }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Notifications",
            subtitle = "$unread unread · ${notices.size} total",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { container.notifications.markAllRead() },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Mark all read") }
            Button(
                onClick = { container.notifications.clear() },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Clear") }
        }
        Spacer(Modifier.height(12.dp))

        if (notices.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        "NOTHING HERE YET",
                        style = MaterialTheme.typography.labelLarge,
                        color = QCColors.Dim,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Notices will show up as modules post them — injection-flag " +
                            "warnings from the AI panel, transcript export confirmations, " +
                            "update checks, security events.",
                        style = MaterialTheme.typography.bodySmall,
                        color = QCColors.Dim,
                    )
                }
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(notices.reversed()) { n ->
                val accent = when (n.severity) {
                    NotificationCenter.Severity.INFO -> QCColors.Soft
                    NotificationCenter.Severity.WARN -> QCColors.Amber
                    NotificationCenter.Severity.ERROR -> QCColors.Warn
                }
                GlassPanel(modifier = Modifier.fillMaxWidth(), accent = accent) {
                    Column(Modifier.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${n.severity.name} · ${n.category.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatTime(n.postedAtMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = QCColors.Dim,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(n.title, style = MaterialTheme.typography.titleSmall, color = QCColors.Soft)
                        if (n.body.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = QCColors.Dim)
                        }
                    }
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
private fun formatTime(ms: Long): String = timeFmt.format(Date(ms))
