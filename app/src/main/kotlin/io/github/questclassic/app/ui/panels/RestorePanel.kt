package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.FunctionalityRestore
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors

/**
 * "Functionality restore" panel (option 9).
 *
 * Runs the [FunctionalityRestore] escalation ladder:
 *   ask → attempt-fix → show error → fallback text → buzz.
 *
 * This is the "something went wrong, help me" surface. It's deliberately simple on screen —
 * if the user's display is working at all, we show the current step; if not, by the time this
 * page would have been useful, we're already onto the buzz step.
 */
@Composable
fun RestorePanel(container: AppContainer, onClose: () -> Unit) {
    val events = remember { mutableStateListOf<FunctionalityRestore.Progress>() }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!running) {
            running = true
            container.restore.run(timeoutSec = 30).collect { events += it }
        }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose("Functionality Restore", "Sequential ladder: ask → fix → display → fallback → buzz", onClose)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            events.forEach { e ->
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = when (e) {
                        is FunctionalityRestore.Progress.Ask -> QCColors.Teal
                        is FunctionalityRestore.Progress.Attempt -> QCColors.Amber
                        is FunctionalityRestore.Progress.Fixed -> QCColors.OK
                        is FunctionalityRestore.Progress.ShowError -> QCColors.Warn
                        is FunctionalityRestore.Progress.Fallback -> QCColors.Warn
                        is FunctionalityRestore.Progress.Buzz -> QCColors.Warn
                    }) {
                        Column {
                            Text(label(e), style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                            Spacer(Modifier.height(6.dp))
                            Text(detail(e), style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                        }
                    }
                }
            }
        }
    }
}

private fun label(p: FunctionalityRestore.Progress): String = when (p) {
    is FunctionalityRestore.Progress.Ask -> "ASK"
    is FunctionalityRestore.Progress.Attempt -> "ATTEMPT"
    is FunctionalityRestore.Progress.Fixed -> "FIXED"
    is FunctionalityRestore.Progress.ShowError -> "ERROR"
    is FunctionalityRestore.Progress.Fallback -> "FALLBACK"
    is FunctionalityRestore.Progress.Buzz -> "BUZZ"
}

private fun detail(p: FunctionalityRestore.Progress): String = when (p) {
    is FunctionalityRestore.Progress.Ask -> "Do you notice any problems? (timer: ${p.timeoutSec}s)"
    is FunctionalityRestore.Progress.Attempt -> p.action
    is FunctionalityRestore.Progress.Fixed -> p.summary
    is FunctionalityRestore.Progress.ShowError -> "${p.name}: ${p.detail}"
    is FunctionalityRestore.Progress.Fallback -> "fallback surface — ${p.name}"
    is FunctionalityRestore.Progress.Buzz -> "buzz pattern: ${p.pattern.label} (${p.pattern.codeLetters})"
}
