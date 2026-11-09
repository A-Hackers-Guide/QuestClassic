package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiPanelStore by preferencesDataStore("ai_panel")

/**
 * User-facing panel toggles that aren't part of the provider config:
 * voice I/O enable, text display enable. Persisted separately from
 * [AIConfig] so flipping them doesn't rewrite the API-key row.
 *
 * - `voiceEnabled` — speak replies through TTS (phase-M wake word work
 *   will also toggle the microphone path).
 * - `textEnabled`  — show the transcript. Turning this off makes the
 *   panel audio-only, useful when passthrough is enough.
 */
data class AIPanelPrefsSnapshot(
    val voiceEnabled: Boolean = false,
    val textEnabled: Boolean = true,
)

class AIPanelPrefs(private val context: Context) {

    private val VOICE = booleanPreferencesKey("voice_enabled")
    private val TEXT = booleanPreferencesKey("text_enabled")

    suspend fun load(): AIPanelPrefsSnapshot {
        val prefs = context.aiPanelStore.data.map { it }.first()
        return AIPanelPrefsSnapshot(
            voiceEnabled = prefs[VOICE] ?: false,
            textEnabled = prefs[TEXT] ?: true,
        )
    }

    suspend fun save(snapshot: AIPanelPrefsSnapshot) {
        context.aiPanelStore.edit {
            it[VOICE] = snapshot.voiceEnabled
            it[TEXT] = snapshot.textEnabled
        }
    }
}
