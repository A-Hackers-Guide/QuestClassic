package io.github.questclassic.app.ui.panels

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.BuzzPattern
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The "Build Tools" HUD. Minimizable overlay that lets the user compose panels out of widgets.
 * Each widget is a small Composable here (or under ui/panels/widgets/ when it grows). Toggling
 * a widget on/off re-renders the HUD stack below.
 */
enum class Widget(val title: String, val render: @Composable (AppContainer) -> Unit) {
    CLOCK("Clock", { ClockWidget() }),
    DEVICE_FP("Fingerprint", { FingerprintWidget() }),
    FREE_MEM("Free memory", { FreeMemoryWidget() }),
    NET_PING("Network ping", { NetworkPingWidget() }),
    QUICK_NOTE("Quick note", { QuickNoteWidget() }),
    CONTROLLER_HAPTIC("Haptic test", { c -> HapticTestWidget(c) }),
    IP_ADDRESS("IP address", { IPAddressWidget() }),
    UPTIME("Uptime", { UptimeWidget() }),
    BATTERY("Battery", { BatteryWidget() }),
    THERMAL_ZONES("Thermal zones", { ThermalZonesWidget() }),
    STORAGE_BREAKDOWN("Storage breakdown", { StorageBreakdownWidget() }),
    SOC_INFO("SoC info", { SoCInfoWidget() }),
    PROCESS_COUNT("Process count", { ProcessCountWidget() }),
    ROOT_STATUS("Root status", { c -> RootStatusWidget(c) }),
    HOST_INFO("Build host", { HostInfoWidget() }),
    SELINUX("SELinux mode", { SELinuxWidget() }),

    // --- Tier-2 widgets ---
    CPU_CORES("CPU cores", { CpuCoresWidget() }),
    SCREEN_METRICS("Screen metrics", { ScreenMetricsWidget() }),
    LOCALE("Locale", { LocaleWidget() }),
    TIME_ZONE("Time zone", { TimeZoneWidget() }),
    KERNEL("Kernel", { KernelWidget() }),
    PERM_SUMMARY("Granted permissions", { c -> PermSummaryWidget(c) }),
    DEVICE_COUNT("Registered devices", { c -> DeviceCountWidget(c) }),
    VOICES_SUMMARY("Voices catalog", { VoicesSummaryWidget() }),
    BATTERY_TEMP("Battery temperature", { BatteryTempWidget() }),
    DISK_FREE("Disk free", { DiskFreeWidget() }),

    // --- Tier-3 widgets ---
    JVM_HEAP("JVM heap", { JvmHeapWidget() }),
    SECURITY_PATCH("Security patch", { SecurityPatchWidget() }),
    ABI_LIST("Supported ABIs", { AbiListWidget() }),
    BOARD("Board", { BoardWidget() }),
    BOOT_TIME("Boot time", { BootTimeWidget() }),
    SENSOR_COUNT("Sensor count", { SensorCountWidget() }),
    DISPLAY_REFRESH("Display refresh", { DisplayRefreshWidget() }),
    GL_VERSION("GL es version", { GlEsVersionWidget() }),
}

@Composable
fun BuildToolsPanel(container: AppContainer, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { BuildToolsPrefs(ctx) }
    val enabled = remember { mutableStateListOf<Widget>() }

    // On first composition, restore the user's saved widget layout; default
    // to the first two widgets if nothing is saved yet.
    LaunchedEffect(Unit) {
        val saved = prefs.loadEnabled()
        enabled.clear()
        enabled.addAll(saved ?: Widget.entries.take(2))
    }

    // Any mutation to `enabled` writes back to DataStore. snapshotFlow turns
    // the SnapshotStateList into a cold Flow we can collect once.
    LaunchedEffect(Unit) {
        snapshotFlow { enabled.toList() }
            .collect { prefs.saveEnabled(it) }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(title = "Build Tools", subtitle = "${enabled.size}/${Widget.entries.size} widgets active · saved", onClose = onClose)
        Spacer(Modifier.height(16.dp))

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WIDGETS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                Spacer(Modifier.height(8.dp))
                Widget.entries.forEach { w ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(w.title, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft, modifier = Modifier.weight(1f))
                        Button(
                            onClick = { if (w in enabled) enabled.remove(w) else enabled.add(w) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (w in enabled) QCColors.Teal else QCColors.Edge,
                                contentColor = if (w in enabled) QCColors.Space else QCColors.Soft,
                            ),
                        ) { Text(if (w in enabled) "On" else "Off") }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("LIVE HUD", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(enabled.toList()) { w ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(w.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                        Spacer(Modifier.height(6.dp))
                        w.render(container)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockWidget() {
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("EEEE HH:mm:ss", Locale.getDefault())
        while (true) { now = fmt.format(Date()); delay(500) }
    }
    Text(now, style = MaterialTheme.typography.displayMedium, color = QCColors.Soft)
}

@Composable
private fun FingerprintWidget() {
    Text(android.os.Build.FINGERPRINT, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun FreeMemoryWidget() {
    val ctx = LocalContext.current
    var line by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val freeMb = mi.availMem / 1_048_576
            val totalMb = mi.totalMem / 1_048_576
            val pct = if (mi.totalMem > 0) (mi.availMem * 100.0 / mi.totalMem) else 0.0
            line = "%d / %d MiB free · %.1f%%".format(freeMb, totalMb, pct)
            delay(1500)
        }
    }
    Text(line, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun NetworkPingWidget() {
    var host by remember { mutableStateOf("1.1.1.1") }
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QCColors.Soft,
                    unfocusedTextColor = QCColors.Soft,
                    focusedBorderColor = QCColors.Teal,
                    unfocusedBorderColor = QCColors.Edge,
                ),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                enabled = !busy,
                onClick = {
                    busy = true; result = null
                    CoroutineScope(Dispatchers.IO).launch {
                        val start = System.nanoTime()
                        val r = runCatching {
                            val addr = InetAddress.getByName(host)
                            val reachable = addr.isReachable(2000)
                            val ms = (System.nanoTime() - start) / 1_000_000
                            if (reachable) "$addr · ${ms}ms" else "$addr · unreachable"
                        }.getOrElse { it.message ?: "resolve failed" }
                        withContext(Dispatchers.Main) {
                            result = r; busy = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text(if (busy) "…" else "Ping") }
        }
        Spacer(Modifier.height(6.dp))
        Text(result ?: "Not run yet.", style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
    }
}

@Composable
private fun QuickNoteWidget() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ephemeral scratch pad…", color = QCColors.Dim) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = QCColors.Soft,
            unfocusedTextColor = QCColors.Soft,
            focusedBorderColor = QCColors.Teal,
            unfocusedBorderColor = QCColors.Edge,
        ),
    )
}

@Composable
private fun HapticTestWidget(container: AppContainer) {
    // All known patterns — new ones added to BuzzPattern.ALL show up here
    // automatically.
    val patterns = BuzzPattern.ALL
    Column {
        Text("Tap a pattern to feel it on the controllers.", style = MaterialTheme.typography.bodyMedium, color = QCColors.Dim)
        Spacer(Modifier.height(6.dp))
        patterns.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text("${p.label} — ${p.codeLetters}", style = MaterialTheme.typography.bodyMedium, color = QCColors.Soft, modifier = Modifier.weight(1f))
                Button(
                    onClick = { container.haptics.playPattern(p) },
                    colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
                ) { Text("Buzz") }
            }
        }
    }
}

@Composable
private fun IPAddressWidget() {
    var ips by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            ips = runCatching {
                java.net.NetworkInterface.getNetworkInterfaces().asSequence()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.asSequence() }
                    .filterNot { it.isLinkLocalAddress || it.isAnyLocalAddress }
                    .joinToString("\n") { "${it.hostAddress}" }
                    .ifBlank { "no addresses" }
            }.getOrElse { it.message ?: "err" }
            delay(5000)
        }
    }
    Text(ips, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun UptimeWidget() {
    var line by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            val up = android.os.SystemClock.elapsedRealtime() / 1000
            val d = up / 86400
            val h = (up % 86400) / 3600
            val m = (up % 3600) / 60
            val s = up % 60
            line = "${d}d ${h}h ${m}m ${s}s"
            delay(1000)
        }
    }
    Text(line, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun BatteryWidget() {
    val ctx = LocalContext.current
    var line by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val current = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
            line = "$level% · ${current}mA"
            delay(2000)
        }
    }
    Text(line, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun ThermalZonesWidget() {
    var zones by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            zones = runCatching {
                java.io.File("/sys/class/thermal/").listFiles { f -> f.name.startsWith("thermal_zone") }
                    ?.sortedBy { it.name }
                    ?.take(8)
                    ?.mapNotNull { zone ->
                        val type = java.io.File(zone, "type").takeIf { it.canRead() }?.readText()?.trim() ?: return@mapNotNull null
                        val temp = java.io.File(zone, "temp").takeIf { it.canRead() }?.readText()?.trim()?.toIntOrNull() ?: 0
                        "${type.take(14)}: %.1f°C".format(temp / 1000.0)
                    }
                    ?.joinToString("\n") ?: "unavailable"
            }.getOrElse { it.message ?: "err" }
            delay(2500)
        }
    }
    Text(zones, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun StorageBreakdownWidget() {
    var out by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            out = runCatching {
                val mounts = listOf(
                    "/" to android.os.Environment.getRootDirectory(),
                    "/data" to android.os.Environment.getDataDirectory(),
                    "/sdcard" to android.os.Environment.getExternalStorageDirectory(),
                )
                mounts.joinToString("\n") { (label, dir) ->
                    val stat = android.os.StatFs(dir.absolutePath)
                    val free = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
                    val total = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
                    "$label  %.2f / %.2f GiB".format(free, total)
                }
            }.getOrElse { it.message ?: "err" }
            delay(5000)
        }
    }
    Text(out, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun SoCInfoWidget() {
    val line = remember {
        buildString {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                append(android.os.Build.SOC_MANUFACTURER.ifBlank { "?" })
                append(' ')
                append(android.os.Build.SOC_MODEL.ifBlank { "?" })
            } else {
                append(android.os.Build.HARDWARE)
            }
            append(" · ")
            append(android.os.Build.SUPPORTED_ABIS.joinToString(","))
            append(" · ")
            append(Runtime.getRuntime().availableProcessors())
            append(" cores")
        }
    }
    Text(line, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun ProcessCountWidget() {
    val ctx = LocalContext.current
    var line by remember { mutableStateOf("—") }
    LaunchedEffect(Unit) {
        while (true) {
            line = runCatching {
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val procs = am.runningAppProcesses?.size ?: 0
                "$procs running app processes"
            }.getOrDefault("—")
            delay(3000)
        }
    }
    Text(line, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun RootStatusWidget(container: AppContainer) {
    val status = remember { container.rootDetector.status() }
    val col = if (status.isRooted) QCColors.OK else QCColors.Amber
    Column {
        Text(if (status.isRooted) "rooted (${status.confidence})" else "stock (${status.confidence})",
            style = MaterialTheme.typography.titleMedium, color = col)
        Spacer(Modifier.height(4.dp))
        Text("su: ${status.suOnPath} · magisk: ${status.magiskPathsPresent} · " +
            "sysWritable: ${status.systemWritable} · testKeys: ${status.testKeysBuild}",
            style = MaterialTheme.typography.labelMedium, color = QCColors.Dim)
    }
}

@Composable
private fun HostInfoWidget() {
    val host = remember { android.os.Build.HOST }
    val user = remember { android.os.Build.USER }
    val time = remember { java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(android.os.Build.TIME)) }
    Text("$host · $user · $time", style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
}

@Composable
private fun SELinuxWidget() {
    val mode = remember {
        runCatching { java.io.File("/sys/fs/selinux/enforce").readText().trim() }
            .getOrElse { "?" }
            .let { if (it == "1") "enforcing" else if (it == "0") "permissive" else "unknown" }
    }
    Text(mode, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

// --- Tier-2 widgets ---

@Composable
private fun CpuCoresWidget() {
    val n = remember { Runtime.getRuntime().availableProcessors() }
    Text("$n cores available",
        style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun ScreenMetricsWidget() {
    val ctx = LocalContext.current
    val dm = remember { ctx.resources.displayMetrics }
    Text(
        "${dm.widthPixels}×${dm.heightPixels} @ ${dm.densityDpi} dpi (xdpi=${"%.1f".format(dm.xdpi)}, ydpi=${"%.1f".format(dm.ydpi)})",
        style = MaterialTheme.typography.labelLarge, color = QCColors.Soft,
    )
}

@Composable
private fun LocaleWidget() {
    val loc = remember { Locale.getDefault() }
    Text(
        "${loc.toLanguageTag()} · ${loc.displayName}",
        style = MaterialTheme.typography.titleMedium, color = QCColors.Soft,
    )
}

@Composable
private fun TimeZoneWidget() {
    val tz = remember { java.util.TimeZone.getDefault() }
    Text(
        "${tz.id} · ${tz.displayName}",
        style = MaterialTheme.typography.titleMedium, color = QCColors.Soft,
    )
}

@Composable
private fun KernelWidget() {
    val kv = remember { System.getProperty("os.version") ?: "?" }
    Text(kv, style = MaterialTheme.typography.labelLarge, color = QCColors.Soft)
}

@Composable
private fun PermSummaryWidget(container: AppContainer) {
    val ctx = LocalContext.current
    val summary = remember {
        val pm = ctx.packageManager
        val info = pm.getPackageInfo(
            ctx.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions?.size ?: 0
        val granted = info.requestedPermissions
            ?.mapIndexed { i, _ ->
                (info.requestedPermissionsFlags?.getOrNull(i) ?: 0) and
                    android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
            }
            ?.count { it != 0 } ?: 0
        "$granted / $requested granted"
    }
    Text(summary, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun DeviceCountWidget(container: AppContainer) {
    var count by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        count = container.devices.load().size
    }
    Text(
        if (count < 0) "…" else "$count registered",
        style = MaterialTheme.typography.titleMedium, color = QCColors.Soft,
    )
}

@Composable
private fun VoicesSummaryWidget() {
    // Static for now — the panel builder calls this before the voice
    // registry is reachable via container (lands with the voices PR's
    // consumer wiring). Stays accurate once wired.
    Text(
        "Voices registry · open the Voices panel to manage",
        style = MaterialTheme.typography.labelLarge, color = QCColors.Dim,
    )
}

@Composable
private fun BatteryTempWidget() {
    val ctx = LocalContext.current
    val filter = remember { android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED) }
    val intent = remember { ctx.registerReceiver(null, filter) }
    val tenthsC = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val text = if (tenthsC < 0) "—"
    else "%.1f °C / %.1f °F".format(tenthsC / 10.0, tenthsC / 10.0 * 9 / 5 + 32)
    Text(text, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun DiskFreeWidget() {
    val stat = remember {
        runCatching {
            val sf = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeGib = sf.availableBytes.toDouble() / (1 shl 30)
            val totalGib = sf.totalBytes.toDouble() / (1 shl 30)
            "%.1f / %.1f GiB free".format(freeGib, totalGib)
        }.getOrDefault("—")
    }
    Text(stat, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

// --- Tier-3 widgets ---

@Composable
private fun JvmHeapWidget() {
    val rt = remember { Runtime.getRuntime() }
    val stat = remember {
        val used = (rt.totalMemory() - rt.freeMemory()).toDouble() / (1 shl 20)
        val max = rt.maxMemory().toDouble() / (1 shl 20)
        "%.0f / %.0f MiB used".format(used, max)
    }
    Text(stat, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun SecurityPatchWidget() {
    val patch = remember { android.os.Build.VERSION.SECURITY_PATCH.ifBlank { "—" } }
    Text("patch · $patch", style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun AbiListWidget() {
    val abis = remember { android.os.Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "—" } }
    Text(abis, style = MaterialTheme.typography.labelLarge, color = QCColors.Soft)
}

@Composable
private fun BoardWidget() {
    val line = remember {
        "${android.os.Build.MANUFACTURER} / ${android.os.Build.BOARD} / ${android.os.Build.HARDWARE}"
    }
    Text(line, style = MaterialTheme.typography.labelLarge, color = QCColors.Soft)
}

@Composable
private fun BootTimeWidget() {
    val when_ = remember {
        val now = System.currentTimeMillis()
        val uptime = android.os.SystemClock.elapsedRealtime()
        val bootMillis = now - uptime
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(bootMillis))
    }
    Text("booted $when_", style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun SensorCountWidget() {
    val ctx = LocalContext.current
    val count = remember {
        runCatching {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            sm?.getSensorList(android.hardware.Sensor.TYPE_ALL)?.size ?: -1
        }.getOrDefault(-1)
    }
    Text(
        if (count < 0) "—" else "$count sensors",
        style = MaterialTheme.typography.titleMedium, color = QCColors.Soft,
    )
}

@Composable
private fun DisplayRefreshWidget() {
    val ctx = LocalContext.current
    val hz = remember {
        runCatching {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.refreshRate ?: Float.NaN
        }.getOrDefault(Float.NaN)
    }
    val txt = if (hz.isNaN()) "—" else "%.1f Hz".format(hz)
    Text(txt, style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

@Composable
private fun GlEsVersionWidget() {
    val ctx = LocalContext.current
    val ver = remember {
        runCatching {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.deviceConfigurationInfo?.glEsVersion ?: "—"
        }.getOrDefault("—")
    }
    Text("OpenGL ES $ver", style = MaterialTheme.typography.titleMedium, color = QCColors.Soft)
}

