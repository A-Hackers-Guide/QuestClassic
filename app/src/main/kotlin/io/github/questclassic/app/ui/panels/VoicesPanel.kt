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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import io.github.questclassic.app.ai.TrainingState
import io.github.questclassic.app.ai.Voice
import io.github.questclassic.app.ai.VoiceKind
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Voices panel — CRUD over the VoiceRegistry.
 *
 * Shows every voice (built-in + cloned) with current training state.
 * Add → starts a clone (QUEUED). Enable/disable → toggles without
 * deleting. Remove → drops the row.
 *
 * The actual training pipeline (audio capture + on-device model
 * fitting) is a follow-up PR; this panel exposes the state machine
 * so the user can see the pipeline's progress and retry FAILED
 * clones.
 */
@Composable
fun VoicesPanel(container: AppContainer, onClose: () -> Unit) {
    val voices = remember { mutableStateListOf<Voice>() }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        val loaded = container.voices.load()
        voices.clear()
        voices.addAll(loaded)
    }

    LaunchedEffect(Unit) { refresh() }

    val builtIns = voices.filter { it.kind == VoiceKind.BUILT_IN }
    val clones = voices.filter { it.kind == VoiceKind.CLONED }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Voices",
            subtitle = "${voices.size} total · ${clones.size} cloned",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("START A CLONE", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recording + on-device model fitting happen in a dedicated " +
                        "capture surface (future PR). Tap 'Start' here and the " +
                        "pipeline picks it up when ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Voice name (e.g. \"Me\")", color = QCColors.Dim) },
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = QCColors.Soft,
                        unfocusedTextColor = QCColors.Soft,
                        focusedBorderColor = QCColors.Teal,
                        unfocusedBorderColor = QCColors.Edge,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val n = newName.trim(); newName = ""
                        scope.launch {
                            runCatching {
                                container.voices.startClone(
                                    id = "clone-${UUID.randomUUID()}",
                                    displayName = n,
                                )
                            }.onSuccess { refresh() }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QCColors.Teal,
                        contentColor = QCColors.Space,
                    ),
                ) { Text("Start") }
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (clones.isNotEmpty()) {
                item {
                    Text("CLONED",
                        style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                }
                items(clones) { v -> VoiceRow(v, onToggle = {
                    scope.launch {
                        container.voices.toggleEnabled(v.id, !v.enabled); refresh()
                    }
                }, onRemove = {
                    scope.launch { container.voices.remove(v.id); refresh() }
                }, onRetry = {
                    scope.launch {
                        container.voices.advanceTraining(v.id, TrainingState.QUEUED); refresh()
                    }
                }) }
            }
            if (builtIns.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("BUILT-IN (platform TTS)",
                        style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                }
                items(builtIns) { v -> VoiceRow(v, onToggle = {
                    scope.launch {
                        container.voices.toggleEnabled(v.id, !v.enabled); refresh()
                    }
                }, onRemove = {
                    scope.launch { container.voices.remove(v.id); refresh() }
                }, onRetry = null) }
            }
            if (voices.isEmpty()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("NO VOICES YET",
                                style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Start a clone above, or wait for the platform TTS " +
                                    "scan to populate built-ins on first use.",
                                style = MaterialTheme.typography.bodySmall,
                                color = QCColors.Soft,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(
    voice: Voice,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(voice.displayName,
                    style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft)
                Text(
                    "${voice.kind.name} · ${voice.trainingState.name.lowercase()} · ${voice.locale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
            }
            if (onRetry != null && voice.trainingState == TrainingState.FAILED) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QCColors.Amber, contentColor = QCColors.Space,
                    ),
                ) { Text("Retry") }
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (voice.enabled) QCColors.Teal else QCColors.Edge,
                    contentColor = if (voice.enabled) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (voice.enabled) "On" else "Off") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge, contentColor = QCColors.Soft,
                ),
            ) { Text("Remove") }
        }
    }
}
