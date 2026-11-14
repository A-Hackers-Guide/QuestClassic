package io.github.questclassic.app.ai

import io.github.questclassic.app.core.SecretRedactor
import kotlinx.serialization.json.Json

/**
 * Serialises a ChatHistoryStore thread into a form the user can share.
 *
 * Two output formats:
 *   - Markdown: readable, pasteable in GitHub issues / Discord.
 *   - JSON:     round-trippable, for reimport or external tooling.
 *
 * Both run every message through [SecretRedactor] so a key accidentally
 * pasted into a conversation doesn't leak through the export. The user
 * is still responsible for reading what they share — this is a backstop,
 * not a filter.
 *
 * Pure function; unit-testable without the network or DataStore.
 */
object TranscriptExporter {

    private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

    enum class Format { MARKDOWN, JSON }

    /** Entry point. */
    fun export(
        backendId: String,
        assistantName: String,
        history: List<ChatMessage>,
        format: Format,
    ): String = when (format) {
        Format.MARKDOWN -> markdown(backendId, assistantName, history)
        Format.JSON -> json(backendId, assistantName, history)
    }

    private fun markdown(
        backendId: String,
        assistantName: String,
        history: List<ChatMessage>,
    ): String {
        val sb = StringBuilder()
        sb.append("# Transcript — $assistantName (").append(backendId).append(")\n\n")
        sb.append("Messages: ${history.size}\n\n")
        for (msg in history) {
            val speaker = when (msg.role) {
                Role.user -> "**You**"
                Role.assistant -> "**$assistantName**"
                Role.system -> "_system_"
            }
            sb.append(speaker).append("\n\n")
            sb.append(SecretRedactor.redact(msg.content).trim()).append("\n\n")
        }
        return sb.toString().trimEnd() + "\n"
    }

    private fun json(
        backendId: String,
        assistantName: String,
        history: List<ChatMessage>,
    ): String {
        val redacted = history.map {
            ChatMessage(role = it.role, content = SecretRedactor.redact(it.content))
        }
        val wrapper = Envelope(
            backendId = backendId,
            assistantName = assistantName,
            messages = redacted,
        )
        return prettyJson.encodeToString(Envelope.serializer(), wrapper)
    }

    @kotlinx.serialization.Serializable
    data class Envelope(
        val version: Int = 1,
        val backendId: String,
        val assistantName: String,
        val messages: List<ChatMessage>,
    )
}
