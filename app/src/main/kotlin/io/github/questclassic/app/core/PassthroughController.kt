package io.github.questclassic.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.passthroughStore by preferencesDataStore("passthrough")

/**
 * User-facing passthrough controls. Three independent toggles:
 *
 *  - `cameraPassthrough` — show the real world through the cameras
 *    (what people usually mean when they say "passthrough").
 *  - `audioPassthrough`  — route headset microphone audio to the
 *    panel's AI companion / virtual-monitor voice channel.
 *  - `textPassthrough`   — display AI replies + system notifications
 *    as floating text; turning this off makes those surfaces audio-only.
 *
 * The *control* lives here as a settings store. The *plumbing* that
 * actually flips the Quest's passthrough surface requires the
 * `com.oculus.permission.USE_SCENE` signature permission we don't
 * hold on retail builds. See `docs/user-guide/passthrough.md` for the
 * runtime story and the detect-rooted fall-back.
 */
data class PassthroughState(
    val cameraPassthrough: Boolean = false,
    val audioPassthrough: Boolean = false,
    val textPassthrough: Boolean = true,
)

class PassthroughController(private val context: Context) {

    private val CAMERA = booleanPreferencesKey("camera")
    private val AUDIO = booleanPreferencesKey("audio")
    private val TEXT = booleanPreferencesKey("text")

    suspend fun load(): PassthroughState {
        val prefs = context.passthroughStore.data.map { it }.first()
        return PassthroughState(
            cameraPassthrough = prefs[CAMERA] ?: false,
            audioPassthrough = prefs[AUDIO] ?: false,
            textPassthrough = prefs[TEXT] ?: true,
        )
    }

    suspend fun save(state: PassthroughState) {
        context.passthroughStore.edit {
            it[CAMERA] = state.cameraPassthrough
            it[AUDIO] = state.audioPassthrough
            it[TEXT] = state.textPassthrough
        }
    }

    /**
     * Returns `true` if we can actually flip camera passthrough on this
     * device. Non-root retail Quests cannot — the compositor owns the
     * passthrough surface and only the signed Meta shell can request it.
     *
     * Downstream code should treat the setting as a *preference* even when
     * this returns `false`; the actual compositor flip only happens when
     * [effectiveCameraOn] agrees.
     */
    fun cameraPassthroughSupported(rootDetector: RootDetector): Boolean =
        rootDetector.isRooted()

    /**
     * The *effective* state — what the compositor will actually do when
     * the panel asks for passthrough. Combines the user's preference with
     * device capability.
     */
    fun effectiveCameraOn(
        state: PassthroughState,
        rootDetector: RootDetector,
    ): Boolean = state.cameraPassthrough && cameraPassthroughSupported(rootDetector)
}
