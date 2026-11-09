package io.github.questclassic.app.ui.panels

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
import io.github.questclassic.app.core.PassthroughState
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Passthrough panel — three toggles over `PassthroughController`.
 *
 * Camera passthrough is capability-gated: the preference saves regardless,
 * but the "effective" badge flips only when `cameraPassthroughSupported`
 * returns true (i.e. we're on a rooted device where the compositor
 * surface is reachable).
 */
@Composable
fun PassthroughPanel(container: AppContainer, onClose: () -> Unit) {
    var state by remember { mutableStateOf<PassthroughState?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state = container.passthrough.load() }

    val current = state ?: return
    val supported = container.passthrough.cameraPassthroughSupported(container.rootDetector)
    val effective = container.passthrough.effectiveCameraOn(current, container.rootDetector)

    fun update(new: PassthroughState) {
        state = new
        scope.launch { container.passthrough.save(new) }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Passthrough",
            subtitle = "Camera: ${if (effective) "on" else "off"} · Audio: " +
                "${if (current.audioPassthrough) "on" else "off"} · Text: " +
                if (current.textPassthrough) "on" else "off",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("CAMERA PASSTHROUGH",
                    style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (supported)
                        "Rooted device — the compositor accepts our passthrough requests. Flip and go."
                    else
                        "Stock device — the compositor only accepts passthrough requests from the signed Meta shell. Your preference saves below, but effective stays off until you root or sideload a custom build. See docs/user-guide/passthrough.md.",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Soft,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preference", modifier = Modifier.weight(1f),
                        color = QCColors.Soft)
                    Button(
                        onClick = { update(current.copy(cameraPassthrough = !current.cameraPassthrough)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (current.cameraPassthrough) QCColors.Teal else QCColors.Edge,
                            contentColor = if (current.cameraPassthrough) QCColors.Space else QCColors.Soft,
                        ),
                    ) { Text(if (current.cameraPassthrough) "On" else "Off") }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)) {
                    Text("Effective", modifier = Modifier.weight(1f),
                        color = QCColors.Dim)
                    Text(
                        if (effective) "ACTIVE" else "blocked",
                        color = if (effective) QCColors.Teal else QCColors.Amber,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        SimpleToggleRow(
            label = "Audio passthrough (mic → AI / voice channel)",
            value = current.audioPassthrough,
        ) { update(current.copy(audioPassthrough = it)) }
        Spacer(Modifier.height(10.dp))

        SimpleToggleRow(
            label = "Text display (AI transcript, system notifications)",
            value = current.textPassthrough,
        ) { update(current.copy(textPassthrough = it)) }
    }
}

@Composable
private fun SimpleToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f),
                color = QCColors.Soft, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { onChange(!value) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (value) QCColors.Teal else QCColors.Edge,
                    contentColor = if (value) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (value) "On" else "Off") }
        }
    }
}
