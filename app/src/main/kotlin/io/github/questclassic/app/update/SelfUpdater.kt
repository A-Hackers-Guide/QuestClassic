package io.github.questclassic.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.questclassic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
data class GithubAsset(val name: String, val browser_download_url: String, val size: Long)

@Serializable
data class GithubRelease(
    val tag_name: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
    val body: String? = null,
)

/**
 * "Plugs into newer updates" lives here. We don't touch Meta's OTA pipeline (we can't — see the
 * reverse-engineering report on AVB). Instead the APK itself ships a self-updater that polls our
 * GitHub release feed and installs the newer APK through Android's standard PackageInstaller.
 *
 * User-installed APKs survive every Meta A/B OTA, so once installed the app stays through all
 * future Meta system updates, and this updater keeps our side current independently.
 */
class SelfUpdater(private val context: Context, private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val releasesUrl = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    suspend fun checkLatest(): GithubRelease? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(releasesUrl).header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val body = resp.body?.string() ?: return@use null
            runCatching { json.decodeFromString<GithubRelease>(body) }.getOrNull()
        }
    }

    /** Pick the APK asset from a release. */
    fun pickApk(release: GithubRelease): GithubAsset? =
        release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

    fun isNewer(release: GithubRelease): Boolean {
        val latest = release.tag_name.trimStart('v', 'V').trim()
        val current = BuildConfig.VERSION_NAME.trimStart('v', 'V').trim()
        return compareSemver(latest, current) > 0
    }

    suspend fun downloadApk(asset: GithubAsset): File? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(asset.browser_download_url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val target = File(context.cacheDir, "questclassic-update.apk")
            target.outputStream().use { out -> resp.body?.byteStream()?.copyTo(out) }
            target
        }
    }

    /**
     * Launch Android's install UI on the downloaded APK. Requires REQUEST_INSTALL_PACKAGES; the
     * user grants it once in Settings → Apps → Install unknown apps.
     */
    fun install(apk: File) {
        val authority = "${context.packageName}.updates"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun compareSemver(a: String, b: String): Int {
        val ap = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val bp = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(ap.size, bp.size)
        for (i in 0 until n) {
            val av = ap.getOrElse(i) { 0 }
            val bv = bp.getOrElse(i) { 0 }
            if (av != bv) return av - bv
        }
        return 0
    }

    companion object {
        // These get overwritten at CI release time (sed in release.yml) so a forked repo uses
        // its own channel without code edits.
        const val OWNER = "REPLACE_WITH_OWNER"
        const val REPO = "QuestClassic"
    }
}
