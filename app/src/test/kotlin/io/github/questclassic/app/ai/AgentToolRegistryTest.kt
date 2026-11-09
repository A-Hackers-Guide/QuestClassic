package io.github.questclassic.app.ai

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolRegistryTest {

    @After
    fun tearDown() = AgentToolRegistry.reset()

    private fun stub(id: String, cat: AgentToolCategory = AgentToolCategory.META) = AgentTool(
        id = id,
        displayName = id,
        description = "",
        category = cat,
        safety = SafetyLevel.READ_ONLY,
        params = emptyList(),
        invoke = { ToolResult(ok = true, summary = id) },
    )

    @Test
    fun `register and lookup`() {
        AgentToolRegistry.register(stub("a"))
        assertNotNull(AgentToolRegistry.get("a"))
        assertEquals(1, AgentToolRegistry.all().size)
    }

    @Test
    fun `duplicate id throws`() {
        AgentToolRegistry.register(stub("a"))
        val threw = runCatching { AgentToolRegistry.register(stub("a")) }.isFailure
        assertTrue(threw)
    }

    @Test
    fun `unregister removes the tool`() {
        AgentToolRegistry.register(stub("a"))
        AgentToolRegistry.unregister("a")
        assertNull(AgentToolRegistry.get("a"))
    }

    @Test
    fun `byCategory filters`() {
        AgentToolRegistry.register(stub("a", AgentToolCategory.META))
        AgentToolRegistry.register(stub("b", AgentToolCategory.DEVICE_INFO))
        AgentToolRegistry.register(stub("c", AgentToolCategory.META))
        assertEquals(2, AgentToolRegistry.byCategory(AgentToolCategory.META).size)
        assertEquals(1, AgentToolRegistry.byCategory(AgentToolCategory.DEVICE_INFO).size)
    }

    @Test
    fun `invoke returns the declared summary`() {
        AgentToolRegistry.register(stub("echo"))
        val res = runBlocking { AgentToolRegistry.get("echo")!!.invoke(buildJsonObject {}) }
        assertEquals(true, res.ok)
        assertEquals("echo", res.summary)
    }

    @Test
    fun `reset empties the registry`() {
        AgentToolRegistry.register(stub("a"))
        AgentToolRegistry.register(stub("b"))
        AgentToolRegistry.reset()
        assertEquals(0, AgentToolRegistry.all().size)
    }
}
