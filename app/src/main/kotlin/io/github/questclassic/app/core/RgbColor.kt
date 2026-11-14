package io.github.questclassic.app.core

/**
 * Small pure RGB color helpers used by the boot-animation + buzz-
 * pattern UI previews. Keeps us off Compose's ui.graphics.Color inside
 * core (so unit tests don't need Android) while still giving the panels
 * a uniform way to talk about hues.
 *
 * - [parseHex] accepts '#RRGGBB' / 'RRGGBB' / '#AARRGGBB'.
 * - [toHex] produces '#RRGGBB' or '#AARRGGBB' depending on alpha.
 * - [lerp] mixes two colors linearly in sRGB space; good enough for
 *   the UI previews, not a color-accurate blend.
 * - [luminance] returns the ITU-R BT.709 relative luminance in 0..1,
 *   used to pick a readable foreground over an arbitrary background.
 */
data class RgbColor(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255 && a in 0..255) {
            "channel out of range"
        }
    }

    fun toHex(): String =
        if (a == 255) "#%02X%02X%02X".format(r, g, b)
        else "#%02X%02X%02X%02X".format(a, r, g, b)

    fun luminance(): Double =
        (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0

    companion object {
        fun parseHex(raw: String): RgbColor {
            val t = raw.trim().removePrefix("#")
            return when (t.length) {
                6 -> RgbColor(
                    r = t.substring(0, 2).toInt(16),
                    g = t.substring(2, 4).toInt(16),
                    b = t.substring(4, 6).toInt(16),
                )
                8 -> RgbColor(
                    a = t.substring(0, 2).toInt(16),
                    r = t.substring(2, 4).toInt(16),
                    g = t.substring(4, 6).toInt(16),
                    b = t.substring(6, 8).toInt(16),
                )
                else -> throw IllegalArgumentException("bad hex length: ${t.length}")
            }
        }

        fun lerp(a: RgbColor, b: RgbColor, t: Double): RgbColor {
            val tc = t.coerceIn(0.0, 1.0)
            return RgbColor(
                r = (a.r + (b.r - a.r) * tc).toInt(),
                g = (a.g + (b.g - a.g) * tc).toInt(),
                b = (a.b + (b.b - a.b) * tc).toInt(),
                a = (a.a + (b.a - a.a) * tc).toInt(),
            )
        }
    }
}
