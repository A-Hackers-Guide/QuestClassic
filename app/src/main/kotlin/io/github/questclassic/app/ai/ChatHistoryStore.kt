package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.chatHistoryStore by preferencesDataStore("ai_chat_history")

/**
 * Persists AI conversation history **per backend** so that a user can hop
 * between Claude / OpenAI / Gemini / Ollama without losing the thread they
 * were having with each.
 *
 * - One DataStore row per backend id, holding the JSON-encoded list of
 *   [ChatMessage]s.
 * - 200-message cap per backend (configurable via [MAX_MESSAGES]) so a
 *   runaway loop doesn't fill the app-private partition.
 * - All I/O is suspend; callers use coroutines.
 */
class ChatHistoryStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(ChatMessage.serializer())

    /** Hard upper bound on retained messages per backend. */
    companion object { const val MAX_MESSAGES: Int = 200 }

    private fun keyFor(backendId: String) =
        stringPreferencesKey("history/${backendId.ifBlank { "unknown" }}")

    /** Read the conversation for `backendId`. Empty if nothing saved yet. */
    suspend fun load(backendId: String): List<ChatMessage> {
        val raw = context.chatHistoryStore.data.map { it[keyFor(backendId)] }.first()
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(serializer, raw)
        }.getOrElse { emptyList() }
    }

    /** Save the conversation for `backendId`, truncating to [MAX_MESSAGES]. */
    suspend fun save(backendId: String, messages: List<ChatMessage>) {
        val trimmed = if (messages.size > MAX_MESSAGES) {
            messages.takeLast(MAX_MESSAGES)
        } else messages
        val encoded = json.encodeToString(serializer, trimmed)
        context.chatHistoryStore.edit { it[keyFor(backendId)] = encoded }
    }

    /** Wipe the conversation for `backendId`. Other backends are untouched. */
    suspend fun clear(backendId: String) {
        context.chatHistoryStore.edit { it.remove(keyFor(backendId)) }
    }
}
