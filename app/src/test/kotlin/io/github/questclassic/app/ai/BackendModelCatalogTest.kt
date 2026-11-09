package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendModelCatalogTest {

    @Test
    fun `each provider has at least one model`() {
        for (provider in listOf("claude", "openai", "gemini", "ollama")) {
            assertTrue(provider, BackendModelCatalog.models(provider).isNotEmpty())
        }
    }

    @Test
    fun `claude opus 55 is in the catalog`() {
        val info = BackendModelCatalog.findById("claude-opus-5-5")
        assertNotNull(info)
        assertEquals(200_000, info!!.contextTokens)
    }

    @Test
    fun `unknown model id returns null`() {
        assertNull(BackendModelCatalog.findById("totally-made-up"))
    }

    @Test
    fun `unknown backend returns empty list`() {
        assertTrue(BackendModelCatalog.models("future-provider").isEmpty())
    }

    @Test
    fun `defaultFor returns the first model`() {
        assertEquals("claude-opus-5-5", BackendModelCatalog.defaultFor("claude"))
        assertEquals("gpt-5-mini", BackendModelCatalog.defaultFor("openai"))
    }

    @Test
    fun `backend id lookup is case insensitive`() {
        assertEquals(
            BackendModelCatalog.models("claude"),
            BackendModelCatalog.models("CLAUDE"),
        )
    }

    @Test
    fun `defaultFor returns null for unknown backend`() {
        assertNull(BackendModelCatalog.defaultFor("totally-new"))
    }

    @Test
    fun `context tokens are reasonable`() {
        // sanity: no model has <1000 or >10 million tokens in this catalog.
        for ((_, list) in BackendModelCatalog.MODELS) {
            for (m in list) {
                assertTrue("${m.id} ctx too small", m.contextTokens >= 1000)
                assertTrue("${m.id} ctx too big", m.contextTokens <= 10_000_000)
            }
        }
    }

    @Test
    fun `ids are unique across every provider`() {
        val allIds = BackendModelCatalog.MODELS.values.flatten().map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)
    }
}
