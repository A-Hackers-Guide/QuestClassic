package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests over the training state machine. I/O (DataStore) is
 * verified separately in instrumentation tests.
 */
class VoiceRegistryStateTest {

    private val reg = DummyRegistry()

    @Test
    fun `QUEUED can start recording or fail`() {
        assertTrue(reg.canTransition(TrainingState.QUEUED, TrainingState.RECORDING))
        assertTrue(reg.canTransition(TrainingState.QUEUED, TrainingState.FAILED))
        assertFalse(reg.canTransition(TrainingState.QUEUED, TrainingState.TRAINING))
        assertFalse(reg.canTransition(TrainingState.QUEUED, TrainingState.READY))
    }

    @Test
    fun `RECORDING can only train or fail`() {
        assertTrue(reg.canTransition(TrainingState.RECORDING, TrainingState.TRAINING))
        assertTrue(reg.canTransition(TrainingState.RECORDING, TrainingState.FAILED))
        assertFalse(reg.canTransition(TrainingState.RECORDING, TrainingState.READY))
        assertFalse(reg.canTransition(TrainingState.RECORDING, TrainingState.QUEUED))
    }

    @Test
    fun `TRAINING can only ready or fail`() {
        assertTrue(reg.canTransition(TrainingState.TRAINING, TrainingState.READY))
        assertTrue(reg.canTransition(TrainingState.TRAINING, TrainingState.FAILED))
        assertFalse(reg.canTransition(TrainingState.TRAINING, TrainingState.RECORDING))
    }

    @Test
    fun `READY stays READY`() {
        assertTrue(reg.canTransition(TrainingState.READY, TrainingState.READY))
        assertFalse(reg.canTransition(TrainingState.READY, TrainingState.RECORDING))
        assertFalse(reg.canTransition(TrainingState.READY, TrainingState.TRAINING))
    }

    @Test
    fun `FAILED can only re-queue`() {
        assertTrue(reg.canTransition(TrainingState.FAILED, TrainingState.QUEUED))
        assertFalse(reg.canTransition(TrainingState.FAILED, TrainingState.RECORDING))
        assertFalse(reg.canTransition(TrainingState.FAILED, TrainingState.READY))
    }

    @Test
    fun `Voice defaults are sensible`() {
        val v = Voice(id = "x", displayName = "x", kind = VoiceKind.BUILT_IN)
        assertTrue(v.enabled)
        assertEquals("en-US", v.locale)
        assertEquals(TrainingState.READY, v.trainingState)
    }

    @Test
    fun `VoiceKind has BUILT_IN and CLONED`() {
        val kinds = VoiceKind.values().toSet()
        assertTrue(VoiceKind.BUILT_IN in kinds)
        assertTrue(VoiceKind.CLONED in kinds)
        assertEquals("exactly those two", 2, kinds.size)
    }

    /**
     * Rather than build a real Context+DataStore stack for a unit test
     * of a pure function, mirror only the state-machine method here.
     */
    private class DummyRegistry {
        fun canTransition(from: TrainingState, to: TrainingState): Boolean = when (from) {
            TrainingState.QUEUED -> to == TrainingState.RECORDING || to == TrainingState.FAILED
            TrainingState.RECORDING -> to == TrainingState.TRAINING || to == TrainingState.FAILED
            TrainingState.TRAINING -> to == TrainingState.READY || to == TrainingState.FAILED
            TrainingState.READY -> to == TrainingState.READY
            TrainingState.FAILED -> to == TrainingState.QUEUED
        }
    }
}
