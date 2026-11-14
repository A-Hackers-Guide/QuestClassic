package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.ClipboardPolicy
import io.github.questclassic.app.core.ContentIntegrity
import io.github.questclassic.app.core.DiagnosticsBundle
import io.github.questclassic.app.core.NotificationCenter
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Compose panel over [DiagnosticsBundle] (#85) + [ContentIntegrity] (#112)
 * + [ClipboardPolicy] (#101). Generates a shareable support snapshot,
 * shows its SHA-256 short hash, and copies it to the clipboard through
 * the redaction choke-point so secrets can't leak.
 */
@Composable
fun DiagnosticsBundlePanel(container: AppContainer, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var json by remember { mutableStateOf<String?>(null) }
    var shortHash by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val bundle = DiagnosticsBundle.capture(context, container)
        val serialized = DiagnosticsBundle.toJson(bundle)
        json = serialized
        shortHash = ContentIntegrity.shortHash(ContentIntegrity.sha256Hex(serialized))
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Diagnostics bundle",
            subtitle = if (json == null) "capturing…" else "ready · ${json!!.length} bytes · #$shortHash",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                "What's in: app version + device + feature counts. What's OUT: " +
                    "API keys, chat history, timezone, IP. Safe to paste into " +
                    "an issue or support thread.",
                style = MaterialTheme.typography.bodySmall,
                color = QCColors.Dim,
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = json != null,
                onClick = {
                    val src = json ?: return@Button
                    val decision = ClipboardPolicy.decide(ClipboardPolicy.Sensitivity.DEBUG, src)
                    when (decision) {
                        is ClipboardPolicy.Decision.Copy -> {
                            clipboard.setText(AnnotatedString(decision.text))
                            scope.launch {
                                container.notifications.post(
                                    severity = NotificationCenter.Severity.INFO,
                                    category = NotificationCenter.Category.SYSTEM,
                                    title = "Diagnostics copied",
                                    body = "Hash: #$shortHash" +
                                        if (decision.wasRedacted) " · redacted secrets" else "",
                                )
                            }
                        }
                        is ClipboardPolicy.Decision.Refused -> {
                            container.notifications.post(
                                severity = NotificationCenter.Severity.WARN,
                                category = NotificationCenter.Category.SYSTEM,
                                title = "Copy refused",
                                body = decision.reason,
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text("Copy to clipboard") }

            Button(
                enabled = json != null,
                onClick = {
                    scope.launch {
                        val bundle = DiagnosticsBundle.capture(context, container)
                        val serialized = DiagnosticsBundle.toJson(bundle)
                        json = serialized
                        shortHash = ContentIntegrity.shortHash(ContentIntegrity.sha256Hex(serialized))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Refresh") }
        }
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                Text(
                    json ?: "Capturing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Soft,
                )
            }
        }
    }
}
