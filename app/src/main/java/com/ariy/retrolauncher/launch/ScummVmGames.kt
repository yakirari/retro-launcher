package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import org.json.JSONObject
import java.util.Locale

/** Engine IDs identify data; configuration section names identify launchable targets. Never conflate them. */
internal object ScummVmGames {
    const val PACKAGE = "org.scummvm.scummvm"
    const val REGISTERED_TARGET = "scummvmRegisteredTarget"
    private val titles = mapOf(
        "sky" to "Beneath a Steel Sky", "tentacle" to "Day of the Tentacle",
        "emi" to "Escape from Monkey Island", "grim" to "Grim Fandango",
        "monkey" to "The Secret of Monkey Island", "monkey2" to "Monkey Island 2: LeChuck's Revenge",
        "maniac" to "Maniac Mansion", "dig" to "The Dig",
        "laurabow2" to "Laura Bow 2: The Dagger of Amon Ra", "samnmax" to "Sam & Max Hit the Road",
        "tlj" to "The Longest Journey", "comi" to "The Curse of Monkey Island",
        "atlantis" to "Indiana Jones and the Fate of Atlantis", "indy3" to "Indiana Jones and the Last Crusade",
        "ft" to "Full Throttle", "loom" to "Loom", "queen" to "Flight of the Amazon Queen",
        "sword1" to "Broken Sword: The Shadow of the Templars", "sword2" to "Broken Sword II: The Smoking Mirror"
    )
    private val aliases = mapOf("dot" to "tentacle", "dott" to "tentacle", "beneaths" to "sky", "btssky" to "sky",
        "maniac_mansion" to "maniac")

    fun gameId(raw: String?): String? {
        val value = raw?.trim()?.removePrefix("\uFEFF")?.lowercase(Locale.ROOT)
            ?.removeSuffix(".scummvm")?.removeSuffix(".svm") ?: return null
        val key = if (':' in value) {
            if (value.substringBefore(':') !in setOf("scumm", "sci", "sky", "grim", "stark", "queen", "sword1", "sword2")) return null
            value.substringAfter(':')
        } else value
        val id = aliases[key] ?: key
        return id.takeIf { it in titles }
    }

    fun title(raw: String?): String? = gameId(raw)?.let(titles::get)

    /** Prefer actual marker contents; conflicting markers must not depend on filesystem ordering. */
    fun detect(folderName: String, files: List<Pair<String, String?>>): String? {
        val contentIds = files.filter { it.first.endsWith(".scummvm", true) || it.first.endsWith(".svm", true) }
            .mapNotNull { gameId(it.second?.lineSequence()?.firstOrNull { line -> line.isNotBlank() }) }.distinct()
        if (contentIds.size == 1) return contentIds.single()
        if (contentIds.size > 1) return null
        val markerIds = files.mapNotNull { (name, _) ->
            if (name.endsWith(".scummvm", true) || name.endsWith(".svm", true)) gameId(name) else null
        }.distinct()
        if (markerIds.size == 1) return markerIds.single()
        if (markerIds.size > 1) return null
        val names = files.map { it.first.lowercase(Locale.ROOT) }.toSet()
        val signature = when {
            "sky.dnr" in names && "sky.dsk" in names -> "sky"
            "monkey2.000" in names && "monkey2.001" in names -> "monkey2"
            "dig.la0" in names && "dig.la1" in names -> "dig"
            "samnmax.000" in names && "samnmax.001" in names -> "samnmax"
            "tentacle.000" in names && "tentacle.001" in names -> "tentacle"
            else -> null
        }
        return signature ?: gameId(folderName).takeIf { names.isNotEmpty() }
    }

    data class RegisteredGame(val target: String, val gameId: String?, val title: String, val path: String)

    /** Parse an exported *existing* ScummVM config, not a generated replacement for its private config. */
    fun parseConfig(text: String): List<RegisteredGame> {
        require(text.length <= 2 * 1024 * 1024) { "ScummVM config is too large" }
        val sections = linkedMapOf<String, MutableMap<String, String>>()
        var section: MutableMap<String, String>? = null
        text.removePrefix("\uFEFF").lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() || line.startsWith('#') || line.startsWith(';') -> Unit
                line.startsWith('[') && line.endsWith(']') -> {
                    val name = line.substring(1, line.length - 1).trim()
                    section = sections.getOrPut(name) { linkedMapOf() }
                }
                '=' in line -> section?.put(line.substringBefore('=').trim().lowercase(Locale.ROOT), line.substringAfter('=').trim())
            }
        }
        return sections.mapNotNull { (target, values) ->
            val path = values["path"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val description = values["description"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (!validTarget(target) || target in setOf("scummvm", "cloud", "keymapper")) return@mapNotNull null
            RegisteredGame(target, values["gameid"], description, path)
        }
    }

    fun validTarget(target: String): Boolean = target.isNotBlank() && target.length <= 256 &&
        target.none { it.isWhitespace() || it in "[]:/\\" } && !target.startsWith('-')

    fun registeredTarget(extras: String?): String? = runCatching {
        JSONObject(extras.orEmpty()).optString(REGISTERED_TARGET).takeIf(::validTarget)
    }.getOrNull()

    fun extrasForTarget(target: String, extras: String? = null): String {
        require(validTarget(target)) { "Invalid ScummVM target" }
        val json = runCatching { JSONObject(extras.orEmpty()) }.getOrDefault(JSONObject())
        json.put(REGISTERED_TARGET, target)
        return json.toString()
    }

    fun artworkTitle(raw: String): String = title(raw) ?: raw
        .replace(Regex("""\s*\((?i:[^)]*(?:DOS|Windows|English|Floppy|CD|Demo)[^)]*)\)\s*$"""), "")
        .replace(Regex("(?i),\\s*The$"), "")
        .let { if (raw.substringBefore('(').trim().endsWith(", The", true)) "The $it" else it }.trim()

    fun repairTitle(game: GameEntry): GameEntry {
        if (GamePlatforms.normalizeOrNull(game.platform) != "ScummVM") return game
        val corrected = title(game.title) ?: return game
        // Only discard known auto-fill files from the old key-based query. Keep manually chosen art.
        val automaticArt = game.imageUri?.contains("${game.id}-autofill") == true
        return game.copy(title = corrected, imageUri = if (automaticArt) null else game.imageUri)
    }

    fun mergeRegistered(existing: GameEntry, incoming: GameEntry): GameEntry? {
        if (existing.packageName != PACKAGE || incoming.packageName != PACKAGE) return null
        val target = registeredTarget(incoming.extrasJson) ?: return null
        val previous = registeredTarget(existing.extrasJson)
        if (previous != null && previous != target) return null // Do not collapse demos, languages or editions.
        if (previous != target && LibretroArtworkCatalog.scummVmArtworkMatchKey(existing.title) !=
            LibretroArtworkCatalog.scummVmArtworkMatchKey(incoming.title)) return null
        val repaired = repairTitle(existing)
        return repaired.copy(
            title = if (title(existing.title) != null) incoming.title else repaired.title,
            sourceType = incoming.sourceType, packageName = incoming.packageName, activityName = incoming.activityName,
            intentAction = incoming.intentAction, extrasJson = incoming.extrasJson,
            // Retain the selected ROM folder when repairing a legacy tile; launching uses the registered target.
            intentDataUri = existing.intentDataUri ?: incoming.intentDataUri,
            preferredTargetKey = null
        )
    }
}