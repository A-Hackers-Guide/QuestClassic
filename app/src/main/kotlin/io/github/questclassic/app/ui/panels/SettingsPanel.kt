package io.github.questclassic.app.ui.panels

import android.content.Intent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.ar.ARHomeActivity
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.StatRow
import io.github.questclassic.app.ui.theme.QCColors

@Composable
fun SettingsPanel(container: AppContainer, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val props = container.introspection.summary()

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "About",
            subtitle = if (container.introspection.isQuest) "Meta Quest device" else "Non-Quest Android device",
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("DEVICE", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(8.dp))
                props.forEach { StatRow(it.key, it.value ?: "—") }
            }
        }
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("EXPERIMENTAL", style = MaterialTheme.typography.labelLarge, color = QCColors.Amber)
                Spacer(Modifier.height(8.dp))
                Text(
                    "AR Home — pop-out semicircular hand menu. Requires passthrough support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QCColors.Dim,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { ctx.startActivity(Intent(ctx, ARHomeActivity::class.java)) },
                    colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
                ) { Text("Open AR Home") }
            }
        }
    }
}
