package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import io.github.questclassic.app.core.FirstRunController
import io.github.questclassic.app.core.FirstRunStep
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Setup wizard panel — one card per step in the first-run pipeline.
 *
 * Renders whatever [FirstRunController] reports as the current step.
 * Taps on "Next" advance one step; taps on "Skip AI setup" advance
 * from CAPABILITIES_REVIEWED to AI_CONFIGURED with the aiSkipped flag.
 *
 * COMPLETE is terminal; once the user reaches it, the About panel's
 * "Replay setup" button is the only way back to BLANK (via reset()).
 */
@Composable
fun SetupWizardPanel(container: AppContainer, onClose: () -> Unit) {
    var step by remember { mutableStateOf<FirstRunStep?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { step = container.firstRun.load().step }

    val current = step ?: return

    fun advance(next: FirstRunStep, aiSkipped: Boolean = false) {
        scope.launch {
            val ok = container.firstRun.advance(current, next, aiSkipped = aiSkipped)
            if (ok) step = next
        }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Setup",
            subtitle = stepHumanLabel(current),
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stepTitle(current), style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
                Spacer(Modifier.height(8.dp))
                Text(stepCopy(current), style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (current) {
                        FirstRunStep.BLANK -> {
                            Button(onClick = { advance(FirstRunStep.WELCOMED) }) { Text("Start") }
                        }
                        FirstRunStep.WELCOMED -> {
                            Button(onClick = { advance(FirstRunStep.CAPABILITIES_REVIEWED) }) { Text("Continue") }
                        }
                        FirstRunStep.CAPABILITIES_REVIEWED -> {
                            Button(onClick = { advance(FirstRunStep.AI_CONFIGURED) }) { Text("Add AI") }
                            Button(
                                onClick = { advance(FirstRunStep.AI_CONFIGURED, aiSkipped = true) },
                                colors = ButtonDefaults.outlinedButtonColors(),
                            ) { Text("Skip for now") }
                        }
                        FirstRunStep.AI_CONFIGURED -> {
                            Button(onClick = { advance(FirstRunStep.COMPLETE) }) { Text("Finish") }
                        }
                        FirstRunStep.COMPLETE -> {
                            Button(onClick = onClose) { Text("Close") }
                        }
                    }
                }
            }
        }
    }
}

private fun stepHumanLabel(step: FirstRunStep): String = when (step) {
    FirstRunStep.BLANK -> "Step 1/5 · Welcome"
    FirstRunStep.WELCOMED -> "Step 2/5 · Capabilities"
    FirstRunStep.CAPABILITIES_REVIEWED -> "Step 3/5 · AI"
    FirstRunStep.AI_CONFIGURED -> "Step 4/5 · Finish"
    FirstRunStep.COMPLETE -> "Done"
}

private fun stepTitle(step: FirstRunStep): String = when (step) {
    FirstRunStep.BLANK -> "Welcome to QuestClassic"
    FirstRunStep.WELCOMED -> "What this build does on stock vs. rooted"
    FirstRunStep.CAPABILITIES_REVIEWED -> "Optional: wire an AI assistant"
    FirstRunStep.AI_CONFIGURED -> "Setup complete"
    FirstRunStep.COMPLETE -> "You're all set"
}

private fun stepCopy(step: FirstRunStep): String = when (step) {
    FirstRunStep.BLANK ->
        "A nostalgic retro-Oculus shell. No telemetry, no ads, nothing phones home " +
            "except what you explicitly configure."
    FirstRunStep.WELCOMED ->
        "On stock builds, launcher / build-tools / AI panel work. Passthrough " +
            "camera control, firmware dump, home-menu swap — those need root. " +
            "The app tells you which is which before you open them."
    FirstRunStep.CAPABILITIES_REVIEWED ->
        "Hook up Claude, Gemini, OpenAI, or a local Ollama. Each keeps its own " +
            "API key and wake name so you can run several in parallel. Keys stay " +
            "device-local; you can skip this and add one later."
    FirstRunStep.AI_CONFIGURED ->
        "That's it. Build Tools HUD is on the home screen; everything else is a " +
            "panel you can open from the launcher."
    FirstRunStep.COMPLETE ->
        "Setup is marked complete. You can replay it any time from the About panel."
}
