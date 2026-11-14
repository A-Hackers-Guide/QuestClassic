package io.github.questclassic.app.ai

/**
 * Orchestrator that walks a [Voice] (kind = CLONED) through the
 * [TrainingState] ladder:
 *
 *   QUEUED → RECORDING → TRAINING → READY
 *       │       │           │
 *       └── FAILED ◄────────┘
 *                │
 *                └── QUEUED (retry)
 *
 * The transition rules live in [VoiceRegistry.canTransition] and are
 * enforced here so a bad request fails loudly (no silent state drift).
 *
 * The recording / training implementations are injected as suspend
 * callbacks so the pipeline is pure logic + testable without
 * AudioRecord / a TTS model. Real wire-in lives in a future
 * `VoiceCloneService` (not in this commit).
 */
class VoiceClonePipeline(
    private val registry: VoiceRegistry,
    private val record: suspend (voiceId: String) -> RecordingResult,
    private val train: suspend (voiceId: String) -> TrainingResult,
) {

    sealed interface Result {
        val voiceId: String
        data class Success(override val voiceId: String) : Result
        data class Failure(override val voiceId: String, val reason: String) : Result
    }

    sealed interface RecordingResult {
        data object Ok : RecordingResult
        data class Error(val reason: String) : RecordingResult
    }

    sealed interface TrainingResult {
        data object Ok : TrainingResult
        data class Error(val reason: String) : TrainingResult
    }

    /**
     * Run the full pipeline for one voice. Idempotent on READY: calling
     * on an already-READY voice is a no-op that reports Success without
     * re-recording. On any step failure, advances to FAILED with the
     * reason so the user can retry.
     */
    suspend fun run(voiceId: String): Result {
        val before = registry.load().firstOrNull { it.id == voiceId }
            ?: return Result.Failure(voiceId, "voice not registered")
        if (before.trainingState == TrainingState.READY) {
            return Result.Success(voiceId) // idempotent
        }

        if (!registry.canTransition(before.trainingState, TrainingState.RECORDING)) {
            return fail(voiceId, "cannot transition ${before.trainingState} → RECORDING")
        }
        registry.advanceTraining(voiceId, TrainingState.RECORDING)

        when (val r = record(voiceId)) {
            RecordingResult.Ok -> Unit
            is RecordingResult.Error -> return fail(voiceId, "recording: ${r.reason}")
        }

        if (!registry.canTransition(TrainingState.RECORDING, TrainingState.TRAINING)) {
            return fail(voiceId, "cannot transition RECORDING → TRAINING")
        }
        registry.advanceTraining(voiceId, TrainingState.TRAINING)

        when (val t = train(voiceId)) {
            TrainingResult.Ok -> Unit
            is TrainingResult.Error -> return fail(voiceId, "training: ${t.reason}")
        }

        if (!registry.canTransition(TrainingState.TRAINING, TrainingState.READY)) {
            return fail(voiceId, "cannot transition TRAINING → READY")
        }
        registry.advanceTraining(voiceId, TrainingState.READY)
        return Result.Success(voiceId)
    }

    private suspend fun fail(voiceId: String, reason: String): Result.Failure {
        runCatching { registry.advanceTraining(voiceId, TrainingState.FAILED) }
        return Result.Failure(voiceId, reason)
    }
}
