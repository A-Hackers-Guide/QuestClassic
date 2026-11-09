package io.github.questclassic.app.core

import android.content.Context
import io.github.questclassic.app.ai.Voice
import io.github.questclassic.app.ui.panels.BuildToolsPrefs
import io.github.questclassic.app.ui.panels.Widget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Export / import the user's QuestClassic configuration as a single JSON
 * document, so they can move settings between devices without manually
 * copying over every DataStore row.
 *
 * What goes IN the backup:
 *  - AI provider config (names, models, system prompt; **not** API keys —
 *    those stay device-local by policy)
 *  - Device registry (metadata only, as per [AppContainer.devices])
 *  - Voice registry (built-in picks; NOT cloned-voice sample paths —
 *    those only make sense on the device that captured them)
 *  - Passthrough state (3 toggles)
 *  - Build Tools widget layout (by enum name)
 *
 * What stays OUT of the backup, by policy:
 *  - API keys (each backend's `apiKey`) — never travel off-device
 *  - Cloned voice training artifacts — rooted in app-private storage
 *  - Chat history — treat as ephemeral + possibly sensitive
 *  - Payload-verifier state and update-channel caches
 */
@Serializable
data class ConfigBackup(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long,
    val ai: AIBackupSlice? = null,
    val devices: List<Device>? = null,
    val voices: List<Voice>? = null,
    val passthroughCamera: Boolean? = null,
    val passthroughAudio: Boolean? = null,
    val passthroughText: Boolean? = null,
    val buildToolsEnabled: List<String>? = null,
) {
    companion object {
        /** Bump when any field's meaning changes (not just added fields). */
        const val CURRENT_VERSION: Int = 1
    }
}

@Serializable
data class AIBackupSlice(
    val assistantName: String,
    val backendId: String,
    val model: String,
    val systemPrompt: String,
    val perBackendNames: Map<String, String>,
)

class ConfigBackupController(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Build a snapshot from the live container state. Does NOT write
     * anywhere — the caller owns the resulting String (write to a file,
     * copy to clipboard, share, …).
     */
    suspend fun export(container: AppContainer): String {
        val cfg = container.ai.load()
        val pass = container.passthrough.load()
        val backup = ConfigBackup(
            createdAt = System.currentTimeMillis(),
            ai = AIBackupSlice(
                assistantName = cfg.assistantName,
                backendId = cfg.backendId,
                model = cfg.model,
                systemPrompt = cfg.systemPrompt,
                perBackendNames = cfg.perBackendNames,
            ),
            devices = container.devices.load(),
            voices = container.voices.load(),
            passthroughCamera = pass.cameraPassthrough,
            passthroughAudio = pass.audioPassthrough,
            passthroughText = pass.textPassthrough,
            buildToolsEnabled = BuildToolsPrefs(context).loadEnabled()?.map { it.name },
        )
        return json.encodeToString(ConfigBackup.serializer(), backup)
    }

    /**
     * Restore a backup. Overwrites existing settings for sections present
     * in the payload. API keys are NOT included in backups, so the user
     * will have to re-enter them after restore.
     *
     * Returns a [RestoreSummary] the UI can show the user.
     */
    suspend fun import(container: AppContainer, payload: String): RestoreSummary {
        val backup = runCatching {
            json.decodeFromString(ConfigBackup.serializer(), payload)
        }.getOrElse {
            return RestoreSummary(ok = false, reason = "parse error: ${it.message}")
        }

        if (backup.version != ConfigBackup.CURRENT_VERSION) {
            return RestoreSummary(
                ok = false,
                reason = "unsupported backup version ${backup.version} (expected ${ConfigBackup.CURRENT_VERSION})",
            )
        }

        var aiRestored = false
        var devicesRestored = 0
        var voicesRestored = 0
        var passthroughRestored = false
        var buildToolsRestored = 0

        backup.ai?.let { slice ->
            val existing = container.ai.load()
            container.ai.save(
                existing.copy(
                    assistantName = slice.assistantName,
                    backendId = slice.backendId,
                    model = slice.model,
                    systemPrompt = slice.systemPrompt,
                    perBackendNames = slice.perBackendNames,
                )
            )
            aiRestored = true
        }
        backup.devices?.let { list ->
            container.devices.save(list)
            devicesRestored = list.size
        }
        backup.voices?.let { list ->
            container.voices.save(list)
            voicesRestored = list.size
        }
        if (backup.passthroughCamera != null ||
            backup.passthroughAudio != null ||
            backup.passthroughText != null
        ) {
            val current = container.passthrough.load()
            container.passthrough.save(
                PassthroughState(
                    cameraPassthrough = backup.passthroughCamera ?: current.cameraPassthrough,
                    audioPassthrough = backup.passthroughAudio ?: current.audioPassthrough,
                    textPassthrough = backup.passthroughText ?: current.textPassthrough,
                )
            )
            passthroughRestored = true
        }
        backup.buildToolsEnabled?.let { names ->
            val byName = Widget.entries.associateBy { it.name }
            val widgets = names.mapNotNull { byName[it] }
            BuildToolsPrefs(context).saveEnabled(widgets)
            buildToolsRestored = widgets.size
        }

        return RestoreSummary(
            ok = true,
            aiRestored = aiRestored,
            devicesRestored = devicesRestored,
            voicesRestored = voicesRestored,
            passthroughRestored = passthroughRestored,
            buildToolsRestored = buildToolsRestored,
        )
    }
}

data class RestoreSummary(
    val ok: Boolean,
    val reason: String = "",
    val aiRestored: Boolean = false,
    val devicesRestored: Int = 0,
    val voicesRestored: Int = 0,
    val passthroughRestored: Boolean = false,
    val buildToolsRestored: Int = 0,
) {
    fun humanLine(): String = if (!ok) "restore failed: $reason" else buildString {
        append("restored: ")
        if (aiRestored) append("AI config, ")
        if (devicesRestored > 0) append("$devicesRestored devices, ")
        if (voicesRestored > 0) append("$voicesRestored voices, ")
        if (passthroughRestored) append("passthrough, ")
        if (buildToolsRestored > 0) append("$buildToolsRestored widgets, ")
        if (endsWith(", ")) setLength(length - 2)
        append(" (re-enter API keys manually)")
    }
}
