package io.github.questclassic.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Local Ollama server — great for privacy-first use. The user runs Ollama on their LAN
 * (or on another Quest if they're that kind of person) and points this at it.
 *
 * `apiKey` is repurposed as the base URL for this backend, since Ollama doesn't use auth.
 * Default: http://10.0.0.1:11434
 */
class OllamaBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "ollama"
    override val displayName = "Ollama (local LAN)"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Msg(val role: String, val content: String)
    @Serializable
    private data class Req(val model: String, val messages: List<Msg>, val stream: Boolean = false)
    @Serializable
    private data class RespMsg(val role: String, val content: String)
    @Serializable
    private data class Resp(val message: RespMsg? = null)

    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = apiKey.trimEnd('/').ifBlank { "http://10.0.0.1:11434" }
            val msgs = buildList {
                if (systemPrompt.isNotBlank()) add(Msg("system", systemPrompt))
                history.forEach { add(Msg(it.role.name, it.content)) }
            }
            val payload = Req(model = model ?: "llama3.2", messages = msgs)
            val body = json.encodeToString(Req.serializer(), payload)
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("$base/api/chat").post(body).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Ollama ${resp.code}: ${resp.body?.string()}")
                val parsed = json.decodeFromString(Resp.serializer(), resp.body?.string() ?: "")
                parsed.message?.content.orEmpty()
            }
        }
    }
}
