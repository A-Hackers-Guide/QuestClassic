package io.github.questclassic.app.core

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Library-level root detection. Nothing here *performs* privilege escalation; it only reads
 * public filesystem state + build tags to decide whether the device already has root installed
 * by some external method (Magisk, a vendor-built userdebug ROM, a Meta-signed debug build).
 *
 * This detector is defensive: QuestClassic's privileged panels are gated on it. On a stock
 * retail Quest 2, every result below is false and the gated panels show a clear
 * "this device isn't rooted — here's what you'd need" explanation instead of acting.
 *
 * We deliberately do NOT:
 *   - try to exploit anything
 *   - call `su` and see what happens (noisy, triggers device policy, not useful)
 *   - probe Meta's TrustZone-backed attestation (that's for Meta, not us)
 *
 * The check runs on first query and the result is memoized. If the device state changes
 * mid-session, call [refresh] — we don't do that automatically because root transitions during
 * a single session are extremely rare.
 */
class RootDetector(private val app: Context) {

    /** The best cheap evidence we can gather without shelling out. */
    data class RootStatus(
        val suOnPath: Boolean,
        val magiskPathsPresent: Boolean,
        val systemWritable: Boolean,
        val testKeysBuild: Boolean,
        val debuggableBuild: Boolean,
    ) {
        /** Rough confidence: any one hit is suggestive; several together are definitive. */
        val isRooted: Boolean
            get() = suOnPath || magiskPathsPresent || systemWritable
        val confidence: Confidence
            get() = when {
                suOnPath && magiskPathsPresent -> Confidence.HIGH
                suOnPath || magiskPathsPresent || systemWritable -> Confidence.MEDIUM
                testKeysBuild || debuggableBuild -> Confidence.LOW
                else -> Confidence.NONE
            }

        enum class Confidence { NONE, LOW, MEDIUM, HIGH }
    }

    @Volatile private var cached: RootStatus? = null

    fun status(): RootStatus = cached ?: compute().also { cached = it }

    fun refresh(): RootStatus = compute().also { cached = it }

    private fun compute(): RootStatus = RootStatus(
        suOnPath = suOnPath(),
        magiskPathsPresent = magiskPathsPresent(),
        systemWritable = systemWritable(),
        testKeysBuild = Build.TAGS?.contains("test-keys") == true,
        debuggableBuild = Build.TYPE == "userdebug" || Build.TYPE == "eng",
    )

    private fun suOnPath(): Boolean {
        val candidates = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/su/bin/su",
            "/vendor/bin/su",
        )
        return candidates.any { File(it).exists() }
    }

    private fun magiskPathsPresent(): Boolean {
        val magiskCandidates = listOf(
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/modules",
            "/sbin/.magisk",
            "/cache/.disable_magisk",
        )
        return magiskCandidates.any { File(it).exists() }
    }

    private fun systemWritable(): Boolean {
        // `/system` is read-only on a sealed stock build. A rooted build will have remounted
        // some subdirectory of it. We probe a benign writeable canary.
        val probe = File("/system/.qc_root_probe")
        return try {
            probe.exists() || probe.createNewFile().also { ok -> if (ok) probe.delete() }
        } catch (_: SecurityException) {
            false
        } catch (_: java.io.IOException) {
            false
        }
    }
}
