package com.ariy.retrolauncher.model

import java.text.Normalizer

object PlatformTopTen {
    private val titleFragmentsByPlatform = mapOf(
        "MAME Arcade" to listOf(
            "pac man",
            "ms pac man",
            "galaga",
            "donkey kong",
            "street fighter 2",
            "defender",
            "robotron 2084",
            "outrun",
            "turtles in time",
            "mortal kombat 2"
        ),
        GamePlatforms.ANDROID to listOf(
            "stardew valley",
            "dead cells",
            "monument valley",
            "slay the spire",
            "vampire survivors",
            "minecraft",
            "terraria",
            "balatro",
            "bloons td 6",
            "chrono trigger"
        ),
        "DOS Games" to listOf(
            "doom",
            "doom 2",
            "duke nukem 3d",
            "x com ufo defense",
            "simcity 2000",
            "secret of monkey island",
            "day of the tentacle",
            "commander keen 4",
            "system shock",
            "tie fighter"
        ),
        "Nintendo Entertainment System" to listOf(
            "super mario bros 3",
            "legend of zelda",
            "metroid",
            "mega man 2",
            "castlevania 3",
            "kirbys adventure",
            "contra",
            "punch out",
            "final fantasy",
            "ninja gaiden"
        ),
        "Sega Master System" to listOf(
            "phantasy star",
            "dragons trap",
            "sonic the hedgehog",
            "alex kidd in miracle world",
            "fantasy zone",
            "land of illusion",
            "castle of illusion",
            "shinobi",
            "power strike",
            "r type"
        ),
        "ScummVM" to listOf(
            "secret of monkey island",
            "monkey island 2",
            "day of the tentacle",
            "sam and max hit the road",
            "fate of atlantis",
            "full throttle",
            "grim fandango",
            "beneath a steel sky",
            "broken sword",
            "curse of monkey island"
        ),
        "PC Engine / TurboGrafx-16" to listOf(
            "rondo of blood",
            "soldier blade",
            "gate of thunder",
            "bonks revenge",
            "lords of thunder",
            "ys book 1 2",
            "bomberman 94",
            "devils crush",
            "blazing lazers",
            "splatterhouse"
        ),
        "Sega Genesis / Mega Drive" to listOf(
            "sonic 2",
            "streets of rage 2",
            "gunstar heroes",
            "shining force 2",
            "castlevania bloodlines",
            "phantasy star 4",
            "sonic 3 and knuckles",
            "contra hard corps",
            "landstalker",
            "beyond oasis"
        ),
        "Final Burn Neo" to listOf(
            "3rd strike",
            "marvel vs capcom 2",
            "garou mark of the wolves",
            "king of fighters 98",
            "metal slug 3",
            "blazing star",
            "dodonpachi daioujou",
            "espgaluda",
            "puzzle bobble",
            "raiden 2"
        ),
        "Game Boy" to listOf(
            "tetris",
            "pokemon red",
            "links awakening",
            "super mario land 2",
            "metroid 2",
            "kirbys dream land",
            "donkey kong",
            "wario land",
            "final fantasy legend 2",
            "pokemon yellow"
        ),
        "Neo Geo" to listOf(
            "metal slug 3",
            "garou mark of the wolves",
            "last blade 2",
            "samurai shodown 2",
            "king of fighters 98",
            "blazing star",
            "windjammers",
            "shock troopers",
            "magical drop 3",
            "metal slug x"
        ),
        "Super Nintendo" to listOf(
            "chrono trigger",
            "super metroid",
            "link to the past",
            "super mario world",
            "yoshis island",
            "final fantasy 6",
            "earthbound",
            "donkey kong country 2",
            "super mario rpg",
            "mega man x"
        ),
        "Sega CD" to listOf(
            "sonic cd",
            "snatcher",
            "lunar silver star",
            "lunar eternal blue",
            "popful mail",
            "final fight cd",
            "shining force cd",
            "silpheed",
            "keio flying squadron",
            "terminator cd"
        ),
        "Sega 32X" to listOf(
            "knuckles chaotix",
            "star wars arcade",
            "virtua racing deluxe",
            "kolibri",
            "doom",
            "shadow squadron",
            "tempo",
            "virtua fighter",
            "mortal kombat 2",
            "space harrier"
        ),
        "Sega Saturn" to listOf(
            "panzer dragoon saga",
            "nights into dreams",
            "sega rally championship",
            "radiant silvergun",
            "guardian heroes",
            "dragon force",
            "saturn bomberman",
            "virtua fighter 2",
            "burning rangers",
            "shining force 3"
        ),
        "Sony PlayStation" to listOf(
            "metal gear solid",
            "symphony of the night",
            "final fantasy 7",
            "final fantasy 9",
            "resident evil 2",
            "gran turismo 2",
            "silent hill",
            "xenogears",
            "tony hawks pro skater 2",
            "vagrant story"
        ),
        "Nintendo 64" to listOf(
            "ocarina of time",
            "super mario 64",
            "majoras mask",
            "goldeneye 007",
            "mario kart 64",
            "banjo kazooie",
            "perfect dark",
            "star fox 64",
            "paper mario",
            "f zero x"
        ),
        "Game Boy Color" to listOf(
            "pokemon crystal",
            "oracle of ages",
            "oracle of seasons",
            "pokemon gold",
            "pokemon silver",
            "metal gear solid",
            "wario land 3",
            "dragon warrior 3",
            "shantae",
            "tetris dx"
        ),
        "Neo Geo Pocket" to listOf(
            "match of the millennium",
            "sonic pocket adventure",
            "metal slug 1st mission",
            "samurai shodown 2",
            "last blade",
            "card fighters clash",
            "king of fighters r 2",
            "dark arms",
            "fatal fury first contact",
            "biomotor unitron"
        ),
        "Sega Dreamcast" to listOf(
            "shenmue",
            "soulcalibur",
            "jet set radio",
            "crazy taxi",
            "code veronica",
            "skies of arcadia",
            "phantasy star online",
            "marvel vs capcom 2",
            "ikaruga",
            "power stone 2"
        ),
        "Neo Geo Pocket Color" to listOf(
            "match of the millennium",
            "sonic pocket adventure",
            "metal slug 2nd mission",
            "card fighters clash",
            "biomotor unitron",
            "dark arms",
            "king of fighters r 2",
            "last blade",
            "fatal fury first contact",
            "snk gals fighters"
        ),
        "Sony PlayStation 2" to listOf(
            "shadow of the colossus",
            "snake eater",
            "san andreas",
            "god of war 2",
            "persona 4",
            "final fantasy 10",
            "silent hill 2",
            "okami",
            "devil may cry 3",
            "burnout 3"
        ),
        "Game Boy Advance" to listOf(
            "advance wars",
            "aria of sorrow",
            "metroid fusion",
            "minish cap",
            "fire emblem",
            "superstar saga",
            "warioware",
            "pokemon emerald",
            "golden sun",
            "final fantasy tactics advance"
        ),
        "Nintendo GameCube" to listOf(
            "metroid prime",
            "resident evil 4",
            "wind waker",
            "smash bros melee",
            "thousand year door",
            "double dash",
            "f zero gx",
            "super mario sunshine",
            "eternal darkness",
            "viewtiful joe"
        ),
        "Nintendo DS" to listOf(
            "world ends with you",
            "pokemon heartgold",
            "mario kart ds",
            "dawn of sorrow",
            "ghost trick",
            "dual strike",
            "new super mario bros",
            "chrono trigger",
            "animal crossing wild world",
            "unwound future"
        ),
        "Sony PlayStation Portable" to listOf(
            "ghost of sparta",
            "crisis core",
            "persona 3 portable",
            "freedom unite",
            "peace walker",
            "lumines",
            "jeanne d arc",
            "war of the lions",
            "patapon 2",
            "daxter"
        ),
        "Sony PlayStation 3" to listOf(
            "the last of us",
            "uncharted 2",
            "metal gear solid 4",
            "demons souls",
            "journey",
            "arkham city",
            "red dead redemption",
            "gta 5",
            "persona 5",
            "dark souls"
        ),
        "Nintendo Wii" to listOf(
            "super mario galaxy 2",
            "super mario galaxy",
            "metroid prime trilogy",
            "xenoblade chronicles",
            "skyward sword",
            "donkey kong country returns",
            "mario kart wii",
            "smash bros brawl",
            "new super mario bros wii",
            "resident evil 4 wii edition"
        ),
        "Nintendo 3DS" to listOf(
            "link between worlds",
            "fire emblem awakening",
            "super mario 3d land",
            "monster hunter 4 ultimate",
            "animal crossing new leaf",
            "kid icarus uprising",
            "pokemon sun",
            "pokemon moon",
            "samus returns",
            "mario kart 7"
        ),
        "Sony PlayStation Vita" to listOf(
            "persona 4 golden",
            "gravity rush",
            "golden abyss",
            "tearaway",
            "memories of celceta",
            "hotline miami",
            "muramasa rebirth",
            "spelunky",
            "lumines electronic symphony",
            "olliolli 2"
        ),
        "Nintendo Switch" to listOf(
            "breath of the wild",
            "tears of the kingdom",
            "super mario odyssey",
            "mario kart 8 deluxe",
            "metroid prime remastered",
            "metroid dread",
            "smash bros ultimate",
            "animal crossing new horizons",
            "xenoblade chronicles 3",
            "hades"
        )
    ).mapValues { (_, titles) -> titles.map(::normalizeTitleFragment) }

    fun rankFor(game: GameEntry): Int? {
        val normalizedPlatform = GamePlatforms.normalizeOrNull(game.platform) ?: return null
        val titleFragments = titleFragmentsByPlatform[normalizedPlatform] ?: return null
        val normalizedTitle = normalizeTitleFragment(game.title)
        if (normalizedTitle.isBlank()) return null
        val rank = titleFragments.indexOfFirst { fragment ->
            normalizedTitle == fragment || normalizedTitle.contains(fragment)
        }
        return if (rank >= 0) rank + 1 else null
    }

    private fun normalizeTitleFragment(raw: String): String {
        if (raw.isBlank()) return ""
        val stripped = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("&", " and ")
            .replace("'", "")
            .replace(Regex("[^a-zA-Z0-9]+"), " ")
            .trim()
            .lowercase()
        return stripped
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .map(::normalizeRomanNumeral)
            .joinToString(" ")
    }

    private fun normalizeRomanNumeral(token: String): String = when (token) {
        "i" -> "1"
        "ii" -> "2"
        "iii" -> "3"
        "iv" -> "4"
        "v" -> "5"
        "vi" -> "6"
        "vii" -> "7"
        "viii" -> "8"
        "ix" -> "9"
        "x" -> "10"
        else -> token
    }
}
