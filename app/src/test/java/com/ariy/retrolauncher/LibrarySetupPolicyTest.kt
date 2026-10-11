package com.ariy.retrolauncher

import com.ariy.retrolauncher.launch.CustomIntentTemplate
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import com.ariy.retrolauncher.launch.InstalledShortcutOption
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.toJsonString
import org.junit.Assert.*
import org.junit.Test

class LibrarySetupPolicyTest {
    @Test fun explicitFlagWinsAndExistingUsersMigrateWithoutLibraryEmptinessHeuristics() {
        assertFalse(LibrarySetupPolicy.migrateCompleted(null, false))
        assertTrue(LibrarySetupPolicy.migrateCompleted(null, true))
        assertFalse(LibrarySetupPolicy.migrateCompleted(false, true))
        assertTrue(LibrarySetupPolicy.migrateCompleted(true, false))
    }

    @Test fun automaticRootRecognitionDoesNotTreatSiblingPlatformOrContainerAsTheSystem() {
        assertTrue(isRecognizedRomScanRoot(listOf("SNES"), "snes", "Super Nintendo"))
        assertTrue(isRecognizedRomScanRoot(listOf("Famicom"), "nes", "Nintendo Entertainment System"))
        assertFalse(isRecognizedRomScanRoot(listOf("PS2"), "psp", "Sony PlayStation Portable"))
        assertFalse(isRecognizedRomScanRoot(listOf("ROMs"), "snes", "Super Nintendo"))
        assertFalse(isRecognizedRomScanRoot(listOf("GameCube"), "wii", "Nintendo Wii"))
    }

    private fun option(index: Int, core: String? = null, platform: String = "Super Nintendo") = GameSystemEmulatorOption(
        key = "option-$index", templateIndex = index, systemId = "snes", systemLabel = platform,
        platform = platform, romFolderHint = "snes", pickerKind = GamePickerKind.FILE, retroArchSystemId = core
    )

    @Test fun standaloneSelectionIsStableHonorsExistingPreferenceAndNeverGuessesRetroArchCore() {
        val templates = listOf(CustomIntentTemplate("B", "", packageName = "b"),
            CustomIntentTemplate("A", "", packageName = "a"), CustomIntentTemplate("RA", "", packageName = "com.retroarch"))
        val options = listOf(option(0), option(1), option(2, "snes"))
        assertEquals(1, LibrarySetupPolicy.selectStandalone(options, templates, "Super Nintendo", setOf("a", "b"), null)?.templateIndex)
        assertEquals(0, LibrarySetupPolicy.selectStandalone(options, templates, "Super Nintendo", setOf("a", "b"), "b")?.templateIndex)
        assertNull(LibrarySetupPolicy.selectStandalone(options, templates, "Super Nintendo", setOf("com.retroarch"), null))
        assertNull(LibrarySetupPolicy.selectStandalone(options, templates, "Game Boy", setOf("a"), null))
        assertNull(LibrarySetupPolicy.selectStandalone(options, templates, "Super Nintendo", emptySet(), null))
    }

    @Test fun sharedSystemMappingsReuseManualPresets() {
        val options = buildGameSystemEmulatorOptions(CustomIntentTemplates.all)
        fun platforms(pkg: String) = options.filter { CustomIntentTemplates.all[it.templateIndex].packageName == pkg }
            .mapNotNull { it.platform }.toSet()
        assertEquals(setOf("Nintendo GameCube", "Nintendo Wii"), platforms("org.dolphinemu.dolphinemu"))
        assertEquals(setOf("Game Boy", "Game Boy Color"), platforms("com.explusalpha.GbcEmu"))
        assertEquals(setOf("Neo Geo Pocket", "Neo Geo Pocket Color"), platforms("com.explusalpha.NgpEmu"))
        assertTrue(platforms("com.explusalpha.MdEmu").containsAll(listOf("Sega Master System", "Sega CD", "Sega 32X")))
    }

    private fun shortcut(id: String, profile: Long = 0, pkg: String = "app.gamenative") = InstalledShortcutOption(
        title = "Same title", subtitle = pkg, sourceLabel = "GameNative", packageName = pkg,
        shortcutId = if (pkg == "app.gamenative") when (id) {
            "one" -> "game_1"
            "two" -> "game_2"
            else -> id
        } else id, userSerial = profile, shortLabel = "Same title", longLabel = null
    )
    private fun game(option: InstalledShortcutOption) = GameEntry(
        title = option.title, platform = "GameNative", sourceType = GameSourceType.SHORTCUT,
        packageName = option.packageName, shortcutId = option.shortcutId, userSerial = option.userSerial
    )

    @Test fun hubFilterUsesVerifiedPackagesNotLabelsAndDedupsAllTargetsWithProfile() {
        val primary = shortcut("one")
        val alternate = game(shortcut("two")).primaryTarget()
        val existing = game(primary).copy(title = "Manual title", alternateTargetsJson = listOf(alternate).toJsonString())
        val work = shortcut("one", 10)
        val hub = shortcut("game", pkg = "com.xiaoji.egggame")
        val lite = shortcut("game", pkg = "gamehub.lite")
        val unrelated = shortcut("other", pkg = "unrelated.package")
        assertEquals(listOf(work, hub, lite), LibrarySetupPolicy.missingHubShortcuts(
            listOf(primary, shortcut("two"), work, work, hub, lite, unrelated,
                shortcut("settings"), shortcut("game_3").copy(intentAction = "invalid.action")), listOf(existing)))
    }

    @Test fun differentShortcutIdsAndProfilesWithSameTitleRemainSeparate() {
        val first = game(shortcut("one"))
        val library = mutableListOf(first)
        val second = game(shortcut("two"))
        val work = game(shortcut("one", 10))
        assertEquals(UpsertOutcome.ADDED, upsertOrMergeInPlace(library, second, second.primaryTarget()).outcome)
        assertEquals(UpsertOutcome.ADDED, upsertOrMergeInPlace(library, work, work.primaryTarget()).outcome)
        assertEquals(3, library.size)
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, first.copy(title = "Changed publisher title"), first.primaryTarget()).outcome)
        val changedPayload = first.copy(title = "New publisher title", intentDataUri = "redacted-or-new")
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, changedPayload, changedPayload.primaryTarget()).outcome)
    }

    @Test fun verifiedGameNativePayloadCanMergeWithManualTargetWithoutGuessingOpaqueIds() {
        val manual = GameEntry(title = "Custom title", platform = "Steam", sourceType = GameSourceType.CUSTOM_INTENT,
            packageName = "app.gamenative", intentAction = "app.gamenative.LAUNCH_GAME",
            extrasJson = "{\"app_id\":1,\"game_source\":\"STEAM\"}", imageUri = "file:///mine.png")
        val incoming = game(shortcut("one")).copy(extrasJson = manual.extrasJson, platform = "Steam")
        val library = mutableListOf(manual)
        assertEquals(UpsertOutcome.MERGED, upsertOrMergeInPlace(library, incoming, incoming.primaryTarget()).outcome)
        assertEquals(1, library.size)
        assertEquals(manual.title, library.single().title)
        assertEquals(manual.imageUri, library.single().imageUri)
        assertEquals(2, library.single().allTargets().size)
    }

    @Test fun placeholderImportIsDurableIdempotentAndCanBeRepairedWithoutLosingUserData() {
        val pending = GameEntry(title = "Original", subtitle = "Needs emulator setup", platform = "Super Nintendo",
            sourceType = GameSourceType.CUSTOM_INTENT, intentDataUri = "content://rom/game", needsLaunchSetup = true)
        val manual = pending.copy(title = "My title", imageUri = "file:///mine.png", playCount = 5,
            genre = "Favorites", lastPlayedAtEpochMillis = 123)
        val library = mutableListOf(manual)
        assertTrue(GameEntry.fromJson(pending.toJson()).needsLaunchSetup)
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, pending, pending.primaryTarget()).outcome)
        val ready = pending.copy(packageName = "com.explusalpha.Snes9xPlus", intentAction = "android.intent.action.VIEW",
            subtitle = "Snes9x EX+", needsLaunchSetup = false)
        assertEquals(UpsertOutcome.MERGED, upsertOrMergeInPlace(library, ready, ready.primaryTarget()).outcome)
        assertEquals(manual.id, library.single().id)
        assertEquals(manual.title, library.single().title)
        assertEquals(manual.imageUri, library.single().imageUri)
        assertEquals(manual.playCount, library.single().playCount)
        assertEquals(manual.genre, library.single().genre)
        assertFalse(library.single().needsLaunchSetup)
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, pending, pending.primaryTarget()).outcome)
    }

    @Test fun cancellationRetainsCommittedCountsAndNoLongerReportsRunning() {
        val progress = LibraryScanProgress(running = true, totalPlatforms = 24, completedPlatforms = 3,
            discovered = 10, imported = 7, skipped = 3, unconfigured = 2, errors = 1)
        val cancelled = progress.finished(cancelled = true)
        assertFalse(cancelled.running)
        assertTrue(cancelled.cancelled)
        assertEquals(progress.discovered, cancelled.imported + cancelled.skipped)
        assertEquals(progress.completedPlatforms, cancelled.completedPlatforms)
        assertEquals(progress.unconfigured, cancelled.unconfigured)
        assertEquals(progress.errors, cancelled.errors)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun queryCheckpointObservesCancelledScanJob() {
        val job = kotlinx.coroutines.Job()
        val check = RomScanCancellationCheck(job)
        check()
        job.cancel()
        check()
    }
}