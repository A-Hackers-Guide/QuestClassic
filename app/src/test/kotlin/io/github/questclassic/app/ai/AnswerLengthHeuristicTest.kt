package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerLengthHeuristicTest {

    @Test
    fun `short question with mark is terse`() {
        assertEquals(AnswerLengthHeuristic.Bucket.TERSE, AnswerLengthHeuristic.bucket("what time is it?"))
    }

    @Test
    fun `yes-no opener is terse`() {
        assertEquals(AnswerLengthHeuristic.Bucket.TERSE, AnswerLengthHeuristic.bucket("Is Zurich warm in April"))
    }

    @Test
    fun `explain is detailed`() {
        assertEquals(AnswerLengthHeuristic.Bucket.DETAILED, AnswerLengthHeuristic.bucket("explain monads"))
    }

    @Test
    fun `code fence is detailed`() {
        val q = "look at this:\n```\nkotlin code here\n```\nwhat's wrong"
        assertEquals(AnswerLengthHeuristic.Bucket.DETAILED, AnswerLengthHeuristic.bucket(q))
    }

    @Test
    fun `long question is detailed`() {
        assertEquals(AnswerLengthHeuristic.Bucket.DETAILED,
            AnswerLengthHeuristic.bucket("a".repeat(1000)))
    }

    @Test
    fun `standard default`() {
        assertEquals(AnswerLengthHeuristic.Bucket.STANDARD,
            AnswerLengthHeuristic.bucket("tell me about Zurich architecture in a couple sentences"))
    }

    @Test
    fun `bucket token counts increase`() {
        val terse = AnswerLengthHeuristic.Bucket.TERSE.maxTokens
        val std = AnswerLengthHeuristic.Bucket.STANDARD.maxTokens
        val det = AnswerLengthHeuristic.Bucket.DETAILED.maxTokens
        assert(terse < std && std < det)
    }
}
