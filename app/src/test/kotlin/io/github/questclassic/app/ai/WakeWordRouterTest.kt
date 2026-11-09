package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WakeWordRouterTest {

    private val router = WakeWordRouter()
    private val names = mapOf(
        "Nova" to "gemini",
        "Sage" to "openai",
        "Classic" to "claude",
        "Local" to "ollama",
    )

    @Test
    fun `routes with comma and hey filler`() {
        val m = router.route("Hey Nova, what's the weather", names)
        assertNotNull(m)
        assertEquals("gemini", m!!.backendId)
        assertEquals("Nova", m.wakeName)
        assertEquals("what's the weather", m.payload)
    }

    @Test
    fun `routes without filler word`() {
        val m = router.route("Classic: open the AR panel", names)
        assertNotNull(m)
        assertEquals("claude", m!!.backendId)
        assertEquals("open the AR panel", m.payload)
    }

    @Test
    fun `case insensitive`() {
        val m = router.route("HEY sage WHAT TIME IS IT", names)
        assertNotNull(m)
        assertEquals("openai", m!!.backendId)
        assertEquals("WHAT TIME IS IT", m.payload)
    }

    @Test
    fun `strips trailing question mark on normalisation`() {
        val m = router.route("Hey Local, are you up?", names)
        assertNotNull(m)
        assertEquals("ollama", m!!.backendId)
        assertEquals("are you up", m.payload)
    }

    @Test
    fun `respects word boundary so Novak does not match Nova`() {
        val m = router.route("Hey Novak, launch the app", names)
        assertNull(m)
    }

    @Test
    fun `prefers longer wake name on prefix collision`() {
        val overlapping = mapOf("Classic" to "claude", "Classic Pro" to "openai")
        val m = router.route("Hey Classic Pro, open settings", overlapping)
        assertNotNull(m)
        assertEquals("openai", m!!.backendId)
        assertEquals("Classic Pro", m.wakeName)
    }

    @Test
    fun `eats optional please filler`() {
        val m = router.route("Hey Nova, please summarize", names)
        assertNotNull(m)
        assertEquals("summarize", m!!.payload)
    }

    @Test
    fun `empty names map yields null`() {
        assertNull(router.route("Hey anyone, hi", emptyMap()))
    }

    @Test
    fun `blank utterance yields null`() {
        assertNull(router.route("   ", names))
    }

    @Test
    fun `no matching wake name yields null`() {
        assertNull(router.route("Hey Dolores, the weather today", names))
    }

    @Test
    fun `blank wake name is ignored`() {
        val withBlank = mapOf("" to "gemini", "Nova" to "gemini")
        val m = router.route("Hey Nova, hi", withBlank)
        assertNotNull(m)
        assertEquals("Nova", m!!.wakeName)
    }
}
