package io.github.questclassic.app.core

import kotlin.math.hypot

/**
 * Applies a radial deadzone + outer clamp + response curve to a Touch
 * joystick's (x, y) in [-1, 1]². Used by Virtual Monitor cursor
 * control so a rested thumb doesn't drift the pointer.
 *
 *   - If magnitude < [innerRadius]: output is (0, 0) — dead.
 *   - If magnitude > [outerRadius]: output is clamped to the outer.
 *   - Otherwise linearly remapped so the edge of the dead circle
 *     is 0 and the outer circle is 1; the direction is preserved.
 *   - A gentle exponential response ([expo]) sharpens fine movement
 *     near centre without clipping full-stick.
 *
 * Pure function; unit-testable.
 */
object ControllerAxisDeadzone {

    data class XY(val x: Float, val y: Float)

    fun apply(
        rawX: Float,
        rawY: Float,
        innerRadius: Float = 0.12f,
        outerRadius: Float = 0.95f,
        expo: Float = 1.6f,
    ): XY {
        require(innerRadius in 0f..0.5f) { "innerRadius out of range: $innerRadius" }
        require(outerRadius in (innerRadius + 0.05f)..1f) { "outerRadius out of range: $outerRadius" }
        val mag = hypot(rawX, rawY)
        if (mag <= innerRadius) return XY(0f, 0f)
        val span = outerRadius - innerRadius
        val normalised = ((mag - innerRadius) / span).coerceIn(0f, 1f)
        val eased = kotlin.math.pow(normalised.toDouble(), expo.toDouble()).toFloat()
        // Preserve direction, scale to eased magnitude.
        val scale = eased / mag
        return XY(rawX * scale, rawY * scale)
    }
}
