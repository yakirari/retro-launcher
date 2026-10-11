package com.ariy.retrolauncher.launch

import java.io.File
import java.util.Locale

/** Standalone catalog verification; compile alongside ArcadeGameCatalog.kt, no Android/Gradle. */
fun main(args: Array<String>) {
    val paths = File(args[0]).readLines().filter(String::isNotBlank)
    val roms = paths.map { File(it.trim()).name.removeSuffix(".zip").replace(Regex(" \\([0-9]+\\)$"), "") }.toSet()
    check(paths.size == 750 && roms.size == 383)
    for (path in paths) {
        val name = File(path.trim()).name
        val game = checkNotNull(ArcadeGameCatalog.find(name)) { "Unresolved $name" }
        check(game.title.isNotBlank() && game.description.isNotBlank())
    }
    for (game in ArcadeGameCatalog.games) {
        check(ArcadeGameCatalog.find(game.rom) === game)
        check(ArcadeGameCatalog.find("${game.rom} (123).ZIP") === game)
        check(game.description.length > 30)
        for (clone in game.clones) check(ArcadeGameCatalog.find(clone) === game)
    }
    check(ArcadeGameCatalog.find("tmnt22p")?.rom == "tmnt2")
    check(ArcadeGameCatalog.find("thndrbdj")?.rom == "thndrbld")
    check(ArcadeGameCatalog.canonicalParent("thndrbdj (3).zip") == "thndrbld")
    check(ArcadeGameCatalog.canonicalParent("sf2j") == "sf2")
    check(ArcadeGameCatalog.canonicalParent("defenderg") == null) // preserved, but unverified alias
    for ((a, b) in listOf("dkong" to "dkong3", "rtype" to "rtype2", "tmnt" to "tmnt2",
        "aburner" to "aburner2", "mk3" to "umk3", "sf2ce" to "sf2t")) {
        check(ArcadeGameCatalog.find(a) !== ArcadeGameCatalog.find(b)) { "Conflated $a/$b" }
    }
    // An exact ROM wins over a coincident display title (Tetris is also the Sega ROM name).
    check(ArcadeGameCatalog.find("Tetris")?.rom == "tetris")
    for (unknown in listOf("sf2 (Japan)", "sf2 (1) (2)", "sf2abc", "dkong4", "sf2.foo", "unknown (7).zip"))
        check(ArcadeGameCatalog.find(unknown) == null) { "Guessed $unknown" }
    check(ArcadeGameCatalog.find("Street Fighter II: The World Warrior")?.rom == "sf2")
    val locale = Locale.getDefault()
    try {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        check(ArcadeGameCatalog.find("INVADERS.ZIP")?.rom == "invaders")
    } finally { Locale.setDefault(locale) }
    println("Passed: all 750 filenames / 383 distinct ROMs, ${ArcadeGameCatalog.games.size} games, aliases, descriptions, clone evidence, sequels, conservative negatives and locale independence.")
}