package io.github.questclassic.app.core

/**
 * Catalogue of useful one-liner shell + adb snippets that recur across
 * the app's panels. The Dev Super-Mode panel renders these as copyable
 * tiles so users can paste them into an adb shell instead of retyping.
 *
 * Nothing here runs anywhere — this is strictly a data catalogue. All
 * [ScriptTemplate.body] values are inert strings; actually executing
 * them requires a user with adb + a device in developer mode.
 */
data class ScriptTemplate(
    val id: String,
    val title: String,
    val category: ScriptCategory,
    val body: String,
    val requiresRoot: Boolean = false,
)

enum class ScriptCategory {
    DEVICE_INFO, STRIP_META, DIAGNOSTICS, FIRMWARE, PASSTHROUGH, AI, OTHER,
}

object ScriptTemplates {

    val ALL: List<ScriptTemplate> = listOf(
        // --- Device info ---
        ScriptTemplate(
            id = "device.getprop",
            title = "Read every system property",
            category = ScriptCategory.DEVICE_INFO,
            body = "adb shell getprop | sort",
        ),
        ScriptTemplate(
            id = "device.build-fingerprint",
            title = "Build fingerprint",
            category = ScriptCategory.DEVICE_INFO,
            body = "adb shell getprop ro.build.fingerprint",
        ),
        ScriptTemplate(
            id = "device.selinux",
            title = "Current SELinux mode",
            category = ScriptCategory.DEVICE_INFO,
            body = "adb shell getenforce",
        ),

        // --- Strip Meta (see StripMetaPanel) ---
        ScriptTemplate(
            id = "strip.horizon",
            title = "Remove Meta Horizon (social/avatars)",
            category = ScriptCategory.STRIP_META,
            body = "adb shell pm uninstall --user 0 com.oculus.horizon",
        ),
        ScriptTemplate(
            id = "strip.store",
            title = "Remove the Oculus Store client",
            category = ScriptCategory.STRIP_META,
            body = "adb shell pm uninstall --user 0 com.oculus.store",
        ),
        ScriptTemplate(
            id = "strip.insights",
            title = "Remove Meta Insights analytics",
            category = ScriptCategory.STRIP_META,
            body = "adb shell pm uninstall --user 0 com.oculus.insights",
        ),
        ScriptTemplate(
            id = "strip.disable-systemux",
            title = "Disable System UX (keeps intent routing)",
            category = ScriptCategory.STRIP_META,
            body = "adb shell pm disable-user --user 0 com.oculus.systemux",
        ),

        // --- Diagnostics ---
        ScriptTemplate(
            id = "diag.logcat-ai",
            title = "logcat filtered to QuestClassic.AI",
            category = ScriptCategory.DIAGNOSTICS,
            body = "adb logcat -s 'QuestClassic.AI:V'",
        ),
        ScriptTemplate(
            id = "diag.meminfo",
            title = "Dump meminfo for this app",
            category = ScriptCategory.DIAGNOSTICS,
            body = "adb shell dumpsys meminfo io.github.questclassic.app",
        ),
        ScriptTemplate(
            id = "diag.perms-granted",
            title = "List granted runtime permissions",
            category = ScriptCategory.DIAGNOSTICS,
            body = "adb shell cmd appops get io.github.questclassic.app",
        ),

        // --- Firmware (root-only) ---
        ScriptTemplate(
            id = "firmware.boot-partition",
            title = "Hex-dump the boot partition header",
            category = ScriptCategory.FIRMWARE,
            requiresRoot = true,
            body = "adb shell su -c 'dd if=/dev/block/by-name/boot_a bs=4096 count=1 2>/dev/null | xxd | head'",
        ),
        ScriptTemplate(
            id = "firmware.selinux-status",
            title = "SELinux enforcement + policy version",
            category = ScriptCategory.FIRMWARE,
            requiresRoot = true,
            body = "adb shell su -c 'getenforce && cat /sys/fs/selinux/policyvers'",
        ),

        // --- Passthrough ---
        ScriptTemplate(
            id = "passthrough.toggle",
            title = "Toggle the camera passthrough surface (root)",
            category = ScriptCategory.PASSTHROUGH,
            requiresRoot = true,
            body = "adb shell su -c 'service call vrshell 42 i32 1'",
        ),

        // --- AI ---
        ScriptTemplate(
            id = "ai.ollama-reachable",
            title = "Is the user's Ollama endpoint reachable from the headset?",
            category = ScriptCategory.AI,
            body = "adb shell curl -s http://192.168.1.2:11434/api/tags | head",
        ),
    )

    fun byCategory(cat: ScriptCategory): List<ScriptTemplate> =
        ALL.filter { it.category == cat }

    fun findById(id: String): ScriptTemplate? = ALL.firstOrNull { it.id == id }

    /** Count of unique ids = count of templates (sanity assertion for tests). */
    fun size(): Int = ALL.size
}
