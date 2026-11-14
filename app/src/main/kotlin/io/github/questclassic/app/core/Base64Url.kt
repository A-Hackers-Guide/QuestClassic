package io.github.questclassic.app.core

import java.util.Base64

/**
 * URL-safe base64 helpers (RFC 4648 §5). Used by the pairing code
 * flow, deep-link payloads, and the diagnostics bundle attachment
 * encoder.
 *
 * - Encodes to the '-_' alphabet with no padding.
 * - Decodes both padded and non-padded inputs, both alphabets.
 * - Rejects inputs containing characters outside the base64 range
 *   (including whitespace) so a copy-paste typo doesn't decode to
 *   garbage silently.
 */
object Base64Url {

    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()
    private val stdDecoder: Base64.Decoder = Base64.getDecoder()
    private val SAFE_CHARS = Regex("""^[A-Za-z0-9_\-+/=]*$""")

    fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)
    fun encode(text: String): String = encode(text.toByteArray(Charsets.UTF_8))

    /**
     * Decode a URL-safe string back to bytes. Accepts standard-alphabet
     * input too (so pasted `+/` strings still decode). Throws
     * IllegalArgumentException on invalid input.
     */
    fun decode(input: String): ByteArray {
        val t = input.trim()
        if (t.isEmpty()) return ByteArray(0)
        require(SAFE_CHARS.matches(t)) { "input contains non-base64 chars" }
        return try {
            decoder.decode(t)
        } catch (_: IllegalArgumentException) {
            stdDecoder.decode(t)
        }
    }

    fun decodeString(input: String): String = String(decode(input), Charsets.UTF_8)
}
