package io.github.questclassic.app.core

/**
 * Honest estimate of how many virtual monitors a given headset SoC can
 * realistically wirelessly stream to a paired desktop.
 *
 * The user's original ask was "20–100 virtual screens depending on
 * processing power". That upper bound doesn't match any shipping Quest
 * SoC: Quest 2's XR2 Gen 1 caps around 4 decoded H.264/H.265 streams at
 * 1080p60, Quest 3's XR2 Gen 2 nearly doubles that, and the ceiling
 * climbs from there — but not into double digits of monitors without
 * dropping framerate or resolution.
 *
 * This estimator returns a conservative number per [HeadsetTier] and a
 * human string the UI can show so the user never sees 100 as a goal.
 *
 * Pure function; unit-testable without hardware.
 */
object VirtualMonitorEstimator {

    enum class HeadsetTier {
        /** XR1 or older — Quest 1, Go, Rift S, pre-Quest 2 */
        LEGACY_XR1,

        /** XR2 Gen 1 — Quest 2, Quest Pro */
        XR2_GEN_1,

        /** XR2 Gen 2 — Quest 3, Quest 3S */
        XR2_GEN_2,

        /** XR3 and newer — unreleased at time of writing; returned when
         *  SoC detection hits a chip we don't recognise but is clearly
         *  newer than XR2 Gen 2. */
        FUTURE,
    }

    data class Estimate(
        val tier: HeadsetTier,
        /** The realistic number of monitors streamed in parallel. */
        val realistic: Int,
        /** A hard cap we'll never let the UI exceed. */
        val hardCap: Int,
        /** One-line user-facing explanation. */
        val humanNote: String,
    )

    /**
     * Pick a tier from Build.HARDWARE / Build.BOARD strings.
     * Case-insensitive; falls back to LEGACY_XR1 when nothing matches
     * (safer to under-promise).
     */
    fun tierFor(hardware: String, board: String): HeadsetTier {
        val h = "$hardware $board".lowercase()
        return when {
            // Quest 3 / 3S — monterey / eureka
            "monterey" in h || "eureka" in h -> HeadsetTier.XR2_GEN_2
            // Quest 2 / Pro — hollywood / seacliff
            "hollywood" in h || "seacliff" in h -> HeadsetTier.XR2_GEN_1
            // Quest 1 / Go / Rift S — vr-monterey, aglaia, miramar
            "vr_monterey" in h || "aglaia" in h || "miramar" in h -> HeadsetTier.LEGACY_XR1
            // Rough heuristic for a newer chip we don't recognise.
            "sm85" in h || "sm90" in h -> HeadsetTier.FUTURE
            else -> HeadsetTier.LEGACY_XR1
        }
    }

    /** Estimate for a known tier. */
    fun estimate(tier: HeadsetTier): Estimate = when (tier) {
        HeadsetTier.LEGACY_XR1 -> Estimate(
            tier = tier,
            realistic = 1,
            hardCap = 2,
            humanNote = "XR1-class SoC: 1 monitor at 1080p60 is realistic; " +
                "2 only at reduced resolution.",
        )
        HeadsetTier.XR2_GEN_1 -> Estimate(
            tier = tier,
            realistic = 3,
            hardCap = 4,
            humanNote = "XR2 Gen 1 (Quest 2 / Pro): 3 monitors at 1080p60 " +
                "or 4 at 1080p30 with headroom for the compositor.",
        )
        HeadsetTier.XR2_GEN_2 -> Estimate(
            tier = tier,
            realistic = 6,
            hardCap = 8,
            humanNote = "XR2 Gen 2 (Quest 3 / 3S): 6 monitors at 1080p60 " +
                "or 8 at 1080p30 is the realistic ceiling.",
        )
        HeadsetTier.FUTURE -> Estimate(
            tier = tier,
            realistic = 8,
            hardCap = 12,
            humanNote = "Newer SoC detected. Estimates conservative; " +
                "real numbers will depend on the specific chip.",
        )
    }

    /** Convenience: tier + estimate in one call. */
    fun estimate(hardware: String, board: String): Estimate =
        estimate(tierFor(hardware, board))
}
