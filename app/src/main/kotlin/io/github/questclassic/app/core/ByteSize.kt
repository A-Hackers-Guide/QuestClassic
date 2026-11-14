package io.github.questclassic.app.core

/**
 * Format byte counts human-readably. IEC (base 1024, suffixed KiB/MiB
 * /GiB/TiB) because storage / memory numbers inside QuestClassic are
 * all powers of two; the SI side is deliberately not supported.
 *
 * Pure function; unit-testable.
 */
object ByteSize {

    fun format(bytes: Long, decimals: Int = 1): String {
        require(decimals in 0..4) { "decimals must be 0..4" }
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KiB", "MiB", "GiB", "TiB", "PiB")
        var value = bytes.toDouble() / 1024
        var i = 0
        while (value >= 1024 && i < units.lastIndex) {
            value /= 1024
            i++
        }
        return "%.${decimals}f %s".format(value, units[i])
    }

    /** Strict 3-digit form like 'du -h': 'K' / 'M' / 'G' / 'T' suffixes
     *  (no 'iB'), always one or two chars of mantissa. */
    fun formatShort(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val units = listOf("K", "M", "G", "T")
        var value = bytes.toDouble() / 1024
        var i = 0
        while (value >= 1024 && i < units.lastIndex) {
            value /= 1024
            i++
        }
        val dp = if (value >= 100) 0 else if (value >= 10) 0 else 1
        return "%.${dp}f%s".format(value, units[i])
    }
}
