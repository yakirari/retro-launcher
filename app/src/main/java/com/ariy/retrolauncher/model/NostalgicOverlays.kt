package com.ariy.retrolauncher.model

import androidx.annotation.DrawableRes
import com.ariy.retrolauncher.R

data class NostalgicOverlaySpec(
    @DrawableRes val drawableResourceId: Int,
    val aspectRatio: Float,
    val artworkLeftFraction: Float,
    val artworkTopFraction: Float,
    val artworkWidthFraction: Float,
    val artworkHeightFraction: Float,
    val artworkCornerRadiusFraction: Float,
    val detectArtworkPlacementFromTransparency: Boolean = true,
    val clipArtworkToTransparencyMask: Boolean = false,
    val fitArtworkToWindow: Boolean = false,
    val applyVintageArtwork: Boolean = false,
    val preTrimLeftFraction: Float = 0f,
    val preTrimTopFraction: Float = 0f,
    val preTrimWidthFraction: Float = 1f,
    val preTrimHeightFraction: Float = 1f,
    val cropLeftFraction: Float = 0f,
    val cropTopFraction: Float = 0f,
    val cropWidthFraction: Float = 1f,
    val cropHeightFraction: Float = 1f,
    val trimPlayStationSpine: Boolean = false,
    // Scale the complete cartridge and its artwork together, keeping label placement intact.
    val gridArtworkScale: Float = 1f
) {
    val croppedAspectRatio: Float
        get() = aspectRatio * (cropWidthFraction / cropHeightFraction)
}

object NostalgicOverlays {
    private val digitalGameSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_digital_pc,
        aspectRatio = 600f / 900f,
        artworkLeftFraction = 54f / 600f, artworkTopFraction = 81f / 900f,
        artworkWidthFraction = 492f / 600f, artworkHeightFraction = 738f / 900f,
        artworkCornerRadiusFraction = 0f,
        detectArtworkPlacementFromTransparency = false
    )
    // These artwork fractions are fallbacks. The renderer derives live placement
    // from the overlay PNG's transparent window when a mask is available.
    private val defaultSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_default,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 340f / 960f,
        artworkTopFraction = 241f / 768f,
        artworkWidthFraction = 292f / 960f,
        artworkHeightFraction = 287f / 768f,
        artworkCornerRadiusFraction = 0.01f
    )
    private fun floppyDiskSpec(@DrawableRes drawableResourceId: Int) = NostalgicOverlaySpec(
        drawableResourceId = drawableResourceId,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 450f / 960f,
        artworkTopFraction = 101f / 768f,
        artworkWidthFraction = 274f / 960f,
        artworkHeightFraction = 158f / 768f,
        artworkCornerRadiusFraction = 0.018f,
        applyVintageArtwork = true,
        cropLeftFraction = 284f / 1408f,
        cropTopFraction = 64f / 768f,
        cropWidthFraction = 843f / 1408f,
        cropHeightFraction = 696f / 768f
    )
    private val scummVmSpec = floppyDiskSpec(R.drawable.nostalgic_overlay_scummvm)
    private val dosGamesSpec = floppyDiskSpec(R.drawable.nostalgic_overlay_dos)
    private val compactDiscSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_cd,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 215f / 960f,
        artworkTopFraction = 93f / 768f,
        artworkWidthFraction = 607f / 960f,
        artworkHeightFraction = 587f / 768f,
        artworkCornerRadiusFraction = 0f,
        cropLeftFraction = 355f / 1408f,
        cropTopFraction = 77f / 768f,
        cropWidthFraction = 703f / 1408f,
        cropHeightFraction = 619f / 768f
    )
    private val ps1Spec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_ps1,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 215f / 960f,
        artworkTopFraction = 93f / 768f,
        artworkWidthFraction = 607f / 960f,
        artworkHeightFraction = 587f / 768f,
        artworkCornerRadiusFraction = 0f,
        detectArtworkPlacementFromTransparency = false,
        fitArtworkToWindow = true,
        cropLeftFraction = 126f / 960f,
        cropTopFraction = 72f / 768f,
        cropWidthFraction = 714f / 960f,
        cropHeightFraction = 630f / 768f
    )
    private val snesCartridgeSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_snes,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 198f / 960f,
        artworkTopFraction = 107f / 768f,
        artworkWidthFraction = 397f / 960f,
        artworkHeightFraction = 465f / 768f,
        artworkCornerRadiusFraction = 0f,
        applyVintageArtwork = true
    )
    private val nesSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_nes,
        aspectRatio = 960f / 768f,
        // Recalculated from the current PNG transparency mask.
        artworkLeftFraction = 416f / 960f,
        artworkTopFraction = 22f / 768f,
        artworkWidthFraction = 307f / 960f,
        artworkHeightFraction = 493f / 768f,
        artworkCornerRadiusFraction = 12f / 960f,
        applyVintageArtwork = true
    )
    private val gameBoySpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_gb,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 331f / 960f,
        artworkTopFraction = 366f / 768f,
        artworkWidthFraction = 301f / 960f,
        artworkHeightFraction = 264f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        fitArtworkToWindow = true,
        applyVintageArtwork = true
    )
    private val gameBoyColorSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_gbc,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 331f / 960f,
        artworkTopFraction = 366f / 768f,
        artworkWidthFraction = 301f / 960f,
        artworkHeightFraction = 264f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        applyVintageArtwork = true
    )
    private val megaDriveSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_md,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 238f / 960f,
        artworkTopFraction = 126f / 768f,
        artworkWidthFraction = 481f / 960f,
        artworkHeightFraction = 513f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        applyVintageArtwork = true
    )
    private val gameBoyAdvanceSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_gba,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 277f / 960f,
        artworkTopFraction = 418f / 768f,
        artworkWidthFraction = 408f / 960f,
        artworkHeightFraction = 185f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        applyVintageArtwork = true
    )
    private val pspSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_psp,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 291f / 960f,
        artworkTopFraction = 110f / 768f,
        artworkWidthFraction = 344f / 960f,
        artworkHeightFraction = 579f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        cropLeftFraction = 494f / 1408f,
        cropTopFraction = 88f / 768f,
        cropWidthFraction = 380f / 1408f,
        cropHeightFraction = 622f / 768f
    )
    private val wiiSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_wii,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 262f / 960f,
        artworkTopFraction = 91f / 768f,
        artworkWidthFraction = 436f / 960f,
        artworkHeightFraction = 601f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        cropLeftFraction = 484f / 1408f,
        cropTopFraction = 73f / 768f,
        cropWidthFraction = 439f / 1408f,
        cropHeightFraction = 622f / 768f
    )
    private val gameCubeSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_gc,
        aspectRatio = 960f / 768f,
        // Recalculated from the current PNG transparency mask.
        artworkLeftFraction = 287f / 960f,
        artworkTopFraction = 163f / 768f,
        artworkWidthFraction = 364f / 960f,
        artworkHeightFraction = 486f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        preTrimTopFraction = 0.05f,
        preTrimHeightFraction = 0.95f
    )
    private val nintendo3dsSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_3ds,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 172f / 960f,
        artworkTopFraction = 107f / 768f,
        artworkWidthFraction = 551f / 960f,
        artworkHeightFraction = 554f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        cropLeftFraction = 397f / 1408f,
        cropTopFraction = 77f / 768f,
        cropWidthFraction = 614f / 1408f,
        cropHeightFraction = 614f / 768f
    )
    private val ps3Spec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_ps3,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 238f / 960f,
        artworkTopFraction = 178f / 768f,
        artworkWidthFraction = 468f / 960f,
        artworkHeightFraction = 497f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        cropLeftFraction = 458f / 1408f,
        cropTopFraction = 77f / 768f,
        cropWidthFraction = 491f / 1408f,
        cropHeightFraction = 614f / 768f
    )
    private val ps2Spec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_ps2,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 203f / 960f,
        artworkTopFraction = 91f / 768f,
        artworkWidthFraction = 527f / 960f,
        artworkHeightFraction = 639f / 768f,
        artworkCornerRadiusFraction = 0f,
        clipArtworkToTransparencyMask = true,
        cropLeftFraction = 203f / 960f,
        cropTopFraction = 91f / 768f,
        cropWidthFraction = 527f / 960f,
        cropHeightFraction = 639f / 768f
    )
    private val switchSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_switch,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 294f / 960f,
        artworkTopFraction = 94f / 768f,
        artworkWidthFraction = 357f / 960f,
        artworkHeightFraction = 581f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        cropLeftFraction = 514f / 1408f,
        cropTopFraction = 77f / 768f,
        cropWidthFraction = 379f / 1408f,
        cropHeightFraction = 614f / 768f
    )
    private val ndsSpec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_nds,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 308f / 960f,
        artworkTopFraction = 202f / 768f,
        artworkWidthFraction = 395f / 960f,
        artworkHeightFraction = 414f / 768f,
        artworkCornerRadiusFraction = 0f,
        preTrimLeftFraction = 0.08f,
        preTrimWidthFraction = 0.92f
    )
    private val n64Spec = NostalgicOverlaySpec(
        drawableResourceId = R.drawable.nostalgic_overlay_n64,
        aspectRatio = 960f / 768f,
        artworkLeftFraction = 242f / 960f,
        artworkTopFraction = 141f / 768f,
        artworkWidthFraction = 478f / 960f,
        artworkHeightFraction = 554f / 768f,
        artworkCornerRadiusFraction = 0.01f,
        applyVintageArtwork = true,
        cropLeftFraction = 459f / 1408f,
        cropTopFraction = 77f / 769f,
        cropWidthFraction = 482f / 1408f,
        cropHeightFraction = 615f / 769f,
        gridArtworkScale = 0.88f
    )

    private val specByPlatform = mapOf(
        "ScummVM" to scummVmSpec,
        "DOS Games" to dosGamesSpec,
        "Sony PlayStation" to ps1Spec,
        "Sony PlayStation 2" to ps2Spec,
        "Sony PlayStation 3" to ps3Spec,
        "Sega Dreamcast" to compactDiscSpec,
        "Nintendo Entertainment System" to nesSpec,
        "Super Nintendo" to snesCartridgeSpec,
        "Game Boy" to gameBoySpec,
        "Game Boy Color" to gameBoyColorSpec,
        "Sega Genesis / Mega Drive" to megaDriveSpec,
        "Game Boy Advance" to gameBoyAdvanceSpec,
        "Sony PlayStation Portable" to pspSpec,
        "Nintendo DS" to ndsSpec,
        "Nintendo GameCube" to gameCubeSpec,
        "Nintendo Wii" to wiiSpec,
        "Nintendo 3DS" to nintendo3dsSpec,
        "Nintendo Switch" to switchSpec,
        "Nintendo 64" to n64Spec
    )

    fun hasOverlay(platform: String?): Boolean =
        specForPlatform(platform) != null

    fun specForPlatform(platform: String?): NostalgicOverlaySpec? =
        if (platform in PcGameStores.platforms || platform == "PC (Windows)") digitalGameSpec else platform
            ?.takeIf { it.isNotBlank() }
            ?.let { key ->
                specByPlatform.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            }
            ?: defaultSpec
}
