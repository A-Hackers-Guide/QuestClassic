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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.ClipboardPolicy
import io.github.questclassic.app.core.NotificationCenter
import io.github.questclassic.app.core.ScriptCategory
import io.github.questclassic.app.core.ScriptTemplates
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors

/**
 * Compose panel listing [ScriptTemplates] (#123). Each entry is a
 * copy-to-clipboard tile; firmware / passthrough entries are gated
 * behind a 'show root commands' toggle that defaults OFF so a stock
 * user can't accidentally paste something the shell will reject.
 */
@Composable
fun ScriptTemplatesPanel(container: AppContainer, onClose: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var rooted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rooted = container.rootDetector.status().isRooted
    }

    var showRoot by remember { mutableStateOf(false) }
    val list = ScriptTemplates.ALL.filter { it.requiresRoot && showRoot || !it.requiresRoot }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Script Templates",
            subtitle = "${list.size} / ${ScriptTemplates.size()} shown · copy, don't paste blindly",
            onClose = onClose,
        )
        Spacer(Modifier.height(10.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "These are copy-ready adb + pm one-liners. Nothing here runs on tap; " +
                        "paste into an adb shell on a desktop with the headset connected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                if (rooted) {
                    Button(
                        onClick = { showRoot = !showRoot },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QCColors.Edge, contentColor = QCColors.Soft,
                        ),
                    ) { Text(if (showRoot) "Hide root" else "Show root") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(list) { tmpl ->
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (tmpl.requiresRoot) QCColors.Amber else QCColors.Teal,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tmpl.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = QCColors.Soft,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                categoryTag(tmpl.category),
                                style = MaterialTheme.typography.labelSmall,
                                color = QCColors.Dim,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tmpl.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = QCColors.Dim,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val d = ClipboardPolicy.decide(
                                    ClipboardPolicy.Sensitivity.DEBUG, tmpl.body,
                                )
                                if (d is ClipboardPolicy.Decision.Copy) {
                                    clipboard.setText(AnnotatedString(d.text))
                                    container.notifications.post(
                                        severity = NotificationCenter.Severity.INFO,
                                        category = NotificationCenter.Category.SYSTEM,
                                        title = "Script copied",
                                        body = tmpl.title,
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QCColors.Teal, contentColor = QCColors.Space,
                            ),
                        ) { Text("Copy") }
                    }
                }
            }
        }
    }
}

private fun categoryTag(c: ScriptCategory): String = c.name.lowercase().replace('_', ' ')
