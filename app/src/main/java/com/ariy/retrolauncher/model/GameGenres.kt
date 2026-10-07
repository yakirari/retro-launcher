package com.ariy.retrolauncher.model

object GameGenres {
    const val ALL = "All"

    val values = listOf(
        "Utils",
        "Platformers",
        "Action / Run & Gun",
        "Beat 'em Up",
        "Fighting",
        "Shooters / Shmups",
        "RPG",
        "Adventure",
        "Puzzle",
        "Racing",
        "Sports",
        "Strategy / Tactics",
        "Simulation"
    )

    val filters = listOf(ALL) + values

    fun normalizeOrNull(raw: String?): String? {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return values.firstOrNull { it.equals(normalized, ignoreCase = true) }
    }

    fun normalizeFilter(raw: String?): String = when {
        raw.isNullOrBlank() -> ALL
        raw.equals(ALL, ignoreCase = true) -> ALL
        else -> values.firstOrNull { it.equals(raw, ignoreCase = true) } ?: ALL
    }

    fun inferFromTitle(title: String): String? {
        val value = title.lowercase()
        return when {
            value.containsAny("settings", "config", "utility", "manager", "launcher", "tools") -> "Utils"
            value.containsAny("mario", "sonic", "donkey kong", "platform", "rayman", "celeste", "metroidvania") -> "Platformers"
            value.containsAny("contra", "metal slug", "run and gun", "run & gun", "broforce", "gunstar", "cuphead") -> "Action / Run & Gun"
            value.containsAny("beat em up", "beat 'em up", "streets of rage", "final fight", "double dragon", "river city") -> "Beat 'em Up"
            value.containsAny("street fighter", "tekken", "mortal kombat", "king of fighters", "fighting") -> "Fighting"
            value.containsAny("shmup", "bullet hell", "gradius", "raiden", "r-type", "touhou", "shoot") -> "Shooters / Shmups"
            value.containsAny("rpg", "final fantasy", "dragon quest", "persona", "chrono", "xenoblade") -> "RPG"
            value.containsAny("zelda", "adventure", "point and click", "monkey island", "walking sim") -> "Adventure"
            value.containsAny("tetris", "puyo", "puzzle", "match", "brain", "portal") -> "Puzzle"
            value.containsAny("racing", "kart", "forza", "need for speed", "gran turismo", "f1", "burnout") -> "Racing"
            value.containsAny("fifa", "nba", "2k", "football", "soccer", "tennis", "baseball", "sports", "wwe") -> "Sports"
            value.containsAny("tactics", "strategy", "civilization", "xcom", "fire emblem", "advance wars", "total war") -> "Strategy / Tactics"
            value.containsAny("simulator", "sim", "tycoon", "simcity", "the sims", "animal crossing", "farm") -> "Simulation"
            else -> null
        }
    }
}

private fun String.containsAny(vararg tokens: String): Boolean = tokens.any(::contains)
