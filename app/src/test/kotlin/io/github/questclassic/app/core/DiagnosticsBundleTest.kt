package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsBundleTest {

    private fun fixture() = DiagnosticsBundle(
        createdAt = 1_700_000_000_000L,
        app = AppSlice(
            versionName = "0.3.0",
            versionCode = 42,
            buildType = "release",
            firstRunStep = "COMPLETE",
            aiBackendActive = "claude",
            aiModelActive = "claude-opus-4",
        ),
        device = DeviceSlice(
            manufacturer = "Oculus",
            model = "Quest 2",
            board = "hollywood",
            hardware = "qcom",
            androidRelease = "12",
            sdkInt = 32,
            securityPatch = "2026-10-05",
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
            uptimeSec = 3600,
            languageTag = "en-US",
        ),
        feature = FeatureSlice(
            rooted = false,
            rootSignals = 0,
            deviceRegistryCount = 2,
            voiceRegistryCount = 5,
            passthroughCameraPref = false,
            passthroughAudioPref = true,
            passthroughTextPref = false,
        ),
    )

    @Test
    fun `toJson is a string containing the device model`() {
        val s = DiagnosticsBundle.toJson(fixture())
        assertTrue(s.contains("Quest 2"))
        assertTrue(s.contains("\"versionName\": \"0.3.0\""))
    }

    @Test
    fun `toJson NEVER mentions an api key field`() {
        val s = DiagnosticsBundle.toJson(fixture())
        // The policy is that the whole bundle schema can't carry secrets;
        // this is the single assertion that a schema change didn't
        // accidentally add one back.
        assertFalse("bundle must not expose apiKey", s.contains("apiKey"))
        assertFalse("bundle must not expose perBackendKeys", s.contains("perBackendKeys"))
        assertFalse("bundle must not expose chat history", s.lowercase().contains("chatmessage"))
        assertFalse("bundle must not expose timezone", s.lowercase().contains("timezone"))
    }

    @Test
    fun `version pin`() {
        assertEquals(1, DiagnosticsBundle.CURRENT_VERSION)
    }

    @Test
    fun `json round trip`() {
        val s = DiagnosticsBundle.toJson(fixture())
        val round = kotlinx.serialization.json.Json.decodeFromString(
            DiagnosticsBundle.serializer(), s,
        )
        assertEquals("Quest 2", round.device.model)
        assertEquals(5, round.feature.voiceRegistryCount)
    }
}
