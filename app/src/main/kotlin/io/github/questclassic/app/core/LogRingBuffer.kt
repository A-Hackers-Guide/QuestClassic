package io.github.questclassic.app.core

/**
 * Fixed-capacity in-memory log ring, for attaching to diagnostics.
 *
 * Android's logcat is per-app on Quest-class devices and opening it
 * requires `adb`, which a user filing a bug report doesn't have.
 * This ring holds the last N log lines in process so the
 * DiagnosticsBundle can snapshot them into the shared JSON without
 * shelling out.
 *
 * Every line passes through [SecretRedactor] on write so a careless
 * Log.d call that stringifies an exception carrying the
 * Authorization header never persists the key. The schema-level
 * exclusions in [DiagnosticsBundle] remain the primary defence; this
 * is the belt-and-suspenders path for free-form log output.
 *
 * Thread-safe via @Synchronized; snapshot returns an immutable copy.
 */
class LogRingBuffer(
    val capacity: Int = 256,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init { require(capacity in 16..4096) { "capacity out of range: $capacity" } }

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class Entry(
        val timestampMs: Long,
        val level: Level,
        val tag: String,
        val message: String,
    )

    private val buf = ArrayDeque<Entry>(capacity)

    @Synchronized
    fun write(level: Level, tag: String, message: String) {
        val scrubbed = SecretRedactor.redact(message)
        if (buf.size >= capacity) buf.removeFirst()
        buf.addLast(Entry(now(), level, tag, scrubbed))
    }

    fun debug(tag: String, message: String) = write(Level.DEBUG, tag, message)
    fun info(tag: String, message: String) = write(Level.INFO, tag, message)
    fun warn(tag: String, message: String) = write(Level.WARN, tag, message)
    fun error(tag: String, message: String) = write(Level.ERROR, tag, message)

    @Synchronized
    fun snapshot(): List<Entry> = buf.toList()

    @Synchronized
    fun size(): Int = buf.size

    @Synchronized
    fun clear() = buf.clear()

    /** Format as plain text lines for pasting into an issue. */
    @Synchronized
    fun format(): String = buildString {
        for (e in buf) {
            append(e.timestampMs).append(' ')
            append(e.level.name).append('/')
            append(e.tag).append(": ")
            appendLine(e.message)
        }
    }

    /** Filter snapshot to one or more levels; useful for 'errors only'. */
    fun snapshotAt(atLeast: Level): List<Entry> =
        snapshot().filter { it.level.ordinal >= atLeast.ordinal }
}
