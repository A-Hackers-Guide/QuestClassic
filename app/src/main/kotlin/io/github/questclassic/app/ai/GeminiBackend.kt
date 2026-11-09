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
 * Google Gemini (generative language) backend.
 *
 * Uses the public generativelanguage.googleapis.com endpoint with an API key
 * passed as a query parameter (Google's standard scheme for this surface).
 * The API key stays in device-private DataStore like the other backends; it
 * only leaves the device in the HTTPS request to this endpoint.
 */
class GeminiBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "gemini"
    override val displayName = "Gemini (Google)"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Part(val text: String)
    @Serializable
    private data class Content(val role: String, val parts: List<Part>)
    @Serializable
    private data class SystemInstruction(val parts: List<Part>)
    @Serializable
    private data class Req(
        val contents: List<Content>,
        val systemInstruction: SystemInstruction? = null,
    )

    @Serializable
    private data class RespContent(val parts: List<Part> = emptyList())
    @Serializable
    private data class Candidate(val content: RespContent? = null)
    @Serializable
    private data class Resp(val candidates: List<Candidate> = emptyList())

    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Gemini uses "user" / "model" rather than "user" / "assistant".
            val contents = history
                .filter { it.role != Role.system }
                .map { msg ->
                    Content(
                        role = if (msg.role == Role.assistant) "model" else "user",
                        parts = listOf(Part(msg.content)),
                    )
                }
            val payload = Req(
                contents = contents,
                systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                    ?.let { SystemInstruction(listOf(Part(it))) },
            )
            val body = json.encodeToString(Req.serializer(), payload)
                .toRequestBody("application/json".toMediaType())
            val chosen = model?.ifBlank { null } ?: "gemini-2.0-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/" +
                "models/$chosen:generateContent?key=$apiKey"
            val req = Request.Builder()
                .url(url)
                .post(body)
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("Gemini API ${resp.code}: ${resp.body?.string()}")
                }
                val parsed = json.decodeFromString(Resp.serializer(), resp.body?.string() ?: "")
                parsed.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    ?.joinToString("") { it.text }
                    .orEmpty()
            }
        }
    }
}
