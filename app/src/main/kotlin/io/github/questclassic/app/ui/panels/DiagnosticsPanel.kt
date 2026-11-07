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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.DiagnosticsResult
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.StatRow
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsPanel(container: AppContainer, onClose: () -> Unit) {
    var results by remember { mutableStateOf<List<DiagnosticsResult>>(emptyList()) }
    var running by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        running = true
        results = container.diagnostics.runAll()
        container.diagnostics.buzzFailures(results)
        running = false
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Diagnostics",
            subtitle = if (running) "Running…" else "${results.count { it.ok }}/${results.size} passed",
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    scope.launch {
                        running = true
                        results = container.diagnostics.runAll()
                        container.diagnostics.buzzFailures(results)
                        running = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text("Re-run") }
            Spacer(Modifier.height(0.dp))
            Spacer(Modifier.padding(horizontal = 6.dp))
            Button(
                onClick = { container.haptics.cancel() },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Stop buzz") }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(results) { r ->
                GlassPanel(modifier = Modifier.fillMaxWidth(), accent = if (r.ok) QCColors.OK else QCColors.Warn) {
                    Column {
                        Row {
                            Text(
                                r.name.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (r.ok) QCColors.OK else QCColors.Warn,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (r.ok) "OK" else "FAIL",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (r.ok) QCColors.OK else QCColors.Warn,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        StatRow(label = "value", value = r.value, ok = r.ok)
                        r.failPattern?.let {
                            StatRow(label = "buzz pattern", value = "${it.codeLetters} — ${it.label}", ok = false)
                        }
                    }
                }
            }
        }
    }
}
