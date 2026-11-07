package io.github.questclassic.app.ui.panels

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.InstalledApp
import io.github.questclassic.app.ui.nostalgic.ChipLabel
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.PanelHeader
import io.github.questclassic.app.ui.theme.QCColors

@Composable
fun LauncherPanel(container: AppContainer, onClose: () -> Unit) {
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { apps = container.appList.load() }

    Column(Modifier.fillMaxSize()) {
        PanelClose(title = "Launcher", subtitle = "${apps.size} installed", onClose = onClose)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps) { app ->
                AppTile(app, onClick = { container.appList.launch(app) })
            }
        }
    }
}

@Composable
private fun AppTile(app: InstalledApp, onClick: () -> Unit) {
    GlassPanel(onClick = onClick, accent = if (app.isVrApp) QCColors.Teal else QCColors.Amber) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val bitmap: Bitmap? = (app.icon as? BitmapDrawable)?.bitmap
                ?: app.icon?.let { d ->
                    val w = d.intrinsicWidth.coerceAtLeast(64)
                    val h = d.intrinsicHeight.coerceAtLeast(64)
                    runCatching {
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bm ->
                            val canvas = android.graphics.Canvas(bm)
                            d.setBounds(0, 0, w, h)
                            d.draw(canvas)
                        }
                    }.getOrNull()
                }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(QCColors.Edge),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.bodyMedium,
                color = QCColors.Soft,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            ChipLabel(if (app.isVrApp) "VR" else "2D", if (app.isVrApp) QCColors.Teal else QCColors.Amber)
        }
    }
}

@Composable
internal fun PanelClose(title: String, subtitle: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PanelHeader(title = "panel", subtitle = title)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(QCColors.Edge)
                .clickable(onClick = onClose)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("Close", style = MaterialTheme.typography.labelLarge, color = QCColors.Soft)
        }
    }
}
