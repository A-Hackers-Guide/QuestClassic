package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkTest {

    @Test
    fun `panel link`() {
        val a = DeepLink.parse("quest-classic://panel/ai") as DeepLink.Action.OpenPanel
        assertEquals("ai", a.panelId)
    }

    @Test
    fun `panel link with hyphen`() {
        val a = DeepLink.parse("quest-classic://panel/diagnostics-bundle") as DeepLink.Action.OpenPanel
        assertEquals("diagnostics-bundle", a.panelId)
    }

    @Test
    fun `pair link`() {
        val a = DeepLink.parse("quest-classic://pair?code=ABC-DEF-234") as DeepLink.Action.Pair
        assertEquals("ABC-DEF-234", a.code)
    }

    @Test
    fun `switch link`() {
        val a = DeepLink.parse("quest-classic://switch?backend=claude") as DeepLink.Action.SwitchBackend
        assertEquals("claude", a.backendId)
    }

    @Test
    fun `unknown scheme returns null`() {
        assertNull(DeepLink.parse("https://example.com"))
    }

    @Test
    fun `missing code returns null`() {
        assertNull(DeepLink.parse("quest-classic://pair"))
    }

    @Test
    fun `url-encoded values decoded`() {
        val a = DeepLink.parse("quest-classic://switch?backend=hey%20claude") as DeepLink.Action.SwitchBackend
        assertEquals("hey claude", a.backendId)
    }

    @Test
    fun `empty panel path returns null`() {
        assertNull(DeepLink.parse("quest-classic://panel/"))
    }
}
