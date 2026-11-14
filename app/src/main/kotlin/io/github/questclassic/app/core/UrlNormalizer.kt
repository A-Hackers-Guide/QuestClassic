package io.github.questclassic.app.core

/**
 * Normalises the URLs the user types into the AI settings form or the
 * Devices panel — so 'ollama.local:11434' and
 * '  Https://Ollama.Local:11434/  ' become the same canonical string.
 *
 * Also catches a handful of common paste mistakes before they reach
 * [WifiReachability]:
 *   - embedded spaces (pasted a two-line URL)
 *   - trailing punctuation from a copy from prose
 *   - a Postman-style URL with a scheme + path collapsed as 'http://url/'
 *
 * Pure function; unit-testable.
 */
object UrlNormalizer {

    sealed interface Verdict {
        val value: String?
        val reason: String?

        data class Ok(val url: String) : Verdict {
            override val value: String = url
            override val reason: String? = null
        }
        data class Error(override val reason: String) : Verdict {
            override val value: String? = null
        }
    }

    fun normalize(raw: String, defaultScheme: String = "http"): Verdict {
        val t = raw.trim().trimEnd('.', ',', ';', ':', ')', ']', '>')
        if (t.isEmpty()) return Verdict.Error("empty")
        if (t.contains(' ') || t.contains('\n')) return Verdict.Error("URL has whitespace")
        val withScheme = if (t.contains("://")) t else "$defaultScheme://$t"
        val parsed = runCatching { java.net.URI(withScheme) }.getOrElse {
            return Verdict.Error("bad URL: ${it.message}")
        }
        val scheme = parsed.scheme?.lowercase() ?: return Verdict.Error("no scheme")
        if (scheme !in VALID_SCHEMES) return Verdict.Error("unsupported scheme '$scheme'")
        val host = parsed.host ?: return Verdict.Error("no host")
        val port = if (parsed.port > 0) ":${parsed.port}" else ""
        val path = parsed.path.orEmpty().trimEnd('/')
        return Verdict.Ok("$scheme://${host.lowercase()}$port$path")
    }

    /** True if [host] is a loopback / link-local / RFC1918 private address.
     *  Used by the AI settings form to shrug off a "this URL is HTTP not
     *  HTTPS" warning when the endpoint is obviously LAN-only. */
    fun isLanOnly(host: String): Boolean {
        val h = host.lowercase().trim()
        return h == "localhost" ||
            h.endsWith(".local") ||
            h.startsWith("10.") ||
            h.startsWith("192.168.") ||
            h.startsWith("127.") ||
            (h.startsWith("172.") && h.substringAfter("172.").substringBefore('.').toIntOrNull()
                ?.let { it in 16..31 } == true)
    }

    private val VALID_SCHEMES = setOf("http", "https")
}
