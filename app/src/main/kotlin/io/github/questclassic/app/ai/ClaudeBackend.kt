package io.github.questclassic.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ClaudeBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "claude"
    override val displayName = "Claude (Anthropic)"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class ReqMsg(val role: String, val content: String)
    @Serializable
    private data class Req(val model: String, val max_tokens: Int, val system: String?, val messages: List<ReqMsg>)
    @Serializable
    private data class RespContent(val type: String, val text: String? = null)
    @Serializable
    private data class Resp(val content: List<RespContent> = emptyList())

    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = Req(
                model = model ?: "claude-sonnet-5-5",
                max_tokens = 1024,
                system = systemPrompt.takeIf { it.isNotBlank() },
                messages = history.filter { it.role != Role.system }
                    .map { ReqMsg(role = if (it.role == Role.assistant) "assistant" else "user", content = it.content) },
            )
            val body = json.encodeToString(Req.serializer(), payload)
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Claude API ${resp.code}: ${resp.body?.string()}")
                val parsed = json.decodeFromString(Resp.serializer(), resp.body?.string() ?: "")
                parsed.content.firstOrNull { it.type == "text" }?.text.orEmpty()
            }
        }
    }
}
