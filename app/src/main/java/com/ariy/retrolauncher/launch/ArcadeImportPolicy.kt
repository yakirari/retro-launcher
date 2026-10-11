package com.ariy.retrolauncher.launch

import java.util.Locale

internal data class ArcadeRomForImport(val fileName: String, val uri: String)

/** Artwork is presentation, not proof that a ROM may be imported. */
internal fun arcadeRomIdentity(raw: String?): String? {
    val name = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val stem = if (extension in setOf("zip", "7z", "chd")) name.substringBeforeLast('.') else name
    val clean = stem.replace(Regex(" \\([0-9]+\\)$"), "").lowercase(Locale.ROOT)
    return ArcadeGameCatalog.find(clean)?.rom ?: clean.takeIf(String::isNotBlank)
}

internal fun selectArcadeRomsForImport(
    roms: List<ArcadeRomForImport>,
    existingKeys: Set<String>,
    existingUris: Set<String>
): List<ArcadeScanCandidate> = roms.asSequence()
    .filter { it.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("zip", "7z", "chd") }
    .filterNot { it.uri in existingUris || arcadeRomIdentity(it.fileName) in existingKeys }
    .groupBy { arcadeRomIdentity(it.fileName) }
    .values.map { group ->
        val preferred = group.sortedWith(
            compareBy<ArcadeRomForImport> { Regex(" \\([0-9]+\\)\\.[^.]+$").containsMatchIn(it.fileName) }
                .thenBy { listOf("zip", "7z", "chd").indexOf(it.fileName.substringAfterLast('.').lowercase(Locale.ROOT)) }
                .thenBy { it.fileName.lowercase(Locale.ROOT) }
                .thenBy { it.uri }
        ).first()
        ArcadeScanCandidate(
            title = ArcadeGameCatalog.find(preferred.fileName)?.title
                ?: preferred.fileName.substringBeforeLast('.').replace(Regex(" \\([0-9]+\\)$"), ""),
            romUri = preferred.uri
        )
    }.sortedBy { it.title.lowercase(Locale.ROOT) }