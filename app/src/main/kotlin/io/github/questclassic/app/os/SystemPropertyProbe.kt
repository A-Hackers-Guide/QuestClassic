package io.github.questclassic.app.os

import android.os.Build
import java.lang.reflect.Method

/**
 * Reflection-based `getprop` reader. Everything here uses only publicly-reachable system
 * properties — the same values Settings → About and `adb shell getprop` return.
 */
class SystemPropertyProbe {
    private val method: Method? = runCatching {
        @Suppress("PrivateApi")
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
    }.getOrNull()

    fun get(key: String): String? = runCatching {
        (method?.invoke(null, key) as? String)?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    val summary: String get() = buildString {
        append(get("ro.product.system.model") ?: Build.MODEL)
        append(" · ")
        append(get("ro.build.branch") ?: "Android ${Build.VERSION.RELEASE}")
    }
}
