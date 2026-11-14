package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ExportFilenameTest {

    private fun atFixed(ms: Long) = ExportFilename(
        now = { ms },
        timezone = TimeZone.getTimeZone("UTC"),
    )

    @Test
    fun `prefix and extension honoured`() {
        val e = atFixed(1_700_000_000_000L)  // 2023-11-14 22:13:20 UTC
        val name = e.forExport("config-backup", "json")
        assertTrue(name.startsWith("questclassic-config-backup-"))
        assertTrue(name.endsWith(".json"))
    }

    @Test
    fun `date embedded`() {
        val e = atFixed(1_700_000_000_000L)
        val name = e.forExport("x", "json")
        assertTrue(name.contains("20231114-221320"))
    }

    @Test
    fun `invalid slug sanitised`() {
        val e = atFixed(1_700_000_000_000L)
        val name = e.forExport("bad/slug?", "md")
        assertTrue(!name.contains("/"))
        assertTrue(!name.contains("?"))
    }

    @Test
    fun `leading dot stripped from extension`() {
        val e = atFixed(1_700_000_000_000L)
        val name = e.forExport("x", ".json")
        assertTrue(name.endsWith(".json"))
        assertTrue(!name.endsWith("..json"))
    }

    @Test
    fun `empty slug falls back`() {
        val e = atFixed(1_700_000_000_000L)
        val name = e.forExport("", "json")
        assertTrue(name.contains("export"))
    }
}
