package io.github.questclassic.app.ui.panels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.buildToolsStore by preferencesDataStore("build_tools")

/**
 * Persists the Build Tools HUD widget layout so that the user's chosen set
 * survives app restarts. Widgets are stored as a newline-separated list of
 * enum names in insertion order; unknown names (from an older install) are
 * silently dropped on load.
 */
class BuildToolsPrefs(private val context: Context) {

    private val ENABLED = stringPreferencesKey("enabled_widgets")

    /** Load the saved widget list, or return `null` if nothing was saved yet. */
    suspend fun loadEnabled(): List<Widget>? {
        val raw = context.buildToolsStore.data.map { it[ENABLED] }.first()
        if (raw.isNullOrBlank()) return null
        val byName = Widget.entries.associateBy { it.name }
        return raw.lines()
            .mapNotNull { name -> byName[name.trim()] }
    }

    /** Save the current widget order. Call after every toggle / reorder. */
    suspend fun saveEnabled(widgets: List<Widget>) {
        val encoded = widgets.joinToString("\n") { it.name }
        context.buildToolsStore.edit { it[ENABLED] = encoded }
    }
}
