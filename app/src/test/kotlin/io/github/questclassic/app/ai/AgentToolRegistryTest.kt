package io.github.questclassic.app.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AgentToolRegistryTest {

    @After
    fun tearDown() = AgentToolRegistry.reset()

    private fun stubTool(
        id: String = "device.battery",
        cat: AgentToolCategory = AgentToolCategory.DEVICE_INFO,
    ) = AgentTool(
        id = id,
        displayName = "Battery",
        description = "Current battery level",
        category = cat,
        safety = SafetyLevel.READ_ONLY,
        params = emptyList(),
        invoke = { ToolResult(ok = true, summary = "87%", payload = JsonPrimitive(87)) },
    )

    @Test
    fun `register then get`() {
        AgentToolRegistry.register(stubTool())
        val t = AgentToolRegistry.get("device.battery")
        assertNotNull(t)
        assertEquals("Battery", t!!.displayName)
    }

    @Test
    fun `duplicate id throws`() {
        AgentToolRegistry.register(stubTool())
        try {
            AgentToolRegistry.register(stubTool())
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun `unregister drops the tool`() {
        AgentToolRegistry.register(stubTool())
        AgentToolRegistry.unregister("device.battery")
        assertNull(AgentToolRegistry.get("device.battery"))
    }

    @Test
    fun `byCategory filters`() {
        AgentToolRegistry.register(stubTool(id = "device.battery"))
        AgentToolRegistry.register(stubTool(id = "nav.open", cat = AgentToolCategory.NAVIGATION))
        assertEquals(1, AgentToolRegistry.byCategory(AgentToolCategory.DEVICE_INFO).size)
        assertEquals(1, AgentToolRegistry.byCategory(AgentToolCategory.NAVIGATION).size)
        assertTrue(AgentToolRegistry.byCategory(AgentToolCategory.FEEDBACK).isEmpty())
    }

    @Test
    fun `reset clears everything`() {
        AgentToolRegistry.register(stubTool())
        AgentToolRegistry.reset()
        assertTrue(AgentToolRegistry.all().isEmpty())
    }

    @Test
    fun `safety has 5 levels covering the default allow-list and the deny`() {
        val levels = SafetyLevel.values().toSet()
        assertTrue(SafetyLevel.READ_ONLY in levels)
        assertTrue(SafetyLevel.WRITE_LOCAL in levels)
        assertTrue(SafetyLevel.NETWORK in levels)
        assertTrue(SafetyLevel.SIDE_EFFECT in levels)
        assertTrue(SafetyLevel.CROSS_DEVICE in levels)
        assertEquals(5, levels.size)
    }
}
