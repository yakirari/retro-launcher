package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType

data class CustomIntentTemplate(
    val label: String,
    val description: String,
    val emulatedPlatform: String? = null,
    val romSubfolderHint: String? = null,
    val packageName: String = "",
    val activityName: String = "",
    val action: String = "",
    val dataUri: String = "",
    val mimeType: String = "",
    val categoriesCsv: String = "",
    val extrasJson: String = ""
)

object CustomIntentTemplates {
    val all = listOf(
        CustomIntentTemplate(
            label = "Dolphin MMJR2 preset",
            description = "Launches Dolphin MMJR2 MainActivity and passes ROM path as AutoStartFile. Fill Data URI with your game file path/uri.",
            emulatedPlatform = "Nintendo GameCube / Wii",
            romSubfolderHint = "gamecube",
            packageName = "org.dolphinemu.mmjr",
            activityName = "org.dolphinemu.dolphinemu.ui.main.MainActivity",
            action = "android.intent.action.VIEW",
            extrasJson = "{\"AutoStartFile\":\"{filePath}\"}"
        ),
        CustomIntentTemplate(
            label = "Dolphin preset",
            description = "Launches official Dolphin MainActivity and passes ROM path as AutoStartFile. Fill Data URI with your game file path/uri.",
            emulatedPlatform = "Nintendo GameCube / Wii",
            romSubfolderHint = "wii",
            packageName = "org.dolphinemu.dolphinemu",
            activityName = "org.dolphinemu.dolphinemu.ui.main.MainActivity",
            action = "android.intent.action.MAIN",
            extrasJson = "{\"AutoStartFile\":\"{filePath}\"}"
        ),
        CustomIntentTemplate(
            label = "RPCSX preset",
            description = "Targets RPCSX Android app. Pick your PS3 game file, then launch with path/game extras for compatibility.",
            emulatedPlatform = "PlayStation 3",
            romSubfolderHint = "ps3",
            packageName = "net.rpcsx",
            activityName = "net.rpcsx.MainActivity",
            action = "android.intent.action.MAIN",
            extrasJson = "{\"path\":\"{filePath}\",\"game\":\"{filePath}\",\"uri\":\"{dataUri}\"}"
        ),
        CustomIntentTemplate(
            label = "Eden preset",
            description = "Uses Eden's documented Android launch contract: EmulationActivity + TECH_DISCOVERED. Reuses this game's current ROM URI.",
            emulatedPlatform = "Nintendo Switch",
            romSubfolderHint = "switch",
            packageName = "dev.eden.eden_emulator",
            activityName = "org.yuzu.yuzu_emu.activities.EmulationActivity",
            action = "android.nfc.action.TECH_DISCOVERED"
        ),
        CustomIntentTemplate(
            label = "RetroArch preset",
            description = "RetroArch 32-bit. Choose system/core, then pick ROM. Core launch parameters are filled automatically.",
            emulatedPlatform = "Multi-system",
            romSubfolderHint = "retroarch",
            packageName = "com.retroarch",
            activityName = "com.retroarch.browser.retroactivity.RetroActivityFuture",
            action = "android.intent.action.MAIN"
        ),
        CustomIntentTemplate(
            label = "RetroArch64 preset",
            description = "RetroArch 64-bit. Choose system/core, then pick ROM. Core launch parameters are filled automatically.",
            emulatedPlatform = "Multi-system",
            romSubfolderHint = "retroarch",
            packageName = "com.retroarch.aarch64",
            activityName = "com.retroarch.browser.retroactivity.RetroActivityFuture",
            action = "android.intent.action.MAIN"
        ),
        CustomIntentTemplate(
            label = "Citra preset",
            description = "Targets Citra package. Fill Data URI with your ROM path/URI and tweak action/extras only if needed.",
            emulatedPlatform = "Nintendo 3DS",
            romSubfolderHint = "n3ds",
            packageName = "org.citra.emu",
            activityName = "org.citra.emu.ui.EmulationActivity",
            action = "android.intent.action.VIEW",
            extrasJson = "{\"GamePath\":\"{filePath}\"}"
        ),
        CustomIntentTemplate(
            label = "Azahar preset",
            description = "Targets Azahar's Android EmulationActivity using its current ACTION_VIEW launch contract for Nintendo 3DS titles.",
            emulatedPlatform = "Nintendo 3DS",
            romSubfolderHint = "n3ds",
            packageName = "org.azahar_emu.azahar",
            activityName = "org.citra.citra_emu.activities.EmulationActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "PPSSPP preset",
            description = "Targets PPSSPP package. Use Data URI for direct game launch if your setup supports it.",
            emulatedPlatform = "Sony PSP",
            romSubfolderHint = "psp",
            packageName = "org.ppsspp.ppsspp",
            activityName = "org.ppsspp.ppsspp.PpssppActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "NES.emu preset",
            description = "Targets NES.emu package for direct ROM launch.",
            emulatedPlatform = "Nintendo Entertainment System",
            romSubfolderHint = "nes",
            packageName = "com.explusalpha.NesEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "Mupen64Plus AE preset",
            description = "Targets Mupen64Plus AE package for direct ROM launch.",
            emulatedPlatform = "Nintendo 64",
            romSubfolderHint = "n64",
            packageName = "org.mupen64plusae.v3.fzurita.pro",
            activityName = "paulscode.android.mupen64plusae.SplashActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "DraStic preset",
            description = "Targets DraStic package for direct Nintendo DS launch.",
            emulatedPlatform = "Nintendo DS",
            romSubfolderHint = "nds",
            packageName = "com.dsemu.drastic",
            activityName = "com.dsemu.drastic.DraSticActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "Redream preset",
            description = "Targets Redream package for direct Dreamcast launch.",
            emulatedPlatform = "Sega Dreamcast",
            romSubfolderHint = "dreamcast",
            packageName = "io.recompiled.redream",
            activityName = "io.recompiled.redream.MainActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "Flycast preset",
            description = "Targets standalone Flycast package for direct Dreamcast launch.",
            emulatedPlatform = "Sega Dreamcast",
            romSubfolderHint = "dreamcast",
            packageName = "com.flycast.emulator",
            activityName = "com.reicast.emulator.MainActivity",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "ScummVM preset",
            description = "Targets ScummVM package. Pick the game folder (not a single file) for direct launch; otherwise this opens ScummVM UI.",
            emulatedPlatform = "Classic adventure",
            romSubfolderHint = "scummvm",
            packageName = "org.scummvm.scummvm",
            activityName = ".SplashActivity",
            action = "android.intent.action.MAIN"
        ),
        CustomIntentTemplate(
            label = "DuckStation preset",
            description = "Targets DuckStation package for direct ROM launch.",
            emulatedPlatform = "PlayStation 1",
            romSubfolderHint = "PS",
            packageName = "com.github.stenzek.duckstation",
            activityName = "com.github.stenzek.duckstation.EmulationActivity",
            action = "android.intent.action.MAIN",
            extrasJson = "{\"bootPath\":\"{filePath}\"}"
        ),
        CustomIntentTemplate(
            label = "PCE.emu preset",
            description = "Targets PCE.emu package for direct ROM launch.",
            emulatedPlatform = "PC Engine / TurboGrafx-16",
            romSubfolderHint = "pcengine",
            packageName = "com.PceEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "Neo.emu preset",
            description = "Targets Neo.emu package for direct ROM launch.",
            emulatedPlatform = "Neo Geo",
            romSubfolderHint = "neogeo",
            packageName = "com.explusalpha.NeoEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "MD.emu preset",
            description = "Targets MD.emu package for direct ROM launch.",
            emulatedPlatform = "Sega Genesis / Mega Drive",
            romSubfolderHint = "md",
            packageName = "com.explusalpha.MdEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "GBC.emu preset",
            description = "Targets GBC.emu package for direct ROM launch.",
            emulatedPlatform = "Game Boy Color",
            romSubfolderHint = "gbc",
            packageName = "com.explusalpha.GbcEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "NGP.emu preset",
            description = "Targets NGP.emu package for direct ROM launch.",
            emulatedPlatform = "Neo Geo Pocket",
            romSubfolderHint = "ngp",
            packageName = "com.explusalpha.NgpEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "GBA.emu preset",
            description = "Targets GBA.emu package for direct ROM launch.",
            emulatedPlatform = "Game Boy Advance",
            romSubfolderHint = "gba",
            packageName = "com.explusalpha.GbaEmu",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "Snes9x EX+ preset",
            description = "Targets Snes9x EX+ package for direct ROM launch.",
            emulatedPlatform = "Super Nintendo",
            romSubfolderHint = "snes",
            packageName = "com.explusalpha.Snes9xPlus",
            action = "android.intent.action.VIEW"
        ),
        CustomIntentTemplate(
            label = "NetherSX2 preset",
            description = "Targets NetherSX2 / AetherSX2 package for direct ROM launch.",
            emulatedPlatform = "PlayStation 2",
            romSubfolderHint = "ps2",
            packageName = "xyz.aethersx2.android",
            activityName = "xyz.aethersx2.android.EmulationActivity",
            action = "android.intent.action.MAIN",
            extrasJson = "{\"bootPath\":\"{filePath}\"}"
        ),
        CustomIntentTemplate(
            label = "GameNative preset",
            description = "Targets GameNative's documented LAUNCH_GAME contract to boot a Steam/GOG/Epic/Amazon PC game directly, skipping GameNative's own library screen. Replace app_id/game_source in Extras with the game's numeric id and source (STEAM, GOG, EPIC, AMAZON or CUSTOM_GAME).",
            emulatedPlatform = "PC (Windows)",
            romSubfolderHint = "pc",
            packageName = "app.gamenative",
            activityName = "app.gamenative.MainActivity",
            action = "app.gamenative.LAUNCH_GAME",
            extrasJson = "{\"app_id\":0,\"game_source\":\"STEAM\"}"
        ),
    )

    fun toEntry(
        template: CustomIntentTemplate,
        title: String,
        subtitle: String
    ): GameEntry = GameEntry(
        title = title,
        subtitle = subtitle,
        sourceType = GameSourceType.CUSTOM_INTENT,
        packageName = template.packageName.ifBlank { null },
        activityName = template.activityName.ifBlank { null },
        intentAction = template.action.ifBlank { null },
        intentDataUri = template.dataUri.ifBlank { null },
        mimeType = template.mimeType.ifBlank { null },
        categoriesCsv = template.categoriesCsv.ifBlank { null },
        extrasJson = template.extrasJson.ifBlank { null }
    )

    fun matchingTemplate(packageName: String?, activityName: String?): CustomIntentTemplate? {
        val normalizedPackage = packageName?.takeIf { it.isNotBlank() } ?: return null
        val normalizedActivity = activityName?.takeIf { it.isNotBlank() }
        return all.firstOrNull { template ->
            template.packageName == normalizedPackage &&
                (normalizedActivity == null || template.activityName == normalizedActivity)
        } ?: all.firstOrNull { template ->
            template.packageName == normalizedPackage
        }
    }
}

fun CustomIntentTemplate.defaultSubtitle(
    platform: String?,
    retroArchSystem: String? = null
): String {
    if (packageName.isRetroArchTemplatePackage()) {
        val systemLabel = retroArchSystem?.takeIf { it.isNotBlank() }
            ?: GamePlatforms.normalizeOrNull(platform)
            ?: platform?.takeIf { it.isNotBlank() }
        return listOfNotNull("RetroArch", systemLabel).joinToString(" • ")
    }

    val emulatorLabel = label.removeSuffix(" preset")
        .trim()
        .ifBlank { packageName.substringAfterLast('.').ifBlank { label } }
    val platformLabel = GamePlatforms.normalizeOrNull(platform) ?: platform?.takeIf { it.isNotBlank() }
    return listOfNotNull(platformLabel, emulatorLabel).joinToString(" • ").ifBlank { description }
}

private fun String.isRetroArchTemplatePackage(): Boolean =
    this == "com.retroarch" || this == "com.retroarch.aarch64"
