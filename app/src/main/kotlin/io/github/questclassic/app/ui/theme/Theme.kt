package io.github.questclassic.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colors tuned to look "early Oculus" nostalgic — the deep-navy-with-teal-accent palette
 * that the original Rift/Gear VR shell used. Keep saturation low and contrast high so the
 * Quest's OLED-adjacent panels don't bloom.
 */
object QCColors {
    val Space = Color(0xFF07122A)
    val Deep = Color(0xFF0E1E3F)
    val Panel = Color(0xFF13274F)
    val Edge = Color(0xFF1E3666)
    val Teal = Color(0xFF34E5E7)
    val Amber = Color(0xFFFFB347)
    val Soft = Color(0xFFD7E2FF)
    val Dim = Color(0xFF7F91B9)
    val Warn = Color(0xFFFF6E6E)
    val OK = Color(0xFF66E0A3)
}

private val DarkScheme = darkColorScheme(
    primary = QCColors.Teal,
    onPrimary = QCColors.Space,
    secondary = QCColors.Amber,
    onSecondary = QCColors.Space,
    background = QCColors.Space,
    onBackground = QCColors.Soft,
    surface = QCColors.Panel,
    onSurface = QCColors.Soft,
    surfaceVariant = QCColors.Deep,
    onSurfaceVariant = QCColors.Dim,
    outline = QCColors.Edge,
    error = QCColors.Warn,
    onError = QCColors.Space,
)

// We don't really expect a "light" mode on a VR shell app, but keep one for Compose previews.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF1C3A80),
    secondary = Color(0xFFB87200),
    background = Color(0xFFF2F4FA),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun QuestClassicTheme(
    darkTheme: Boolean = isSystemInDarkTheme() || true,  // force dark; it's a VR shell
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = QCTypography,
        content = content,
    )
}
