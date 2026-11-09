package io.github.questclassic.app.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.draftStore by preferencesDataStore("ai_drafts")

/**
 * Persists the AI panel's unsent-input draft per backend id so a user
 * who starts a prompt on Claude, switches to OpenAI to ask something
 * quick, then switches back, finds their original draft intact.
 *
 * Why a separate store, not a field on [AIConfig]:
 *
 *  - Drafts are ephemeral, modified on every keystroke; writing to
 *    AIConfig's store would churn its row.
 *  - Drafts carry whatever the user was typing, including personal
 *    content; keeping them isolated makes a "clear drafts" easier.
 *
 * Capped at 16 KB per draft. Longer drafts save truncated (the
 * ClickHouse-length ones are typically paste-accidents anyway).
 */
class DraftStore(private val context: Context) {

    suspend fun load(backendId: String): String =
        context.draftStore.data.first()[key(backendId)].orEmpty()

    suspend fun save(backendId: String, text: String) {
        val capped = if (text.length > MAX_LENGTH) text.take(MAX_LENGTH) else text
        context.draftStore.edit { it[key(backendId)] = capped }
    }

    suspend fun clear(backendId: String) {
        context.draftStore.edit { it.remove(key(backendId)) }
    }

    suspend fun clearAll() {
        context.draftStore.edit { it.clear() }
    }

    private fun key(backendId: String) = stringPreferencesKey("draft/$backendId")

    companion object {
        const val MAX_LENGTH: Int = 16 * 1024
    }
}
