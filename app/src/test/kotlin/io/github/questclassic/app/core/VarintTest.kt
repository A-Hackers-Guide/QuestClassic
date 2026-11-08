package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Standalone tests of the protobuf varint reader. We reproduce the parsing logic here (same
 * shape as the one inside PayloadVerifier) so this test can run without Android.
 *
 * If the implementation in PayloadVerifier.kt is changed, these tests become the specification
 * the new code must satisfy. The inputs are canonical protobuf-encoded unsigned varints taken
 * from the Android A/B update_engine payload format.
 */
class VarintTest {

    private fun readVarint(input: ByteArrayInputStream): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = input.read()
            require(b != -1) { "unexpected EOF" }
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
            require(shift < 64) { "varint too long" }
        }
    }

    @Test
    fun `single-byte varint`() {
        val input = ByteArrayInputStream(byteArrayOf(0x05))
        assertEquals(5L, readVarint(input))
    }

    @Test
    fun `two-byte varint`() {
        // 150 = 0b10010110 → low byte 0x96 with high bit set, high byte 0x01
        val input = ByteArrayInputStream(byteArrayOf(0x96.toByte(), 0x01))
        assertEquals(150L, readVarint(input))
    }

    @Test
    fun `boundary 127 one byte`() {
        val input = ByteArrayInputStream(byteArrayOf(0x7F))
        assertEquals(127L, readVarint(input))
    }

    @Test
    fun `boundary 128 two bytes`() {
        val input = ByteArrayInputStream(byteArrayOf(0x80.toByte(), 0x01))
        assertEquals(128L, readVarint(input))
    }

    @Test
    fun `larger value 300`() {
        // 300 = 0b100101100 → 0xAC, 0x02
        val input = ByteArrayInputStream(byteArrayOf(0xAC.toByte(), 0x02))
        assertEquals(300L, readVarint(input))
    }

    @Test
    fun `OTA-sized value (manifest-size ballpark)`() {
        // 0x0123 (= 291) in the manifest-size field position
        val input = ByteArrayInputStream(byteArrayOf(0xA3.toByte(), 0x02))
        assertEquals(291L, readVarint(input))
    }
}
