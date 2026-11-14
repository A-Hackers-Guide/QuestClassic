package io.github.questclassic.app.core

/**
 * Translates the headset's controller-ray hit coordinates on a virtual
 * monitor Compose surface into the companion desktop's absolute mouse
 * coordinates.
 *
 * The controller ray hits a Compose quad at (localX, localY) in pixels.
 * That quad represents a desktop monitor of (remoteW × remoteH) pixels
 * at the companion. The quad is transformed in the scene by position +
 * scale; the ray-hit is already pre-transformed and clipped to the
 * quad's local 0..localW / 0..localH range.
 *
 * We want (remoteX, remoteY) in 0..remoteW-1 / 0..remoteH-1 so the
 * companion's input injector can post a mouseMove event.
 *
 * Pure function; unit-testable.
 */
object MouseCoordMapper {

    data class Mapped(val remoteX: Int, val remoteY: Int)

    fun map(
        localX: Float, localY: Float,
        localW: Int, localH: Int,
        remoteW: Int, remoteH: Int,
    ): Mapped {
        require(localW > 0 && localH > 0) { "local dims must be positive" }
        require(remoteW > 0 && remoteH > 0) { "remote dims must be positive" }
        val xf = (localX.coerceIn(0f, localW.toFloat()) / localW) * remoteW
        val yf = (localY.coerceIn(0f, localH.toFloat()) / localH) * remoteH
        return Mapped(
            remoteX = xf.toInt().coerceIn(0, remoteW - 1),
            remoteY = yf.toInt().coerceIn(0, remoteH - 1),
        )
    }
}
