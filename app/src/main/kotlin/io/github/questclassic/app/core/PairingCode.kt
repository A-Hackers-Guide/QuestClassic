package io.github.questclassic.app.core

import java.security.SecureRandom

/**
 * Generates a short, human-readable pairing code for the companion
 * flow. Both sides (headset + companion) see the same code and the
 * user confirms they match, defeating a trivial MITM on the LAN.
 *
 * Format: 3 groups of 3 characters from a confusion-resistant alphabet:
 *   "ABC-DEF-123"
 *
 * Excludes 0/O, 1/I/L to avoid handwriting confusion. 21^9 possible
 * codes ≈ 5e11, enough for a one-shot pairing nonce.
 *
 * Also includes a cheap verifier: [matches] compares two codes with
 * whitespace + case + separator normalisation, so the user who sees
 * 'abcdef123' on the companion still matches 'ABC-DEF-123' on the
 * headset.
 */
object PairingCode {

    private const val ALPHABET: String = "ABCDEFGHJKMNPQRSTUVWXY23456789"
    private val random = SecureRandom()

    /** Generate a fresh pairing code. */
    fun generate(): String {
        val sb = StringBuilder(11)
        for (i in 0 until 9) {
            if (i == 3 || i == 6) sb.append('-')
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
        return sb.toString()
    }

    /**
     * Compare two user-entered codes forgiving of case, separators,
     * whitespace, and the easy-to-confuse chars we excluded.
     */
    fun matches(a: String, b: String): Boolean = normalise(a) == normalise(b) && normalise(a).length == 9

    private fun normalise(raw: String): String = raw
        .uppercase()
        .filter { it in ALPHABET }

    /** Example: validate a user-entered pairing code's shape. */
    fun isValidShape(code: String): Boolean {
        val n = normalise(code)
        return n.length == 9
    }
}
