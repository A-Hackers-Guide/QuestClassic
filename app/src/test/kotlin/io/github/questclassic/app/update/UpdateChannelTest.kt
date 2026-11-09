package io.github.questclassic.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateChannelTest {

    @Test
    fun `stable allows release tags only`() {
        assertTrue(UpdateChannel.STABLE.allows("v1.2.3"))
        assertTrue(UpdateChannel.STABLE.allows("1.2.3"))
        assertFalse(UpdateChannel.STABLE.allows("v1.2.3-beta.1"))
        assertFalse(UpdateChannel.STABLE.allows("main-20260104"))
    }

    @Test
    fun `beta allows beta and release`() {
        assertTrue(UpdateChannel.BETA.allows("v1.2.3"))
        assertTrue(UpdateChannel.BETA.allows("v1.2.3-beta.1"))
        assertTrue(UpdateChannel.BETA.allows("v1.2.3-rc1"))
        assertFalse(UpdateChannel.BETA.allows("main-20260104"))
    }

    @Test
    fun `edge allows anything`() {
        assertTrue(UpdateChannel.EDGE.allows("v1.2.3"))
        assertTrue(UpdateChannel.EDGE.allows("v1.2.3-beta.1"))
        assertTrue(UpdateChannel.EDGE.allows("main-20260104"))
    }

    @Test
    fun `versionCompare ordering`() {
        assertTrue(UpdateChannel.versionCompare("v1.2.3", "v1.2.4") < 0)
        assertTrue(UpdateChannel.versionCompare("v1.3.0", "v1.2.9") > 0)
        assertEquals(0, UpdateChannel.versionCompare("v1.2.3", "v1.2.3"))
    }

    @Test
    fun `prerelease loses to release of same number`() {
        assertTrue(UpdateChannel.versionCompare("v1.2.3-beta.1", "v1.2.3") < 0)
        assertTrue(UpdateChannel.versionCompare("v1.2.3", "v1.2.3-rc.1") > 0)
    }

    @Test
    fun `prerelease ordering stable`() {
        assertTrue(UpdateChannel.versionCompare("v1.2.3-beta.1", "v1.2.3-beta.2") < 0)
    }

    @Test
    fun `picks latest on each channel`() {
        val tags = listOf("v1.2.3", "v1.2.4-beta.1", "v1.2.4-rc.1", "v1.3.0", "main-20260104")
        assertEquals("v1.3.0", UpdateChannel.picks(tags, UpdateChannel.STABLE))
        assertEquals("v1.3.0", UpdateChannel.picks(tags, UpdateChannel.BETA))
        assertEquals("main-20260104", UpdateChannel.picks(tags, UpdateChannel.EDGE))
    }

    @Test
    fun `picks returns null when nothing matches`() {
        assertNull(UpdateChannel.picks(listOf("main-xxx"), UpdateChannel.STABLE))
    }

    @Test
    fun `byName falls back to STABLE on unknown`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.byName(""))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.byName("nonexistent"))
        assertEquals(UpdateChannel.BETA, UpdateChannel.byName("beta"))
        assertEquals(UpdateChannel.EDGE, UpdateChannel.byName("EDGE"))
    }
}
