package io.github.questclassic.app.ai

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import io.github.questclassic.app.core.AppContainer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Registers the "built-in" tool catalog so that the AI companion can do
 * useful things out of the box without the user wiring tools by hand.
 *
 * Every tool here is READ_ONLY or WRITE_LOCAL — nothing that reaches the
 * network or another device. The CROSS_DEVICE / NETWORK / SIDE_EFFECT
 * tiers are intentionally left for later per-feature registration so the
 * user sees them as explicit opt-ins in Settings.
 *
 * Idempotent: on repeated calls the registry is reset first so hot-reloads
 * don't throw "duplicate tool id".
 */
object AgentToolSeed {

    fun seedAll(container: AppContainer, context: Context) {
        AgentToolRegistry.reset()

        AgentToolRegistry.register(
            AgentTool(
                id = "device.info",
                displayName = "Device info",
                description = "Return manufacturer, board, model, Android version.",
                category = AgentToolCategory.DEVICE_INFO,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    ToolResult(
                        ok = true,
                        summary = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}",
                        payload = buildJsonObject {
                            put("manufacturer", Build.MANUFACTURER)
                            put("model", Build.MODEL)
                            put("board", Build.BOARD)
                            put("androidRelease", Build.VERSION.RELEASE)
                            put("sdkInt", Build.VERSION.SDK_INT)
                            put("securityPatch", Build.VERSION.SECURITY_PATCH)
                        },
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "device.battery",
                displayName = "Battery",
                description = "Current battery percent and charging state.",
                category = AgentToolCategory.DEVICE_INFO,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                    val charging = bm?.isCharging ?: false
                    ToolResult(
                        ok = pct >= 0,
                        summary = if (pct >= 0) "$pct%${if (charging) " (charging)" else ""}" else "unavailable",
                        payload = buildJsonObject {
                            put("percent", pct)
                            put("charging", charging)
                        },
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "device.uptime",
                displayName = "Uptime",
                description = "Seconds since the device booted.",
                category = AgentToolCategory.DEVICE_INFO,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val sec = SystemClock.elapsedRealtime() / 1000
                    ToolResult(
                        ok = true,
                        summary = humanDuration(sec),
                        payload = JsonPrimitive(sec),
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "ai.list_providers",
                displayName = "List AI providers",
                description = "Enumerate registered AI backends and their wake names.",
                category = AgentToolCategory.META,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val cfg = container.ai.load()
                    val rows = container.ai.backends.map { b ->
                        buildJsonObject {
                            put("id", b.id)
                            put("displayName", b.displayName)
                            put("wakeName", cfg.wakeNameFor(b.id, b.displayName))
                            put("active", b.id == cfg.backendId)
                        }
                    }
                    ToolResult(
                        ok = true,
                        summary = "${rows.size} providers; active = ${cfg.backendId}",
                        payload = kotlinx.serialization.json.JsonArray(rows),
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "ai.whoami",
                displayName = "AI self-identify",
                description = "Return the active assistant's name, backend and model.",
                category = AgentToolCategory.META,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val cfg = container.ai.load()
                    ToolResult(
                        ok = true,
                        summary = "${cfg.assistantName} · ${cfg.backendId} · ${cfg.model}",
                        payload = buildJsonObject {
                            put("name", cfg.assistantName)
                            put("backendId", cfg.backendId)
                            put("model", cfg.model)
                        },
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "devices.list",
                displayName = "List devices",
                description = "Return registered companion / server / smart-home devices.",
                category = AgentToolCategory.DEVICES,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val list = container.devices.load()
                    ToolResult(
                        ok = true,
                        summary = "${list.size} devices registered",
                        payload = buildJsonObject {
                            put("count", list.size)
                            put("names", kotlinx.serialization.json.JsonArray(
                                list.map { JsonPrimitive(it.name) }
                            ))
                        },
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "voices.list",
                displayName = "List voices",
                description = "Return built-in and cloned voices available.",
                category = AgentToolCategory.META,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val list = container.voices.load()
                    ToolResult(
                        ok = true,
                        summary = "${list.size} voices available",
                        payload = buildJsonObject {
                            put("count", list.size)
                            put("names", kotlinx.serialization.json.JsonArray(
                                list.map { JsonPrimitive(it.name) }
                            ))
                        },
                    )
                },
            )
        )

        AgentToolRegistry.register(
            AgentTool(
                id = "meta.locale",
                displayName = "Locale",
                description = "Current device locale and timezone.",
                category = AgentToolCategory.DEVICE_INFO,
                safety = SafetyLevel.READ_ONLY,
                params = emptyList(),
                invoke = {
                    val loc = Locale.getDefault()
                    val tz = java.util.TimeZone.getDefault()
                    ToolResult(
                        ok = true,
                        summary = "${loc.toLanguageTag()} · ${tz.id}",
                        payload = buildJsonObject {
                            put("languageTag", loc.toLanguageTag())
                            put("displayName", loc.displayName)
                            put("timezoneId", tz.id)
                        },
                    )
                },
            )
        )
    }

    private fun humanDuration(seconds: Long): String {
        val d = seconds / 86_400
        val h = (seconds % 86_400) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString {
            if (d > 0) append("${d}d ")
            if (h > 0 || d > 0) append("${h}h ")
            if (m > 0 || h > 0 || d > 0) append("${m}m ")
            append("${s}s")
        }.trim()
    }
}

// Opt-in suspend wrapper when a tool wants to shell out to a disk/IO
// operation; invokes the given block on Dispatchers.IO.
suspend fun <T> withIo(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
