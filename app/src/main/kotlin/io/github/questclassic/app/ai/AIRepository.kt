package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

private val Context.aiStore by preferencesDataStore("ai_config")

/**
 * Settings for the user's AI assistant(s).
 *
 * - `assistantName`: name of the ACTIVE provider (shown in the UI, used for
 *   the "Hey {name}" wake phrase). Mirrored into [perBackendNames].
 * - `perBackendNames`: each backend keeps its own wake name so the user can
 *   set e.g. Gemini → "Nova", Claude → "Classic", OpenAI → "Sage" and the
 *   wake-word listener can route "Hey Nova …" to Gemini regardless of
 *   which provider is currently active.
 * - `perBackendKeys`: each backend keeps its own API key / endpoint so
 *   switching backend doesn't wipe the one you configured earlier.
 */
data class AIConfig(
    val assistantName: String,
    val backendId: String,
    val model: String,
    val apiKey: String,
    val systemPrompt: String,
    val perBackendNames: Map<String, String> = emptyMap(),
    val perBackendKeys: Map<String, String> = emptyMap(),
) {
    /** Name this backend answers to when the user says "Hey {name} …". */
    fun wakeNameFor(backendId: String, fallback: String = "Classic"): String =
        perBackendNames[backendId]?.ifBlank { null } ?: fallback
}

/**
 * Persistence + registry for the user's AI assistant. API keys are stored in
 * DataStore under the app-private directory; they never leave the device
 * except in the HTTPS request to the chosen backend.
 */
class AIRepository(private val context: Context, http: OkHttpClient) {
    val backends: List<AIBackend> = listOf(
        ClaudeBackend(http),
        OpenAIBackend(http),
        GeminiBackend(http),
        OllamaBackend(http),
    )

    fun backendById(id: String): AIBackend =
        backends.firstOrNull { it.id == id } ?: backends.first()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    private val NAME = stringPreferencesKey("name")
    private val BACKEND = stringPreferencesKey("backend")
    private val MODEL = stringPreferencesKey("model")
    private val KEY = stringPreferencesKey("api_key")
    private val PROMPT = stringPreferencesKey("prompt")
    private val PER_BACKEND_NAMES = stringPreferencesKey("per_backend_names")
    private val PER_BACKEND_KEYS = stringPreferencesKey("per_backend_keys")

    suspend fun load(): AIConfig {
        val prefs = context.aiStore.data.map { it }.first()
        val names = prefs[PER_BACKEND_NAMES]
            ?.let { runCatching { json.decodeFromString(mapSerializer, it) }.getOrNull() }
            .orEmpty()
        val keys = prefs[PER_BACKEND_KEYS]
            ?.let { runCatching { json.decodeFromString(mapSerializer, it) }.getOrNull() }
            .orEmpty()
        val activeBackend = prefs[BACKEND].orEmpty().ifBlank { "claude" }
        val activeName = prefs[NAME].orEmpty().ifBlank { names[activeBackend] ?: "Classic" }
        val activeKey = prefs[KEY].orEmpty().ifBlank { keys[activeBackend].orEmpty() }
        return AIConfig(
            assistantName = activeName,
            backendId = activeBackend,
            model = prefs[MODEL].orEmpty(),
            apiKey = activeKey,
            systemPrompt = prefs[PROMPT].orEmpty(),
            perBackendNames = names,
            perBackendKeys = keys,
        )
    }

    suspend fun save(cfg: AIConfig) {
        // When saving, update the per-backend maps with the active values so
        // switching providers later restores their own name + key.
        val updatedNames = cfg.perBackendNames.toMutableMap().apply {
            put(cfg.backendId, cfg.assistantName)
        }
        val updatedKeys = cfg.perBackendKeys.toMutableMap().apply {
            put(cfg.backendId, cfg.apiKey)
        }
        context.aiStore.edit { prefs ->
            prefs[NAME] = cfg.assistantName
            prefs[BACKEND] = cfg.backendId
            prefs[MODEL] = cfg.model
            prefs[KEY] = cfg.apiKey
            prefs[PROMPT] = cfg.systemPrompt
            prefs[PER_BACKEND_NAMES] = json.encodeToString(mapSerializer, updatedNames)
            prefs[PER_BACKEND_KEYS] = json.encodeToString(mapSerializer, updatedKeys)
        }
    }
}
