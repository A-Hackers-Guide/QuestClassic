package io.github.questclassic.app.os

import android.content.Context
import android.os.Build
import java.lang.reflect.Method

/**
 * Read-only device fingerprinting from userspace. Everything here uses `getprop` / android.os.Build
 * — the same values a user can read from Settings → About.
 *
 * Interesting ones on Quest 2 (confirmed from the reverse-engineering done for this project):
 *   ro.product.system.model = Quest 2
 *   ro.build.branch = releases-oculus-14.0-v209   (HorizonOS version, not AOSP)
 *   ro.build.host = <leaked internal Meta build-host>
 *   ro.oculus.reality_mode = mr
 */
class DeviceIntrospection(@Suppress("UNUSED_PARAMETER") context: Context) {
    data class Prop(val key: String, val value: String?)

    /** Use reflection to reach android.os.SystemProperties.get(String) safely. */
    private val getpropMethod: Method? = runCatching {
        @Suppress("PrivateApi")
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
    }.getOrNull()

    private fun prop(key: String): String? = runCatching {
        (getpropMethod?.invoke(null, key) as? String)?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun summary(): List<Prop> = listOf(
        Prop("Model", Build.MODEL),
        Prop("Manufacturer", Build.MANUFACTURER),
        Prop("Codename (ro.product.system.device)", prop("ro.product.system.device") ?: Build.DEVICE),
        Prop("Android", Build.VERSION.RELEASE),
        Prop("SDK", Build.VERSION.SDK_INT.toString()),
        Prop("Fingerprint", Build.FINGERPRINT),
        Prop("Build ID", Build.ID),
        Prop("Build host", prop("ro.build.host")),
        Prop("HorizonOS branch", prop("ro.build.branch")),
        Prop("Security patch", Build.VERSION.SECURITY_PATCH),
        Prop("Reality mode (ro.oculus.reality_mode)", prop("ro.oculus.reality_mode")),
        Prop("SoC", prop("ro.soc.model")),
        Prop("Vendor patch (ro.vendor.build.security_patch)", prop("ro.vendor.build.security_patch")),
        Prop("USB VID (ro.recovery.usb.vid)", prop("ro.recovery.usb.vid")),
    )

    val isQuest: Boolean get() = Build.MANUFACTURER.equals("Oculus", ignoreCase = true) ||
        prop("ro.product.system.manufacturer")?.equals("Oculus", ignoreCase = true) == true
}
