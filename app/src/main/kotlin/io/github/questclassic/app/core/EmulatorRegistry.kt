package io.github.questclassic.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Catalog of known emulators we can *launch* on a sideloaded Quest.
 *
 * Why not bundle them? Mixing Dolphin + PCSX2 + DuckStation + Snes9x
 * source into one binary isn't legally possible — see [LicenceNote]
 * below. We route per system to either RetroArch (libretro cores,
 * each in its own loadable shared library) or a user-installed
 * standalone emulator. Nothing is bundled; the user has to install
 * these themselves.
 */
data class EmulatorTarget(
    val id: String,
    val system: String,
    val displayName: String,
    val packageName: String,
    val preferredCore: String? = null,
    val homepage: String,
)

object EmulatorRegistry {

    /**
     * Known emulator targets. Order matters — we prefer the first entry
     * for a given system when both RetroArch and a standalone are
     * installed.
     */
    val ALL: List<EmulatorTarget> = listOf(
        // --- RetroArch (one package, many cores) ---
        EmulatorTarget(
            id = "retroarch.gamecube",
            system = "Nintendo GameCube",
            displayName = "RetroArch (Dolphin core)",
            packageName = "com.retroarch",
            preferredCore = "dolphin_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.wii",
            system = "Nintendo Wii",
            displayName = "RetroArch (Dolphin core)",
            packageName = "com.retroarch",
            preferredCore = "dolphin_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.ps2",
            system = "Sony PlayStation 2",
            displayName = "RetroArch (PCSX2 core)",
            packageName = "com.retroarch",
            preferredCore = "pcsx2_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.psp",
            system = "Sony PSP",
            displayName = "RetroArch (PPSSPP core)",
            packageName = "com.retroarch",
            preferredCore = "ppsspp_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.ps1",
            system = "Sony PlayStation 1",
            displayName = "RetroArch (DuckStation core)",
            packageName = "com.retroarch",
            preferredCore = "duckstation_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.n64",
            system = "Nintendo 64",
            displayName = "RetroArch (Mupen64Plus-Next core)",
            packageName = "com.retroarch",
            preferredCore = "mupen64plus_next_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.snes",
            system = "Super Nintendo",
            displayName = "RetroArch (Snes9x core)",
            packageName = "com.retroarch",
            preferredCore = "snes9x_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.nes",
            system = "Nintendo NES",
            displayName = "RetroArch (Nestopia core)",
            packageName = "com.retroarch",
            preferredCore = "nestopia_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.gba",
            system = "Game Boy Advance",
            displayName = "RetroArch (mGBA core)",
            packageName = "com.retroarch",
            preferredCore = "mgba_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.gb",
            system = "Game Boy / Game Boy Color",
            displayName = "RetroArch (SameBoy core)",
            packageName = "com.retroarch",
            preferredCore = "sameboy_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.genesis",
            system = "Sega Genesis / Mega Drive",
            displayName = "RetroArch (Genesis Plus GX core)",
            packageName = "com.retroarch",
            preferredCore = "genesis_plus_gx_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.saturn",
            system = "Sega Saturn",
            displayName = "RetroArch (Beetle Saturn core)",
            packageName = "com.retroarch",
            preferredCore = "mednafen_saturn_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.dreamcast",
            system = "Sega Dreamcast",
            displayName = "RetroArch (Flycast core)",
            packageName = "com.retroarch",
            preferredCore = "flycast_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.3do",
            system = "3DO",
            displayName = "RetroArch (Opera core)",
            packageName = "com.retroarch",
            preferredCore = "opera_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),

        // --- Standalone emulators (installed independently) ---
        EmulatorTarget(
            id = "standalone.duckstation",
            system = "Sony PlayStation 1 (standalone)",
            displayName = "DuckStation (standalone)",
            packageName = "com.github.stenzek.duckstation",
            homepage = "https://www.duckstation.org/",
        ),
        EmulatorTarget(
            id = "standalone.aethersx2",
            system = "Sony PlayStation 2 (standalone)",
            displayName = "AetherSX2 (standalone)",
            packageName = "xyz.aethersx2.android",
            homepage = "https://www.aethersx2.com/",
        ),
        EmulatorTarget(
            id = "standalone.ppsspp",
            system = "Sony PSP (standalone)",
            displayName = "PPSSPP (standalone)",
            packageName = "org.ppsspp.ppsspp",
            homepage = "https://www.ppsspp.org/",
        ),
        EmulatorTarget(
            id = "standalone.dolphin",
            system = "Nintendo GameCube / Wii (standalone)",
            displayName = "Dolphin (standalone)",
            packageName = "org.dolphinemu.dolphinemu",
            homepage = "https://dolphin-emu.org/",
        ),
        EmulatorTarget(
            id = "standalone.vita3k",
            system = "Sony PS Vita (standalone)",
            displayName = "Vita3K (standalone)",
            packageName = "org.vita3k.emulator",
            homepage = "https://vita3k.org/",
        ),
        EmulatorTarget(
            id = "standalone.citra",
            system = "Nintendo 3DS (standalone)",
            displayName = "Citra MMJ (standalone)",
            packageName = "org.citra.citra_emu",
            homepage = "https://citra-emu.org/",
        ),
        EmulatorTarget(
            id = "standalone.yuzu",
            system = "Nintendo Switch (standalone)",
            displayName = "Yuzu / Suyu (standalone)",
            packageName = "org.suyu.suyu_emu",
            homepage = "https://suyu.dev/",
        ),
        EmulatorTarget(
            id = "standalone.lemuroid",
            system = "Multi-system (standalone, FOSS)",
            displayName = "Lemuroid (standalone)",
            packageName = "com.swordfish.lemuroid",
            homepage = "https://github.com/libretro/Lemuroid",
        ),

        // --- Additional RetroArch targets (one package, many cores) ---
        EmulatorTarget(
            id = "retroarch.atari2600",
            system = "Atari 2600",
            displayName = "RetroArch (Stella core)",
            packageName = "com.retroarch",
            preferredCore = "stella_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.arcade",
            system = "Arcade (MAME 2003 Plus)",
            displayName = "RetroArch (MAME 2003 Plus core)",
            packageName = "com.retroarch",
            preferredCore = "mame2003_plus_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.sega_saturn",
            system = "Sega Saturn",
            displayName = "RetroArch (Beetle Saturn core)",
            packageName = "com.retroarch",
            preferredCore = "mednafen_saturn_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.sega_dreamcast",
            system = "Sega Dreamcast",
            displayName = "RetroArch (Flycast core)",
            packageName = "com.retroarch",
            preferredCore = "flycast_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.gb_color",
            system = "Game Boy / Color",
            displayName = "RetroArch (Gambatte core)",
            packageName = "com.retroarch",
            preferredCore = "gambatte_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.gba",
            system = "Game Boy Advance",
            displayName = "RetroArch (mGBA core)",
            packageName = "com.retroarch",
            preferredCore = "mgba_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.ds",
            system = "Nintendo DS",
            displayName = "RetroArch (melonDS core)",
            packageName = "com.retroarch",
            preferredCore = "melonds_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),
        EmulatorTarget(
            id = "retroarch.apple2",
            system = "Apple II",
            displayName = "RetroArch (Mame Apple II core)",
            packageName = "com.retroarch",
            preferredCore = "lr_mame_apple2_libretro_android.so",
            homepage = "https://www.retroarch.com/",
        ),

        // --- Additional standalone targets ---
        EmulatorTarget(
            id = "standalone.aethersx2",
            system = "Sony PlayStation 2 (standalone, read-only; mirror of last public build)",
            displayName = "AetherSX2 (standalone; discontinued)",
            packageName = "xyz.aethersx2.android",
            homepage = "https://aethersx2.github.io/",
        ),
        EmulatorTarget(
            id = "standalone.redream",
            system = "Sega Dreamcast (standalone)",
            displayName = "Redream (standalone)",
            packageName = "io.recompiled.redream",
            homepage = "https://redream.io/",
        ),
    )

    /** Returns `true` iff the emulator's package is installed on this device. */
    fun isInstalled(ctx: Context, target: EmulatorTarget): Boolean =
        runCatching {
            ctx.packageManager.getPackageInfo(target.packageName, 0)
            true
        }.getOrDefault(false)

    /** Launch intent that drops the user into the emulator's main activity. */
    fun launchIntent(ctx: Context, target: EmulatorTarget): Intent? =
        ctx.packageManager.getLaunchIntentForPackage(target.packageName)

    /** Market-page intent for installing a missing emulator. */
    fun storeIntent(target: EmulatorTarget): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${target.packageName}"),
    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    /**
     * Licence posture: this registry only *detects* and *launches* each
     * emulator's own installed package. No upstream source is bundled or
     * redistributed. See README.md § "Legal / IP posture" for why bundling
     * them into a single binary would be a licence violation (GPL-2.0-only
     * vs GPL-3.0 + Snes9x's non-commercial clause + DuckStation's current
     * PolyForm Strict).
     */
    @Suppress("unused")
    const val LicenceNote: String =
        "Detect + launch only. No emulator source is bundled."
}
