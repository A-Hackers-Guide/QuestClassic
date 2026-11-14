package io.github.questclassic.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.userPrefsStore by preferencesDataStore("user_prefs")

/**
 * Catch-all for small user preferences that don't deserve a dedicated
 * DataStore file. Compile-time keys so a typo at the call site is a
 * compile error, not a silent default.
 *
 * Why not reuse one of the existing stores: those are typed to a
 * specific concern (AI config, drafts, voices, etc.). Spilling random
 * small prefs into them would entangle the shape of those files with
 * UX details the stores shouldn't care about. One central user_prefs
 * store keeps the small knobs together without churning the typed
 * stores.
 */
class UserPrefs(private val context: Context) {

    // ----- Enumeration of every key this store manages. -----

    enum class BoolPref(val key: String, val default: Boolean) {
        HAPTICS_ENABLED("haptics.enabled", default = true),
        DIM_NON_AI_PANELS_IN_DARK_ROOMS("ui.auto_dim", default = false),
        REDUCE_MOTION("ui.reduce_motion", default = false),
        SHOW_SUSPICION_CHIP("ai.show_suspicion_chip", default = true),
    }

    enum class IntPref(val key: String, val default: Int) {
        BUZZ_INTENSITY_PERCENT("haptics.intensity", default = 70),
        AI_TEMPERATURE_INT("ai.temperature_int", default = 50),  // 0..100 = 0.0..1.0
    }

    enum class FloatPref(val key: String, val default: Float) {
        UI_SCALE("ui.scale", default = 1.0f),
    }

    enum class StringPref(val key: String, val default: String) {
        PREFERRED_TONE("ai.preferred_tone", default = "CASUAL"),
        PREFERRED_LOCALE("ui.locale", default = ""), // "" = system
    }

    // ----- Readers / writers. -----

    suspend fun bool(pref: BoolPref): Boolean =
        context.userPrefsStore.data.first()[booleanPreferencesKey(pref.key)] ?: pref.default

    suspend fun int(pref: IntPref): Int =
        context.userPrefsStore.data.first()[intPreferencesKey(pref.key)] ?: pref.default

    suspend fun float(pref: FloatPref): Float =
        context.userPrefsStore.data.first()[floatPreferencesKey(pref.key)] ?: pref.default

    suspend fun string(pref: StringPref): String =
        context.userPrefsStore.data.first()[stringPreferencesKey(pref.key)] ?: pref.default

    suspend fun set(pref: BoolPref, value: Boolean) {
        context.userPrefsStore.edit { it[booleanPreferencesKey(pref.key)] = value }
    }

    suspend fun set(pref: IntPref, value: Int) {
        val clamped = when (pref) {
            IntPref.BUZZ_INTENSITY_PERCENT -> value.coerceIn(0, 100)
            IntPref.AI_TEMPERATURE_INT -> value.coerceIn(0, 100)
        }
        context.userPrefsStore.edit { it[intPreferencesKey(pref.key)] = clamped }
    }

    suspend fun set(pref: FloatPref, value: Float) {
        val clamped = when (pref) {
            FloatPref.UI_SCALE -> value.coerceIn(0.5f, 2.0f)
        }
        context.userPrefsStore.edit { it[floatPreferencesKey(pref.key)] = clamped }
    }

    suspend fun set(pref: StringPref, value: String) {
        context.userPrefsStore.edit { it[stringPreferencesKey(pref.key)] = value }
    }

    suspend fun resetAll() {
        context.userPrefsStore.edit { it.clear() }
    }

    companion object {
        /** Catalogue of every pref key, for the Settings panel's 'list all' view. */
        fun allKeys(): List<String> =
            BoolPref.entries.map { it.key } +
                IntPref.entries.map { it.key } +
                FloatPref.entries.map { it.key } +
                StringPref.entries.map { it.key }
    }
}
