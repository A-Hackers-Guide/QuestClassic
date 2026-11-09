package io.github.questclassic.app.core

import java.security.MessageDigest

/**
 * SHA-256 + short display helpers. Used to verify config backups,
 * diagnostics bundles, and anything else where the user wants to
 * confirm a file hasn't been tampered with between export and import.
 *
 * Why not a dependency: `java.security.MessageDigest` ships with the
 * platform and has a stable API; pulling okio or bouncy-castle for
 * one function would be over-engineering.
 *
 * Pure function; unit-testable.
 */
object ContentIntegrity {

    /** 64-char lowercase hex SHA-256 of the UTF-8 encoding. */
    fun sha256Hex(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Short, user-visible prefix of a hash the user can read aloud over
     *  the phone or type into a text field to confirm equality.
     *
     *  12 hex chars = 48 bits of entropy — enough to spot tampering if
     *  the user is comparing to a hash they got from somewhere else.
     *  NOT a cryptographic commitment; it's a courtesy display helper. */
    fun shortHash(hex: String): String {
        require(hex.length >= 12) { "hex too short: ${hex.length}" }
        return "${hex.substring(0, 4)}-${hex.substring(4, 8)}-${hex.substring(8, 12)}"
    }

    /** Verify a payload against an expected hex. True on match. Pure
     *  input comparison — no timing-safe compare because this is for
     *  user-driven backup verification, not auth. */
    fun verify(text: String, expectedHex: String): Boolean =
        sha256Hex(text).equals(expectedHex.trim(), ignoreCase = true)
}
