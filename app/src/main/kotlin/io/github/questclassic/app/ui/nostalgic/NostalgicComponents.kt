package io.github.questclassic.app.ui.nostalgic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.ui.theme.QCColors

/**
 * The "gradient panel card" — the single reusable surface the whole nostalgic UI is built on.
 * Imitates the frosted-glass panels from the original Oculus Rift Home.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = QCColors.Teal,
    content: @Composable () -> Unit,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(QCColors.Panel, QCColors.Deep),
    )
    val base = modifier
        .clip(RoundedCornerShape(18.dp))
        .background(gradient)
        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    val withClick = onClick?.let { base.clickable(onClick = it) } ?: base
    androidx.compose.foundation.layout.Box(
        withClick.padding(18.dp),
        contentAlignment = Alignment.TopStart,
    ) { content() }
}

@Composable
fun ChipLabel(text: String, color: Color = QCColors.Teal) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun PanelHeader(title: String, subtitle: String? = null, chip: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
            Spacer(Modifier.height(2.dp))
            Text(subtitle ?: "", style = MaterialTheme.typography.titleLarge, color = QCColors.Soft)
        }
        chip?.let { ChipLabel(it) }
    }
}

@Composable
fun StatRow(label: String, value: String, ok: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(50))
                .background(if (ok) QCColors.OK else QCColors.Warn),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
    }
}

@Composable
fun TileButton(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = QCColors.Teal, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = QCColors.Soft)
            }
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
        }
    }
}
