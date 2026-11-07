package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient

private val Context.aiStore by preferencesDataStore("ai_config")

data class AIConfig(
    val assistantName: String,
    val backendId: String,
    val model: String,
    val apiKey: String,
    val systemPrompt: String,
)

/**
 * Persistence + registry for the user's AI assistant. The API key is stored in DataStore under
 * the app-private directory; it never leaves the device except in the HTTPS request to the chosen
 * backend.
 */
class AIRepository(private val context: Context, http: OkHttpClient) {
    val backends: List<AIBackend> = listOf(
        ClaudeBackend(http),
        OpenAIBackend(http),
        OllamaBackend(http),
    )

    fun backendById(id: String): AIBackend = backends.firstOrNull { it.id == id } ?: backends.first()

    private val NAME = stringPreferencesKey("name")
    private val BACKEND = stringPreferencesKey("backend")
    private val MODEL = stringPreferencesKey("model")
    private val KEY = stringPreferencesKey("api_key")
    private val PROMPT = stringPreferencesKey("prompt")

    suspend fun load(): AIConfig {
        val prefs = context.aiStore.data.map { it }.first()
        return AIConfig(
            assistantName = prefs[NAME].orEmpty().ifBlank { "Classic" },
            backendId = prefs[BACKEND].orEmpty().ifBlank { "claude" },
            model = prefs[MODEL].orEmpty(),
            apiKey = prefs[KEY].orEmpty(),
            systemPrompt = prefs[PROMPT].orEmpty(),
        )
    }

    suspend fun save(cfg: AIConfig) {
        context.aiStore.edit { prefs ->
            prefs[NAME] = cfg.assistantName
            prefs[BACKEND] = cfg.backendId
            prefs[MODEL] = cfg.model
            prefs[KEY] = cfg.apiKey
            prefs[PROMPT] = cfg.systemPrompt
        }
    }
}
