package io.github.questclassic.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val launchIntent: Intent,
    val isVrApp: Boolean,
)

/**
 * Enumerates every launchable app on the device.
 *
 * On Quest OS, VR-launchable apps carry `com.oculus.intent.category.VR`; stock Android apps use
 * `android.intent.category.LAUNCHER`. We include both and tag which is which.
 */
class AppListRepository(private val context: Context) {
    private val pm = context.packageManager

    suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val vrIntent = Intent(Intent.ACTION_MAIN).addCategory("com.oculus.intent.category.VR")
        val flatIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PackageManager.GET_RESOLVED_FILTER
        val vr = runCatching { pm.queryIntentActivities(vrIntent, flags) }.getOrDefault(emptyList())
        val flat = runCatching { pm.queryIntentActivities(flatIntent, flags) }.getOrDefault(emptyList())
        val vrPkgs = vr.map { it.activityInfo.packageName }.toHashSet()
        val seen = HashSet<String>()
        buildList<InstalledApp> {
            fun add(ri: ResolveInfo, markedVr: Boolean) {
                val pkg = ri.activityInfo.packageName
                if (!seen.add(pkg)) return
                val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
                val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(if (markedVr) "com.oculus.intent.category.VR" else Intent.CATEGORY_LAUNCHER)
                    .setPackage(pkg)
                    .setClassName(pkg, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                add(InstalledApp(pkg, label, icon, launch, markedVr || pkg in vrPkgs))
            }
            vr.forEach { add(it, markedVr = true) }
            flat.forEach { add(it, markedVr = false) }
        }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: InstalledApp): Boolean = runCatching {
        context.startActivity(app.launchIntent)
    }.isSuccess
}
