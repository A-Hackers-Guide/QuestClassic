package io.github.questclassic.app.core

import android.content.Context
import android.os.Build
import android.os.SystemClock
import io.github.questclassic.app.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Snapshot of everything useful for a support-ticket attachment.
 *
 * **Policy**: this bundle, like [ConfigBackup], NEVER includes API keys,
 * chat history, or cloned-voice samples. It also redacts the user's IP
 * addresses and locale down to language tag (no city / timezone offsets
 * wouldn't ID anyone, but we strip timezone to language tag to err on
 * the side of less).
 *
 * Users attach this to a GitHub issue or paste it in Discord; the
 * on-device UI should preview it before any export.
 */
@Serializable
data class DiagnosticsBundle(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long,
    val app: AppSlice,
    val device: DeviceSlice,
    val feature: FeatureSlice,
) {
    companion object {
        const val CURRENT_VERSION: Int = 1

        private val json = Json { prettyPrint = true; encodeDefaults = true }

        /**
         * Build a snapshot. The counts (device / voice registry etc.) are
         * read via the container; raw rows are NOT included — only sizes.
         * Users who need to share names should use [ConfigBackup] instead,
         * which carries the whole registry.
         */
        suspend fun capture(context: Context, container: AppContainer): DiagnosticsBundle {
            val aiConfig = container.ai.load()
            val devices = container.devices.load()
            val voices = container.voices.load()
            val passthrough = container.passthrough.load()
            val rootStatus = container.rootDetector.status()
            // FirstRunController is constructed inline rather than read off
            // container.firstRun so this module doesn't require that wire-in
            // to be in place first. Idempotent; it just reads DataStore.
            val firstRun = FirstRunController(context).load()

            return DiagnosticsBundle(
                createdAt = System.currentTimeMillis(),
                app = AppSlice(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    buildType = BuildConfig.BUILD_TYPE,
                    firstRunStep = firstRun.step.name,
                    aiBackendActive = aiConfig.backendId,
                    aiModelActive = aiConfig.model,
                    // NOT included: aiConfig.apiKey, perBackendKeys, perBackendNames.
                ),
                device = DeviceSlice(
                    manufacturer = Build.MANUFACTURER,
                    model = Build.MODEL,
                    board = Build.BOARD,
                    hardware = Build.HARDWARE,
                    androidRelease = Build.VERSION.RELEASE,
                    sdkInt = Build.VERSION.SDK_INT,
                    securityPatch = Build.VERSION.SECURITY_PATCH,
                    supportedAbis = Build.SUPPORTED_ABIS.toList(),
                    uptimeSec = SystemClock.elapsedRealtime() / 1000,
                    languageTag = Locale.getDefault().toLanguageTag(),
                    // NOT included: timezone id, IP addresses, serial number, IMEI.
                ),
                feature = FeatureSlice(
                    rooted = rootStatus.isRooted,
                    rootSignals = listOf(
                        rootStatus.suOnPath,
                        rootStatus.magiskPathsPresent,
                        rootStatus.systemWritable,
                        rootStatus.testKeysBuild,
                        rootStatus.debuggableBuild,
                    ).count { it },
                    deviceRegistryCount = devices.size,
                    voiceRegistryCount = voices.size,
                    passthroughCameraPref = passthrough.cameraPassthrough,
                    passthroughAudioPref = passthrough.audioPassthrough,
                    passthroughTextPref = passthrough.textPassthrough,
                ),
            )
        }

        /** Serialize to JSON for paste / file-write. Policy exclusions still apply. */
        fun toJson(bundle: DiagnosticsBundle): String =
            json.encodeToString(serializer(), bundle)
    }
}

@Serializable
data class AppSlice(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val firstRunStep: String,
    val aiBackendActive: String,
    val aiModelActive: String,
)

@Serializable
data class DeviceSlice(
    val manufacturer: String,
    val model: String,
    val board: String,
    val hardware: String,
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val supportedAbis: List<String>,
    val uptimeSec: Long,
    val languageTag: String,
)

@Serializable
data class FeatureSlice(
    val rooted: Boolean,
    val rootSignals: Int,
    val deviceRegistryCount: Int,
    val voiceRegistryCount: Int,
    val passthroughCameraPref: Boolean,
    val passthroughAudioPref: Boolean,
    val passthroughTextPref: Boolean,
)
