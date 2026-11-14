package io.github.questclassic.app.ai

/**
 * Turns raw HTTP status codes from AIBackend calls into user-facing
 * messages. The provider-specific body is often JSON with jargon ({
 * "error": { "type": "insufficient_quota", "code": "billing_hard_limit"}}),
 * which the user doesn't care about. This module maps the status code
 * into a short actionable sentence.
 *
 * Provider parameter lets us vary copy ("add credits" for OpenAI,
 * "check your Anthropic console" for Claude). Everything here is pure
 * strings; no network calls.
 */
object HttpErrorMapper {

    /** Returns null when the status code is informational / success /
     *  redirect and the caller should treat the response as handled. */
    fun humanize(statusCode: Int, backendId: String): String? {
        val provider = displayName(backendId)
        return when (statusCode) {
            in 200..299 -> null
            in 300..399 -> null
            400 -> "$provider rejected the request as malformed. The request body was too long or the model id is wrong."
            401 -> "$provider rejected the API key. Open Settings → ${provider}, check for leading spaces and whether the key was revoked."
            403 -> "$provider refused this key for this model. Check your ${provider} plan tier."
            404 -> "$provider says the model id doesn't exist. Double-check spelling in Settings."
            408 -> "Request timed out. Network looks slow — retry."
            413 -> "Conversation too long for $provider's context window. /clear and start fresh, or shorten your system prompt."
            429 -> "$provider rate-limited us. Wait a few seconds and send again. If this keeps happening, check your plan limits."
            500 -> "$provider internal error. Not your fault; wait a minute and retry."
            502, 503, 504 -> "$provider is down or restarting. Status page: ${statusPage(backendId)}"
            in 400..499 -> "$provider rejected the request (HTTP $statusCode). No actionable detail."
            in 500..599 -> "$provider is having trouble (HTTP $statusCode). Retry in a minute."
            else -> "Unexpected HTTP $statusCode from $provider."
        }
    }

    /** Small wrapper the backends can use at error time. */
    fun formatError(statusCode: Int, backendId: String, body: String?): String {
        val header = humanize(statusCode, backendId) ?: "HTTP $statusCode"
        val trimmed = body?.take(200)?.trim().orEmpty()
        return if (trimmed.isBlank()) header else "$header\n($trimmed)"
    }

    private fun displayName(backendId: String): String = when (backendId.lowercase()) {
        "claude" -> "Anthropic"
        "openai" -> "OpenAI"
        "gemini" -> "Google"
        "ollama" -> "Ollama"
        else -> backendId
    }

    private fun statusPage(backendId: String): String = when (backendId.lowercase()) {
        "claude" -> "status.anthropic.com"
        "openai" -> "status.openai.com"
        "gemini" -> "status.cloud.google.com"
        else -> "the provider's status page"
    }
}
