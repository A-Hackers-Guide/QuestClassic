package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunStateTest {

    @Test
    fun `forward adjacent transitions allowed`() {
        assertTrue(FirstRunController.canTransition(FirstRunStep.BLANK, FirstRunStep.WELCOMED))
        assertTrue(FirstRunController.canTransition(FirstRunStep.WELCOMED, FirstRunStep.CAPABILITIES_REVIEWED))
        assertTrue(FirstRunController.canTransition(FirstRunStep.CAPABILITIES_REVIEWED, FirstRunStep.AI_CONFIGURED))
        assertTrue(FirstRunController.canTransition(FirstRunStep.AI_CONFIGURED, FirstRunStep.COMPLETE))
    }

    @Test
    fun `backward transitions forbidden`() {
        assertFalse(FirstRunController.canTransition(FirstRunStep.WELCOMED, FirstRunStep.BLANK))
        assertFalse(FirstRunController.canTransition(FirstRunStep.COMPLETE, FirstRunStep.AI_CONFIGURED))
    }

    @Test
    fun `non-adjacent forward transitions forbidden`() {
        // Can't skip CAPABILITIES_REVIEWED or AI_CONFIGURED.
        assertFalse(FirstRunController.canTransition(FirstRunStep.BLANK, FirstRunStep.AI_CONFIGURED))
        assertFalse(FirstRunController.canTransition(FirstRunStep.WELCOMED, FirstRunStep.COMPLETE))
    }

    @Test
    fun `self-loops forbidden`() {
        for (step in FirstRunStep.entries) {
            assertFalse("$step → $step must be forbidden", FirstRunController.canTransition(step, step))
        }
    }

    @Test
    fun `COMPLETE is terminal`() {
        for (step in FirstRunStep.entries) {
            assertFalse(FirstRunController.canTransition(FirstRunStep.COMPLETE, step))
        }
    }

    @Test
    fun `isDone only for COMPLETE`() {
        for (step in FirstRunStep.entries) {
            assertEquals(step == FirstRunStep.COMPLETE, FirstRunController.isDone(step))
        }
    }
}
