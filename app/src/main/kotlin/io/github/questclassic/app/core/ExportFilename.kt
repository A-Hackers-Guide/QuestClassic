package io.github.questclassic.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds safe filenames for user-facing exports (ConfigBackup,
 * DiagnosticsBundle, TranscriptExporter).
 *
 * Each export name is 'questclassic-<slug>-<yyyyMMdd-HHmmss>.<ext>'
 * so files sort chronologically in a Downloads folder and the slug
 * tells the user at a glance which export it is.
 *
 * Pure function; injectable clock.
 */
class ExportFilename(
    private val now: () -> Long = System::currentTimeMillis,
    private val timezone: TimeZone = TimeZone.getTimeZone("UTC"),
) {

    private val fmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
        timeZone = timezone
    }

    fun forExport(slug: String, extension: String): String {
        val safe = PathValidator.sanitizeFilename(slug) ?: "export"
        val ext = extension.trimStart('.').lowercase()
        val stamp = fmt.format(Date(now()))
        return "questclassic-$safe-$stamp.$ext"
    }
}
