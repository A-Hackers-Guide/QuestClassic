package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.ChipLabel
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "90° semicircle hand menu" (option 7). This flat-screen version is both the design preview
 * and the 2D fallback that the ARHomeActivity uses when passthrough + hand tracking aren't
 * available. The real AR version renders the same geometry in world space (see ADR 0008).
 *
 * Geometry:
 *   - Pivot: the user's hand or controller.
 *   - Sweep: 90° arc, center on the hand normal, spanning −45° to +45°.
 *   - Items: up to 8 slots equally spaced along the arc.
 *   - Selection: the OTHER hand/controller "touches" (physically intersects) the slot.
 *
 * In this 2D preview, we draw the arc + slots inside a canvas and label each slot.
 */
@Composable
fun ARSemicirclePanel(container: AppContainer, onClose: () -> Unit) {
    val slots = remember {
        listOf(
            "Launcher", "Diagnostics", "AI", "Build Tools",
            "Updates", "Signatures", "Internals", "Settings",
        )
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose("AR Semicircle Hand Menu", "Flat preview — 2D fallback of the AR mode", onClose)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth(), accent = QCColors.Teal) {
                    Column {
                        Text("WHAT YOU WOULD SEE IN AR", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Raise your left hand (or controller). A 90° arc fans out from " +
                                "your palm, pointing away from your body. Eight slots, evenly " +
                                "spaced along the arc. You tap a slot with your other index " +
                                "finger (or right controller) to open it. Flick your palm away " +
                                "to dismiss.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QCColors.Dim,
                        )
                    }
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("ARC PREVIEW", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.6f).padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height * 0.85f
                                val radius = size.width * 0.42f

                                // Arc stroke.
                                drawArc(
                                    color = QCColors.Teal.copy(alpha = 0.7f),
                                    startAngle = -135f,  // sweep from 225° CCW
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    topLeft = Offset(cx - radius, cy - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(width = 3f),
                                )

                                // Slot dots.
                                val n = slots.size
                                for (i in 0 until n) {
                                    val t = i.toDouble() / (n - 1).toDouble()  // [0,1]
                                    val angle = Math.toRadians(-135.0 + 90.0 * t)
                                    val x = cx + radius * cos(angle).toFloat()
                                    val y = cy + radius * sin(angle).toFloat()
                                    drawCircle(
                                        color = Color(0xFF34E5E7),
                                        radius = 10f,
                                        center = Offset(x, y),
                                    )
                                }

                                // Hand position marker.
                                drawCircle(
                                    color = Color(0xFFFFB347),
                                    radius = 18f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 3f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Amber ring = your hand pivot. Teal dots = the eight slot positions along a 90° arc.",
                            style = MaterialTheme.typography.labelMedium,
                            color = QCColors.Dim,
                        )
                    }
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("SLOT BINDINGS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        slots.forEachIndexed { i, label ->
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                ChipLabel("S${i + 1}", QCColors.Amber)
                                Spacer(Modifier.height(0.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = QCColors.Soft)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}
