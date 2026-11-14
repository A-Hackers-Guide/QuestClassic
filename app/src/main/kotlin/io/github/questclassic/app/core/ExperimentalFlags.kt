package io.github.questclassic.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.flagsStore by preferencesDataStore("experimental_flags")

/**
 * User-togglable experimental features.
 *
 * Each flag defaults OFF. Users turn them on from the Dev Super-Mode
 * panel (root-gated) or by invoking `/toggle <flag>` in the AI panel
 * (future addition). Nothing here changes the default build's
 * behaviour unless the user explicitly opts in.
 *
 * When a flag graduates (ships on by default), delete its entry and
 * fold the behaviour into the mainline code. Flags are deliberately
 * compile-time enumerated so adding one is a code change, not a
 * configuration change.
 */
enum class ExperimentalFlag(val key: String, val summary: String) {
    AUTO_SUMMARISE_LONG_HISTORY(
        key = "ai.auto_summarise",
        summary = "AI panel triggers HistoryCompactor automatically at 80 messages.",
    ),
    WAKE_WORD_STRICT_BOUNDARY(
        key = "ai.wake.strict_boundary",
        summary = "WakeWordRouter requires a comma or colon after the wake name; rejects plain spaces.",
    ),
    DIAGNOSTICS_BUNDLE_COPY(
        key = "diagnostics.bundle_copy",
        summary = "Add a 'Copy diagnostics bundle' button to the Diagnostics panel.",
    ),
    COMPANION_PAIRING(
        key = "companion.pairing",
        summary = "Enable the desktop-companion pairing flow (requires companion M2+).",
    ),
    EMULATOR_FOREGROUND_LAUNCH(
        key = "emulator.foreground_launch",
        summary = "Switch focus to the launched emulator (otherwise it stays in the background).",
    ),
    ;

    fun prefsKey() = booleanPreferencesKey(key)
}

/**
 * Lightweight on/off store for [ExperimentalFlag].
 */
class ExperimentalFlags(private val context: Context) {

    suspend fun isEnabled(flag: ExperimentalFlag): Boolean {
        val prefs = context.flagsStore.data.first()
        return prefs[flag.prefsKey()] == true
    }

    suspend fun enabledSet(): Set<ExperimentalFlag> {
        val prefs = context.flagsStore.data.first()
        return ExperimentalFlag.entries.filter { prefs[it.prefsKey()] == true }.toSet()
    }

    suspend fun set(flag: ExperimentalFlag, enabled: Boolean) {
        context.flagsStore.edit { it[flag.prefsKey()] = enabled }
    }

    suspend fun toggle(flag: ExperimentalFlag): Boolean {
        val next = !isEnabled(flag)
        set(flag, next)
        return next
    }

    suspend fun clearAll() {
        context.flagsStore.edit { it.clear() }
    }
}
