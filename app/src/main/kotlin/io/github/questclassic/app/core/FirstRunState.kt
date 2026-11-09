package io.github.questclassic.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.firstRunStore by preferencesDataStore("first_run")

/**
 * Where the user is in the first-run / setup flow. Linear by design;
 * each step can only advance forward (or reset to the start). The AI
 * setup step is split out of general onboarding because the user may
 * genuinely want to skip it (Ollama-only, or deferred configuration).
 */
enum class FirstRunStep {
    /** The user has never opened the app. No bootstrap shown yet. */
    BLANK,

    /** Welcome screen acknowledged. Build Tools HUD may be visible. */
    WELCOMED,

    /** Root / passthrough / device capability briefing acknowledged. */
    CAPABILITIES_REVIEWED,

    /** At least one AI backend has an API key, OR AI was explicitly skipped. */
    AI_CONFIGURED,

    /** All required steps complete; wizard never shows again (unless reset). */
    COMPLETE,
}

/**
 * Snapshot of first-run state. `aiSkipped` records whether the user
 * reached AI_CONFIGURED by configuring AI or by explicitly skipping — the
 * UI can nudge them later if they skipped.
 */
data class FirstRunSnapshot(
    val step: FirstRunStep,
    val aiSkipped: Boolean,
)

/**
 * Persists and progresses the first-run flow. All transitions are forward
 * or reset — there's no "jump to step 3" because the wizard renders steps
 * as a strict pipeline (previous step's output drives the next).
 */
class FirstRunController(private val context: Context) {

    private val STEP = stringPreferencesKey("step")
    private val AI_SKIPPED = booleanPreferencesKey("ai_skipped")

    suspend fun load(): FirstRunSnapshot {
        val prefs = context.firstRunStore.data.map { it }.first()
        val raw = prefs[STEP].orEmpty().ifBlank { FirstRunStep.BLANK.name }
        val step = runCatching { FirstRunStep.valueOf(raw) }.getOrDefault(FirstRunStep.BLANK)
        return FirstRunSnapshot(step = step, aiSkipped = prefs[AI_SKIPPED] == true)
    }

    private suspend fun save(snapshot: FirstRunSnapshot) {
        context.firstRunStore.edit {
            it[STEP] = snapshot.step.name
            it[AI_SKIPPED] = snapshot.aiSkipped
        }
    }

    /**
     * Try to advance from [from] to [next]. Only forward-adjacent moves
     * succeed; a non-adjacent jump returns false and leaves state
     * unchanged, so the UI can detect "someone tried to skip" cases.
     */
    suspend fun advance(from: FirstRunStep, next: FirstRunStep, aiSkipped: Boolean = false): Boolean {
        val now = load()
        if (now.step != from) return false
        if (!canTransition(from, next)) return false
        save(
            FirstRunSnapshot(
                step = next,
                aiSkipped = now.aiSkipped || aiSkipped,
            )
        )
        return true
    }

    /** Reset to BLANK — exposed via the About panel's "Replay setup". */
    suspend fun reset() = save(FirstRunSnapshot(FirstRunStep.BLANK, aiSkipped = false))

    companion object {
        /**
         * The allowed moves. Visible to tests so the state machine's
         * contract is testable without a Context.
         */
        fun canTransition(from: FirstRunStep, to: FirstRunStep): Boolean = when (from) {
            FirstRunStep.BLANK -> to == FirstRunStep.WELCOMED
            FirstRunStep.WELCOMED -> to == FirstRunStep.CAPABILITIES_REVIEWED
            FirstRunStep.CAPABILITIES_REVIEWED -> to == FirstRunStep.AI_CONFIGURED
            FirstRunStep.AI_CONFIGURED -> to == FirstRunStep.COMPLETE
            FirstRunStep.COMPLETE -> false
        }

        /** True once the user is past the wizard; the main shell renders. */
        fun isDone(step: FirstRunStep): Boolean = step == FirstRunStep.COMPLETE
    }
}
