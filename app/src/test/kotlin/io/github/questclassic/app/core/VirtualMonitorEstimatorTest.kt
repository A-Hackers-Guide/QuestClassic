package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualMonitorEstimatorTest {

    @Test
    fun `Quest 2 maps to XR2 Gen 1`() {
        assertEquals(
            VirtualMonitorEstimator.HeadsetTier.XR2_GEN_1,
            VirtualMonitorEstimator.tierFor("qcom", "hollywood"),
        )
    }

    @Test
    fun `Quest Pro maps to XR2 Gen 1`() {
        assertEquals(
            VirtualMonitorEstimator.HeadsetTier.XR2_GEN_1,
            VirtualMonitorEstimator.tierFor("qcom", "seacliff"),
        )
    }

    @Test
    fun `Quest 3 maps to XR2 Gen 2`() {
        assertEquals(
            VirtualMonitorEstimator.HeadsetTier.XR2_GEN_2,
            VirtualMonitorEstimator.tierFor("qcom", "eureka"),
        )
    }

    @Test
    fun `unknown hardware defaults to legacy`() {
        val tier = VirtualMonitorEstimator.tierFor("something", "unknown")
        assertEquals(VirtualMonitorEstimator.HeadsetTier.LEGACY_XR1, tier)
    }

    @Test
    fun `tier ordering: Gen 2 realistic exceeds Gen 1 exceeds legacy`() {
        val legacy = VirtualMonitorEstimator.estimate(VirtualMonitorEstimator.HeadsetTier.LEGACY_XR1).realistic
        val gen1 = VirtualMonitorEstimator.estimate(VirtualMonitorEstimator.HeadsetTier.XR2_GEN_1).realistic
        val gen2 = VirtualMonitorEstimator.estimate(VirtualMonitorEstimator.HeadsetTier.XR2_GEN_2).realistic
        assertTrue("legacy < gen1", legacy < gen1)
        assertTrue("gen1 < gen2", gen1 < gen2)
    }

    @Test
    fun `hard cap always exceeds realistic`() {
        for (t in VirtualMonitorEstimator.HeadsetTier.entries) {
            val e = VirtualMonitorEstimator.estimate(t)
            assertTrue("$t: realistic ${e.realistic} must be <= hardCap ${e.hardCap}",
                e.realistic <= e.hardCap)
        }
    }

    @Test
    fun `no tier returns the user's requested 20-100`() {
        // Policy test: we never produce >12 even for FUTURE. If someone
        // changes the numbers to make up for marketing, this fails.
        for (t in VirtualMonitorEstimator.HeadsetTier.entries) {
            val e = VirtualMonitorEstimator.estimate(t)
            assertTrue("$t: hardCap ${e.hardCap} must be <= 12", e.hardCap <= 12)
        }
    }

    @Test
    fun `case insensitive match`() {
        assertEquals(
            VirtualMonitorEstimator.HeadsetTier.XR2_GEN_2,
            VirtualMonitorEstimator.tierFor("QCOM", "EUREKA"),
        )
    }
}
