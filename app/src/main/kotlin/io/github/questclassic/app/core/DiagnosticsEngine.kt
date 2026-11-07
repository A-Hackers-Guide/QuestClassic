package io.github.questclassic.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Self-diagnostics the APK can actually run from userspace. We intentionally stay away from
 * claims about hardware we can't reach (eye-tracking LEDs, structured-light, modem) — those live
 * in vendor HALs locked to signed daemons on this device. Everything here uses standard Android
 * APIs that work on any Quest without special permissions.
 */
data class DiagnosticsResult(
    val name: String,
    val ok: Boolean,
    val value: String,
    val failPattern: BuzzPattern? = null,
)

class DiagnosticsEngine(
    private val context: Context,
    private val haptics: ControllerHaptics,
) {
    suspend fun runAll(): List<DiagnosticsResult> = withContext(Dispatchers.IO) {
        listOf(
            battery(),
            storage(),
            ram(),
            network(),
            device(),
            performance(),
        )
    }

    fun buzzFailures(results: List<DiagnosticsResult>) {
        val firstFail = results.firstOrNull { !it.ok && it.failPattern != null } ?: run {
            haptics.playPattern(BuzzPattern.OK)
            return
        }
        haptics.playPattern(firstFail.failPattern!!)
    }

    private fun battery(): DiagnosticsResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(null, filter)
        val health = sticky?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "overvoltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            else -> "unknown"
        }
        val ok = level >= 10 && health == BatteryManager.BATTERY_HEALTH_GOOD
        return DiagnosticsResult(
            name = "Battery",
            ok = ok,
            value = "$level% · $healthStr",
            failPattern = if (level < 10) BuzzPattern.BATTERY_LOW else null,
        )
    }

    private fun storage(): DiagnosticsResult {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        val freePct = if (totalBytes > 0) (freeBytes * 100.0 / totalBytes) else 0.0
        val ok = freePct > 5.0
        return DiagnosticsResult(
            name = "Storage",
            ok = ok,
            value = "%.1f GiB free · %.1f%%".format(freeBytes / 1_073_741_824.0, freePct),
            failPattern = BuzzPattern.STORAGE_FULL.takeIf { !ok },
        )
    }

    private fun ram(): DiagnosticsResult {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val freePct = (mi.availMem * 100.0 / mi.totalMem)
        return DiagnosticsResult(
            name = "RAM",
            ok = !mi.lowMemory,
            value = "%.0f MiB free · %.1f%%".format(mi.availMem / 1_048_576.0, freePct),
        )
    }

    private fun network(): DiagnosticsResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val linkSpeed = runCatching {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifi.connectionInfo.linkSpeed.takeIf { it > 0 }?.let { "${it}Mbps" }
        }.getOrNull() ?: "—"
        return DiagnosticsResult(
            name = "Network",
            ok = online,
            value = if (online) "online · $linkSpeed" else "offline",
            failPattern = BuzzPattern.WIFI_DOWN.takeUnless { online },
        )
    }

    private fun device(): DiagnosticsResult {
        return DiagnosticsResult(
            name = "Device",
            ok = true,
            value = "${Build.MODEL} · ${Build.PRODUCT} · SDK ${Build.VERSION.SDK_INT}",
        )
    }

    private fun performance(): DiagnosticsResult {
        // Thermal is a rough proxy: TEMP from /sys is not always readable; we fall back to
        // ActivityManager's low-memory flag + CPU count as a sanity read.
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val procs = Runtime.getRuntime().availableProcessors()
        return DiagnosticsResult(
            name = "Performance",
            ok = !mi.lowMemory,
            value = "$procs cores · ${if (mi.lowMemory) "low-mem" else "ok"}",
            failPattern = BuzzPattern.THERMAL.takeIf { mi.lowMemory },
        )
    }
}
