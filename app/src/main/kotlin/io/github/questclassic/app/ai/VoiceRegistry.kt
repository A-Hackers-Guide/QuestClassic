package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.voiceStore by preferencesDataStore("voice_registry")

/**
 * User-manageable catalog of TTS voices the AI companion can speak as.
 *
 * - Voices are either **built-in** (platform TTS engine voices picked by
 *   locale + engine + voice-name, no training) or **cloned**
 *   (user-recorded voiceprint, lives under app-private storage at
 *   `samplePath`, synthesized on-device only).
 * - Training is a staged process: QUEUED → RECORDING → TRAINING → READY
 *   (or FAILED). The clone pipeline itself isn't in this class — this
 *   is state + metadata the pipeline keeps in sync.
 * - 32-voice cap. Clones get first-class CRUD; built-ins are catalogued
 *   with [registerBuiltIn] + [toggleEnabled] so the user can hide ones
 *   they don't like.
 */
@Serializable
data class Voice(
    val id: String,
    val displayName: String,
    val kind: VoiceKind,
    val enabled: Boolean = true,
    val locale: String = "en-US",
    /** TTS engine package name (for [VoiceKind.BUILT_IN]). */
    val engine: String? = null,
    /** TTS voice name (for [VoiceKind.BUILT_IN]). */
    val voiceName: String? = null,
    /** Path to the cloned voice's training artifacts (for [VoiceKind.CLONED]). */
    val samplePath: String? = null,
    val trainingState: TrainingState = TrainingState.READY,
)

@Serializable
enum class VoiceKind { BUILT_IN, CLONED }

@Serializable
enum class TrainingState {
    /** Scheduled; nothing recorded yet. */
    QUEUED,

    /** Actively capturing audio samples. */
    RECORDING,

    /** Samples captured; model is fitting. */
    TRAINING,

    /** Available for synthesis. */
    READY,

    /** Training gave up (insufficient audio, bad SNR, user cancel). */
    FAILED,
}

class VoiceRegistry(private val context: Context) {

    companion object { const val MAX_VOICES: Int = 32 }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(Voice.serializer())
    private val KEY = stringPreferencesKey("voices")

    suspend fun load(): List<Voice> {
        val raw = context.voiceStore.data.map { it[KEY] }.first()
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrElse { emptyList() }
    }

    suspend fun save(voices: List<Voice>) {
        val trimmed = if (voices.size > MAX_VOICES) voices.take(MAX_VOICES) else voices
        val encoded = json.encodeToString(serializer, trimmed)
        context.voiceStore.edit { it[KEY] = encoded }
    }

    suspend fun registerBuiltIn(voice: Voice) {
        require(voice.kind == VoiceKind.BUILT_IN) { "not a built-in voice" }
        val current = load()
        if (current.any { it.id == voice.id }) {
            save(current.map { if (it.id == voice.id) voice else it })
        } else {
            save(current + voice)
        }
    }

    suspend fun startClone(id: String, displayName: String): Voice {
        val voice = Voice(
            id = id,
            displayName = displayName,
            kind = VoiceKind.CLONED,
            trainingState = TrainingState.QUEUED,
        )
        val current = load()
        if (current.size >= MAX_VOICES) {
            throw IllegalStateException(
                "Voice registry full (max=$MAX_VOICES); remove one before cloning."
            )
        }
        save(current + voice)
        return voice
    }

    suspend fun advanceTraining(id: String, to: TrainingState) {
        save(load().map { if (it.id == id) it.copy(trainingState = to) else it })
    }

    suspend fun rename(id: String, newName: String) {
        save(load().map { if (it.id == id) it.copy(displayName = newName) else it })
    }

    suspend fun toggleEnabled(id: String, enabled: Boolean) {
        save(load().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    suspend fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    /**
     * Legal transitions for [TrainingState]. Pipeline code calls this
     * before `advanceTraining` to reject nonsense ("already READY → start
     * TRAINING from scratch"-style) early with a clear message.
     */
    fun canTransition(from: TrainingState, to: TrainingState): Boolean = when (from) {
        TrainingState.QUEUED -> to == TrainingState.RECORDING || to == TrainingState.FAILED
        TrainingState.RECORDING -> to == TrainingState.TRAINING || to == TrainingState.FAILED
        TrainingState.TRAINING -> to == TrainingState.READY || to == TrainingState.FAILED
        TrainingState.READY -> to == TrainingState.READY
        TrainingState.FAILED -> to == TrainingState.QUEUED  // retry
    }
}
