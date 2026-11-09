package io.github.questclassic.app.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Programmable-AI tool catalog.
 *
 * The AI companion can do more than reply with text — the user asked for
 * it to be "programmable". This registry defines the safe, allowlisted
 * function-call surface the model can trigger. Each tool:
 *
 *  - has a stable [id]
 *  - declares its params in a tiny JSON schema the model sees in the
 *    system prompt
 *  - runs in a bounded impl via [invoke]
 *  - has a [safety] level the user can tighten in Settings
 *
 * Tools are not auto-loaded. Each feature that wants to expose tools
 * calls [register] during AppContainer construction; the user can then
 * enable/disable them individually.
 */
object AgentToolRegistry {

    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        require(!tools.containsKey(tool.id)) { "duplicate tool id: ${tool.id}" }
        tools[tool.id] = tool
    }

    fun unregister(id: String) {
        tools.remove(id)
    }

    fun get(id: String): AgentTool? = tools[id]

    fun all(): List<AgentTool> = tools.values.toList()

    fun byCategory(cat: AgentToolCategory): List<AgentTool> =
        tools.values.filter { it.category == cat }

    /** Clear everything — tests and app shutdown use this. */
    fun reset() { tools.clear() }
}

/** One safe function the AI model can call. */
data class AgentTool(
    val id: String,
    val displayName: String,
    val description: String,
    val category: AgentToolCategory,
    val safety: SafetyLevel,
    val params: List<ToolParam>,
    val invoke: suspend (JsonObject) -> ToolResult,
)

@Serializable
enum class AgentToolCategory {
    /** Read-only queries about the device (battery, uptime, installed apps). */
    DEVICE_INFO,

    /** Launch apps, open URLs, change panels. */
    NAVIGATION,

    /** Haptic / audio feedback. */
    FEEDBACK,

    /** AI self-management (rename assistant, list providers). */
    META,

    /** Device-registry CRUD. */
    DEVICES,

    /** Virtual-monitor actions. */
    DISPLAYS,

    /** Emulator launching (never ROM distribution). */
    EMULATORS,
}

@Serializable
enum class SafetyLevel {
    /** Pure reads. Default-allowed. */
    READ_ONLY,

    /** Writes to app-private state (config, preferences). Default-allowed. */
    WRITE_LOCAL,

    /** Network calls to user-approved endpoints. Default-allowed. */
    NETWORK,

    /** Visible side-effects (opens another app, flips passthrough).
     *  Default-allowed but surfaces a one-line confirmation chip. */
    SIDE_EFFECT,

    /** Cross-device or cross-user side-effects. Default-denied. */
    CROSS_DEVICE,
}

data class ToolParam(
    val name: String,
    val kind: ParamKind,
    val description: String,
    val required: Boolean = true,
)

@Serializable
enum class ParamKind { STRING, INT, BOOLEAN, ENUM }

data class ToolResult(
    val ok: Boolean,
    val summary: String,
    val payload: JsonElement = JsonPrimitive(""),
)
