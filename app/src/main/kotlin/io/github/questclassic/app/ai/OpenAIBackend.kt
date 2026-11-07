package io.github.questclassic.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAIBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "openai"
    override val displayName = "OpenAI"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Msg(val role: String, val content: String)
    @Serializable
    private data class Req(val model: String, val messages: List<Msg>, val max_tokens: Int = 1024)
    @Serializable
    private data class Choice(val message: Msg)
    @Serializable
    private data class Resp(val choices: List<Choice> = emptyList())

    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val msgs = buildList {
                if (systemPrompt.isNotBlank()) add(Msg("system", systemPrompt))
                history.forEach { add(Msg(it.role.name, it.content)) }
            }
            val payload = Req(model = model ?: "gpt-4o-mini", messages = msgs)
            val body = json.encodeToString(Req.serializer(), payload)
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("OpenAI API ${resp.code}: ${resp.body?.string()}")
                val parsed = json.decodeFromString(Resp.serializer(), resp.body?.string() ?: "")
                parsed.choices.firstOrNull()?.message?.content.orEmpty()
            }
        }
    }
}
