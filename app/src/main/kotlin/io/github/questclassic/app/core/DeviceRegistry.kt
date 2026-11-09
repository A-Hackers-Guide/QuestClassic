package io.github.questclassic.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.deviceStore by preferencesDataStore("device_registry")

/**
 * User-registered devices the AI companion and the virtual-monitor
 * panel know about: paired desktops, smart-home bridges, local LLM
 * servers, etc. Pure metadata — no credentials and no protocol wiring
 * is in this class. Credentials live under each feature's own secure
 * store; the plumbing that talks to a device lives under that
 * feature.
 *
 * - Add / remove / rename / toggle `trusted` flag.
 * - 64-device cap so the list stays navigable and the row never
 *   grows beyond what a single DataStore write handles comfortably.
 */
@Serializable
data class Device(
    val id: String,                 // locally unique; UUID in production
    val displayName: String,        // human label the user sees
    val kind: DeviceKind,
    val address: String,            // IP / mDNS name / Bluetooth MAC
    val trusted: Boolean = false,   // user has explicitly OK'd this device
    val lastSeenEpochMs: Long = 0L,
)

@Serializable
enum class DeviceKind {
    COMPANION_DESKTOP,              // QuestClassic Companion host
    OLLAMA_SERVER,                  // on-LAN LLM
    SMART_HOME_BRIDGE,              // HomeKit / Matter / Zigbee bridge
    GAME_SERVER,                    // self-hosted server for a game
    MEDIA_SERVER,                   // Plex / Jellyfin
    OTHER,
}

class DeviceRegistry(private val context: Context) {

    companion object {
        /** Hard cap on how many devices can be registered at once. */
        const val MAX_DEVICES: Int = 64
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(Device.serializer())
    private val KEY = stringPreferencesKey("devices")

    suspend fun load(): List<Device> {
        val raw = context.deviceStore.data.map { it[KEY] }.first()
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrElse { emptyList() }
    }

    suspend fun save(devices: List<Device>) {
        val trimmed = if (devices.size > MAX_DEVICES) devices.take(MAX_DEVICES) else devices
        val encoded = json.encodeToString(serializer, trimmed)
        context.deviceStore.edit { it[KEY] = encoded }
    }

    suspend fun add(device: Device) {
        val current = load()
        if (current.any { it.id == device.id }) {
            // De-dupe by id — treat an add of an existing id as an update.
            save(current.map { if (it.id == device.id) device else it })
        } else if (current.size >= MAX_DEVICES) {
            throw IllegalStateException(
                "Device registry is full (max=$MAX_DEVICES); remove one before adding another."
            )
        } else {
            save(current + device)
        }
    }

    suspend fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    suspend fun rename(id: String, newName: String) {
        save(load().map { if (it.id == id) it.copy(displayName = newName) else it })
    }

    suspend fun setTrusted(id: String, trusted: Boolean) {
        save(load().map { if (it.id == id) it.copy(trusted = trusted) else it })
    }

    suspend fun touch(id: String, epochMs: Long) {
        save(load().map { if (it.id == id) it.copy(lastSeenEpochMs = epochMs) else it })
    }
}
