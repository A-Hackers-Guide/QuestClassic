package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuzzSequenceTest {

    @Test
    fun `every registered sequence has a non-empty pattern list`() {
        for (s in BuzzSequence.ALL) {
            assertTrue(s.id, s.patterns.isNotEmpty())
        }
    }

    @Test
    fun `ids are unique`() {
        val ids = BuzzSequence.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `labels are non-blank`() {
        for (s in BuzzSequence.ALL) {
            assertTrue(s.id, s.label.isNotBlank())
        }
    }

    @Test
    fun `approxDuration is positive`() {
        for (s in BuzzSequence.ALL) {
            assertTrue(s.id, s.approxDurationMs() > 0)
        }
    }

    @Test
    fun `empty pattern list throws`() {
        var threw = false
        try {
            BuzzSequence("empty", "nope", emptyList())
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `invalid gap throws`() {
        var threw = false
        try {
            BuzzSequence("x", "nope", listOf(BuzzPattern.OK), gapMs = 10)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `duration includes gaps`() {
        val s = BuzzSequence(
            id = "x",
            label = "x",
            patterns = listOf(BuzzPattern.OK, BuzzPattern.OK),
            gapMs = 200,
        )
        // Two OK patterns' timings sum + 200ms gap
        val patternSum = BuzzPattern.OK.toTimings().sum() * 2
        assertEquals(patternSum + 200, s.approxDurationMs())
    }

    @Test
    fun `single-pattern sequence has zero gaps`() {
        val s = BuzzSequence("x", "x", listOf(BuzzPattern.OK), gapMs = 200)
        assertEquals(BuzzPattern.OK.toTimings().sum(), s.approxDurationMs())
    }

    @Test
    fun `5 built-in sequences`() {
        assertEquals(5, BuzzSequence.ALL.size)
    }
}
