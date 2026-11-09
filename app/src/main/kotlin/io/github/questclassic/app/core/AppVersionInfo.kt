package io.github.questclassic.app.core

/**
 * Normalises the app version into a single user-visible line.
 *
 * Used by About, DiagnosticsBundle, bug-report flows. The goal is a
 * string that, pasted into an issue, uniquely identifies the build
 * without ambiguity: version name, build type, and (when available)
 * the short git SHA from the release tag.
 *
 * Pure function; unit-testable.
 */
data class AppVersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val gitSha: String? = null,
) {
    /** One-line display: "0.3.1 (release) · 42 · g1a2b3c4". */
    fun human(): String = buildString {
        append(versionName)
        append(" (")
        append(buildType)
        append(") · ")
        append(versionCode)
        val sha = gitSha?.take(7)
        if (!sha.isNullOrBlank()) {
            append(" · g")
            append(sha)
        }
    }

    /** Short machine-parseable form, e.g. "0.3.1+42.release.g1a2b3c4". */
    fun compact(): String = buildString {
        append(versionName)
        append("+")
        append(versionCode)
        append(".")
        append(buildType)
        gitSha?.take(7)?.takeIf { it.isNotBlank() }?.let {
            append(".g")
            append(it)
        }
    }

    /** True for release or RC builds — i.e. 'production' in UX terms. */
    fun isRelease(): Boolean = buildType.equals("release", ignoreCase = true)

    companion object {
        /** Guard against an obviously bad version info. */
        fun valid(info: AppVersionInfo): Boolean =
            info.versionName.isNotBlank() &&
                info.versionCode >= 0 &&
                info.buildType.isNotBlank()
    }
}
