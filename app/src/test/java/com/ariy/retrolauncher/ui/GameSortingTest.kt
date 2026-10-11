package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class GameSortingTest {
    private fun game(title: String, year: String? = null, played: Long? = null, count: Long = 0) = GameEntry(
        id = title, title = title, releaseYear = year, lastPlayedAtEpochMillis = played, playCount = count,
        sourceType = GameSourceType.CUSTOM_INTENT
    )
    @Test fun alphabeticalAppliesWithinBothGroupsNotByRecentTimestamp() {
        val result = sortLibraryGames(listOf(game("Zebra", played=100), game("apple"), game("Alpha", played=10), game("Banana")), GameSortOrder.ALPHABETICAL)
        assertEquals(listOf("Alpha", "Zebra", "apple", "Banana"), result.map { it.title })
    }
    @Test fun yearAscendingAndDescendingKeepUnknownYearsLastWithinEachGroup() {
        val games = listOf(game("Unknown", played=1), game("New", "2000",1), game("Old", "1980",1), game("Unplayed", "1970"), game("Missing"))
        assertEquals(listOf("Old","New","Unknown","Unplayed","Missing"), sortLibraryGames(games,GameSortOrder.YEAR_ASCENDING).map { it.title })
        assertEquals(listOf("New","Old","Unknown","Unplayed","Missing"), sortLibraryGames(games,GameSortOrder.YEAR_DESCENDING).map { it.title })
    }
    @Test fun popularityIsPersonalLaunchCountWithLegacyHistoryPreserved() {
        val games = listOf(game("Legacy",played=1),game("Frequent",played=2,count=5),game("Recent",played=100,count=1),game("Never"))
        assertEquals(listOf("Frequent","Recent","Legacy","Never"),sortLibraryGames(games,GameSortOrder.POPULARITY).map { it.title })
    }
    @Test fun manualYearOverridesArcadeMetadataAndUnknownDoesNotBecomeZero() {
        val arcade = game("pacman").copy(platform="MAME Arcade")
        assertEquals(1980,arcade.libraryReleaseYear())
        assertEquals(1985,arcade.copy(releaseYear="1985").libraryReleaseYear())
        assertNull(game("Unknown","bad").libraryReleaseYear())
    }
    @Test fun playCountRoundTripAndOldJsonHaveSafeDefaults() {
        val entry = game("Test",count=7)
        assertEquals(7L,GameEntry.fromJson(entry.toJson()).playCount)
        val old = entry.toJson().apply { remove("playCount") }
        assertEquals(0L,GameEntry.fromJson(old).playCount)
        assertEquals(GameSortOrder.ALPHABETICAL,GameSortOrder.fromStored("invalid"))
    }
    @Test fun sortingDoesNotMutateStoredOrderAndEqualKeysAreStable() {
        val games=listOf(game("Z"),game("A"),game("A").copy(id="second"))
        val sorted=sortLibraryGames(games,GameSortOrder.ALPHABETICAL)
        assertEquals(listOf("A","second","Z"),sorted.map { it.id })
        assertEquals("Z",games.first().title)
    }
}