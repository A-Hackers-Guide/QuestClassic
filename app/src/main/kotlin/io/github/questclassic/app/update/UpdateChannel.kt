package io.github.questclassic.app.update

/**
 * Lets the user pick which release stream to track:
 *
 *  - STABLE — released tags only (v1.2.3). Default.
 *  - BETA   — pre-release tags matching v*-beta* / v*-rc*.
 *  - EDGE   — any tag; latest wins, including `main`-cut dev builds.
 *
 * The selector [picks] filters a list of release tags to the ones
 * available on the chosen channel. Pure function, unit-testable.
 */
enum class UpdateChannel(val displayName: String) {
    STABLE("Stable"),
    BETA("Beta"),
    EDGE("Edge"),

    ;

    /** True when a tag is eligible for THIS channel. */
    fun allows(tag: String): Boolean = when (this) {
        EDGE -> true
        BETA -> isStableShape(tag) || isBetaShape(tag)
        STABLE -> isStableShape(tag)
    }

    companion object {
        private val STABLE_SHAPE = Regex("""^v?\d+\.\d+\.\d+$""")
        private val BETA_SHAPE = Regex("""^v?\d+\.\d+\.\d+-(beta|rc)\.?\d*$""", RegexOption.IGNORE_CASE)

        fun isStableShape(tag: String): Boolean = STABLE_SHAPE.matches(tag)
        fun isBetaShape(tag: String): Boolean = BETA_SHAPE.matches(tag)

        /** Semver-ish ordering: strips leading 'v', compares numeric
         *  parts, treats prerelease as less than release. */
        fun versionCompare(a: String, b: String): Int {
            val pa = parse(a) ?: return a.compareTo(b)
            val pb = parse(b) ?: return a.compareTo(b)
            for (i in 0..2) {
                val c = pa.nums[i].compareTo(pb.nums[i])
                if (c != 0) return c
            }
            // Same base numbers: prerelease loses to release.
            return when {
                pa.pre == null && pb.pre == null -> 0
                pa.pre == null -> 1   // a is release, b is prerelease → a > b
                pb.pre == null -> -1
                else -> pa.pre.compareTo(pb.pre)
            }
        }

        private data class Parsed(val nums: List<Int>, val pre: String?)

        private fun parse(raw: String): Parsed? {
            val m = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9.]+))?$""").matchEntire(raw)
                ?: return null
            return Parsed(
                nums = listOf(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()),
                pre = m.groupValues[4].ifEmpty { null },
            )
        }

        /** Pick the latest tag on the given channel; returns null if none match. */
        fun picks(tags: List<String>, channel: UpdateChannel): String? =
            tags.filter { channel.allows(it) }.maxWithOrNull(Comparator(::versionCompare))

        /** byName lookup used by DataStore round-trip. */
        fun byName(name: String): UpdateChannel = runCatching { valueOf(name.uppercase()) }.getOrDefault(STABLE)
    }
}
