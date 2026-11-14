package io.github.questclassic.app.core

/**
 * Parser for quest-classic://… deep links.
 *
 * Shapes we accept today:
 *   quest-classic://panel/ai
 *   quest-classic://panel/diagnostics-bundle
 *   quest-classic://pair?code=ABC-DEF-234
 *   quest-classic://switch?backend=claude
 *
 * The manifest entry that catches these lives in the Android manifest;
 * this is the pure parser Compose code calls once the intent arrives.
 *
 * Returns a sealed [Action] so the shell's when() can exhaustively
 * dispatch. Unknown links return null.
 */
object DeepLink {

    sealed interface Action {
        data class OpenPanel(val panelId: String) : Action
        data class Pair(val code: String) : Action
        data class SwitchBackend(val backendId: String) : Action
    }

    const val SCHEME: String = "quest-classic"

    fun parse(uri: String): Action? {
        val t = uri.trim()
        if (!t.startsWith("$SCHEME://")) return null
        val body = t.substring("$SCHEME://".length)
        val host = body.substringBefore('/').substringBefore('?')
        val path = body.substringAfter('/', missingDelimiterValue = "").substringBefore('?')
        val query = if ('?' in body) body.substringAfter('?') else ""
        val params = query.split('&')
            .filter { it.isNotEmpty() }
            .associate { kv ->
                val (k, v) = kv.split('=', limit = 2).let {
                    if (it.size == 2) it[0] to it[1] else it[0] to ""
                }
                decode(k) to decode(v)
            }
        return when (host) {
            "panel" -> if (path.isNotBlank()) Action.OpenPanel(path) else null
            "pair" -> params["code"]?.takeIf { it.isNotBlank() }?.let { Action.Pair(it) }
            "switch" -> params["backend"]?.takeIf { it.isNotBlank() }?.let { Action.SwitchBackend(it) }
            else -> null
        }
    }

    private fun decode(s: String): String = runCatching {
        java.net.URLDecoder.decode(s, "UTF-8")
    }.getOrDefault(s)
}
