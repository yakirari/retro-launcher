package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.toJsonString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwitchGameEntryDeduperTest {
    @Test fun filenameFallbackPromotesBaseRatherThanLaunchingAnUpdate() {
        val update = game("update", "Game Update", "content://rom/Game%20Upd%203.nsp")
        val base = game("base", "Game", "content://rom/Game.nsp")
        val result = dedupeSwitchGameEntries(listOf(update, base), emptyMap()).single()
        assertEquals(base.intentDataUri, result.intentDataUri)
        assertEquals(2, result.allTargets().size)
    }

    @Test fun filenameFallbackRepairsExistingReleaseDuplicatesAndPreservesEdits() {
        val base = game("base", "My edited title", "content://rom/Game%20(USA)%20%5Bv0%5D.nsp")
            .copy(imageUri = "file://manual-cover", genre = "Action", lastPlayedAtEpochMillis = 123L)
        val variant = game("variant", "Game Upd 3", "content://rom/Game%20Upd%203.nsp")
            .copy(lastPlayedAtEpochMillis = 456L)
        val result = dedupeSwitchGameEntries(listOf(base, variant), emptyMap()).single()
        assertEquals("base", result.id)
        assertEquals("My edited title", result.title)
        assertEquals("file://manual-cover", result.imageUri)
        assertEquals("Action", result.genre)
        assertEquals(456L, result.lastPlayedAtEpochMillis)
        assertEquals(2, result.allTargets().size)
    }

    @Test fun conflictingKnownIdsNeverMergeEvenWithIdenticalFilenames() {
        val first = game("first", "Game", "content://one/Game.nsp")
        val second = game("second", "Game", "content://two/Game.xci")
        val ids = mapOf(
            first.id to mapOf(first.primaryTarget().stableKey() to "0100ABCD12345000"),
            second.id to mapOf(second.primaryTarget().stableKey() to "0100ABCD12346000")
        )
        assertEquals(2, dedupeSwitchGameEntries(listOf(first, second), ids).size)
    }

    @Test fun filenameIdsReconcileWithoutPackageMetadataAndKeepSequelsSeparate() {
        val base = game("base", "Game", "content://rom/Game%20%5B0100ABCD12345000%5D.nsp")
        val update = game("update", "Game Update", "content://rom/Game%20%5B0100ABCD12345800%5D.nsp")
        val sequel = game("sequel", "Game 2", "content://rom/Game%202.nsp")
        val result = dedupeSwitchGameEntries(listOf(update, base, sequel), emptyMap())
        assertEquals(2, result.size)
        assertEquals(base.intentDataUri, result.first().intentDataUri)
        assertEquals("sequel", result.last().id)
    }

    @Test
    fun mergesSwitchTitleIdVariantsAndRetainsLaunchTargets() {
        val base = game("base", "Breath of the Wild", "content://rom/base.nsp")
        val update = game("update", "Breath of the Wild Update", "content://rom/update.nsp")

        val result = dedupeSwitchGameEntries(
            entries = listOf(base, update),
            rawTitleIdsByGameId = mapOf(
                "base" to mapOf(base.primaryTarget().stableKey() to "0100ABCD12345000"),
                "update" to mapOf(update.primaryTarget().stableKey() to "0100ABCD12345800")
            )
        )

        assertEquals(1, result.size)
        assertEquals("base", result.single().id)
        assertEquals("content://rom/update.nsp", result.single().alternateTargets().single().intentDataUri)
    }

    @Test
    fun baseTargetIsPromotedWhenAddonEntryAppearsFirst() {
        val addon = game("addon", "Breath of the Wild Update", "content://rom/update.nsp")
        val base = game("base", "Breath of the Wild", "content://rom/base.nsp")

        val result = dedupeSwitchGameEntries(
            entries = listOf(addon, base),
            rawTitleIdsByGameId = mapOf(
                "addon" to mapOf(addon.primaryTarget().stableKey() to "0100ABCD12345800"),
                "base" to mapOf(base.primaryTarget().stableKey() to "0100ABCD12345000")
            )
        ).single()

        assertEquals("content://rom/base.nsp", result.primaryTarget().intentDataUri)
        assertEquals(result.primaryTarget().stableKey(), result.preferredTargetKey)
        assertEquals("content://rom/update.nsp", result.alternateTargets().single().intentDataUri)
    }

    @Test
    fun promotesBaseTargetInsideAnAlreadyGroupedEntry() {
        val addon = game("grouped", "Breath of the Wild", "content://rom/update.nsp")
        val baseTarget = addon.primaryTarget().copy(
            label = "Breath of the Wild",
            intentDataUri = "content://rom/base.nsp"
        )
        val grouped = addon.copy(alternateTargetsJson = listOf(baseTarget).toJsonString())

        val result = dedupeSwitchGameEntries(
            entries = listOf(grouped),
            rawTitleIdsByGameId = mapOf(
                "grouped" to mapOf(
                    addon.primaryTarget().stableKey() to "0100ABCD12345800",
                    baseTarget.stableKey() to "0100ABCD12345000"
                )
            )
        ).single()

        assertEquals("content://rom/base.nsp", result.primaryTarget().intentDataUri)
        assertEquals(result.primaryTarget().stableKey(), result.preferredTargetKey)
    }

    @Test
    fun doesNotMergeDifferentFamiliesPlatformsOrInvalidIds() {
        val first = game("first", "Same title", "content://rom/first.nsp")
        val second = game("second", "Same title", "content://rom/second.nsp")
        val otherPlatform = second.copy(id = "other-platform", platform = "Nintendo DS")
        val ids = mapOf(
            "first" to mapOf(first.primaryTarget().stableKey() to "0100ABCD12345000"),
            "second" to mapOf(second.primaryTarget().stableKey() to "0100ABCD12346000"),
            "other-platform" to mapOf(otherPlatform.primaryTarget().stableKey() to "0100ABCD12345000")
        )

        val distinctFamilies = dedupeSwitchGameEntries(listOf(first, second), ids)
        assertEquals(2, distinctFamilies.size)

        val separatePlatforms = dedupeSwitchGameEntries(listOf(first, otherPlatform), ids)
        assertEquals(2, separatePlatforms.size)

        val invalidIds = dedupeSwitchGameEntries(
            listOf(first, second),
            mapOf("first" to emptyMap(), "second" to mapOf(second.primaryTarget().stableKey() to "not-a-title-id"))
        )
        assertEquals(2, invalidIds.size)
        assertNull(invalidIds.first().alternateTargetsJson)
    }

    private fun game(id: String, title: String, uri: String) = GameEntry(
        id = id,
        title = title,
        platform = "Nintendo Switch",
        sourceType = GameSourceType.CUSTOM_INTENT,
        intentDataUri = uri
    )
}
