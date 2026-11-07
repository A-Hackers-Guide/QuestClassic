package io.github.questclassic.app.ar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.ui.theme.QCColors
import io.github.questclassic.app.ui.theme.QuestClassicTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * First-cut AR Home. The real passthrough + OpenXR integration lives behind the Meta Spatial SDK,
 * which is a large native dependency. This Activity is the entry point and the canvas side of the
 * semicircular hand menu; the SDK wiring lands in a follow-up PR.
 *
 * When started on a non-Quest device or without passthrough, we render the same menu as a 2D
 * preview so you can see what it looks like before the AR integration ships.
 */
class ARHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuestClassicTheme {
                Box(Modifier.fillMaxSize()) {
                    SemicircleHandMenu()
                    Column(Modifier.padding(24.dp)) {
                        Text("AR HOME · PREVIEW", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Semicircular hand menu. Passthrough + hand-tracking integration ships in the next build via Meta Spatial SDK. See docs/ar_mode.md.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Dim,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws the 90°-oriented semicircle of app tiles that would, in AR mode, follow the user's hand.
 * The animation here ticks a selection index so you can see the hover behavior.
 */
@Composable
fun SemicircleHandMenu() {
    val items = listOf("Launcher", "Diagnostics", "AI", "Build", "Update", "About")
    var hoverIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1400)
            hoverIndex = (hoverIndex + 1) % items.size
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width * 0.75f
        val cy = size.height * 0.5f
        val radius = size.minDimension * 0.32f
        val startDeg = -45.0
        val endDeg = 45.0
        val n = items.size
        for (i in 0 until n) {
            val t = i.toFloat() / (n - 1)
            val deg = startDeg + (endDeg - startDeg) * t
            val rad = deg * PI / 180.0
            val x = cx + (cos(rad).toFloat() * radius)
            val y = cy + (sin(rad).toFloat() * radius)
            val isHover = i == hoverIndex
            val tileColor = if (isHover) QCColors.Teal else Color(0xFF8DA2D6)
            drawCircle(color = tileColor.copy(alpha = 0.15f), radius = 42f, center = Offset(x, y))
            drawCircle(color = tileColor, radius = 42f, center = Offset(x, y), style = Stroke(width = 3f))
            drawLine(color = QCColors.Edge.copy(alpha = 0.6f), start = Offset(cx, cy), end = Offset(x, y), strokeWidth = 1.5f)
        }
        drawCircle(color = QCColors.Amber, radius = 14f, center = Offset(cx, cy))
    }
}
