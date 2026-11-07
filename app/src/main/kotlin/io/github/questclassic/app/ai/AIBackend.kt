package io.github.questclassic.app.ai

import kotlinx.serialization.Serializable

@Serializable
enum class Role { user, assistant, system }

@Serializable
data class ChatMessage(val role: Role, val content: String)

sealed interface AIBackend {
    val id: String
    val displayName: String
    suspend fun chat(systemPrompt: String, history: List<ChatMessage>, apiKey: String, model: String?): Result<String>
}
