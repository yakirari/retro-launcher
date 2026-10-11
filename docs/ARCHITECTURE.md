# Architecture and maintenance

[Development](DEVELOPMENT.md) · [Releasing](RELEASING.md)

Compose observes ViewModel StateFlows. The ViewModel coordinates installed targets, scans, persistence,
launch intents and artwork jobs. The library uses JSON and settings use app-private SharedPreferences; images
are app-private files. The root Gradle application is `com.ariy.retrolauncher`.

## Code map

| Component | Responsibility |
| --- | --- |
| [MainActivity.kt](../app/src/main/java/com/ariy/retrolauncher/MainActivity.kt) | Compose root, filters/layouts/dialogs, input, bitmap rendering/caches |
| [LauncherViewModel.kt](../app/src/main/java/com/ariy/retrolauncher/LauncherViewModel.kt) | Library state, background work, launch/import/edit orchestration |
| [GameRepository.kt](../app/src/main/java/com/ariy/retrolauncher/data/GameRepository.kt) | JSON library/preferences persistence |
| [GameEntry.kt](../app/src/main/java/com/ariy/retrolauncher/model/GameEntry.kt) | Entries, alternate/primary targets, history and editable metadata |
| [GamePlatforms.kt](../app/src/main/java/com/ariy/retrolauncher/model/GamePlatforms.kt) | Normalization, aliases and gallery filter groups |
| [InstalledTargets.kt](../app/src/main/java/com/ariy/retrolauncher/launch/InstalledTargets.kt) | Profile-aware LauncherApps queries, launching and icons |
| [EmulatorLaunchers.kt](../app/src/main/java/com/ariy/retrolauncher/launch/EmulatorLaunchers.kt) | Known emulator/hub recognition and standalone-app classification |
| [RomDirectorySnapshot.kt](../app/src/main/java/com/ariy/retrolauncher/RomDirectorySnapshot.kt) | SAF child snapshots and cancellation |
| [GameEntryMerge.kt](../app/src/main/java/com/ariy/retrolauncher/GameEntryMerge.kt) | Additive import/upsert policy |
| [LibrarySetupPolicy.kt](../app/src/main/java/com/ariy/retrolauncher/LibrarySetupPolicy.kt) | Setup migration, verified hub/profile identity and deterministic standalone selection |
| [LibraryScanner.kt](../app/src/main/java/com/ariy/retrolauncher/LibraryScanner.kt) | Strict recognized-folder discovery using existing platform scanners/preset contracts |
| [LibrarySetupDialog.kt](../app/src/main/java/com/ariy/retrolauncher/ui/LibrarySetupDialog.kt) | Adaptive optional welcome, scan progress/cancel/retry and Home guidance |
| [GameLaunchSetupChoices.kt](../app/src/main/java/com/ariy/retrolauncher/ui/GameLaunchSetupChoices.kt) | Explicit installed preset/system/core repair for unconfigured ROM tiles |
| [ArtworkSuggestionService.kt](../app/src/main/java/com/ariy/retrolauncher/launch/ArtworkSuggestionService.kt) | Ordered catalogue/web candidate batches |
| [ImportedIconStore.kt](../app/src/main/java/com/ariy/retrolauncher/launch/ImportedIconStore.kt) | Bounded image downloads/decodes, aspect selection and atomic writes |
| [ArtworkState.kt](../app/src/main/java/com/ariy/retrolauncher/ArtworkState.kt) | Concurrent-edit-safe artwork updates |
| [GlobalGallerySettings.kt](../app/src/main/java/com/ariy/retrolauncher/model/GlobalGallerySettings.kt) | Pure global Grid/Shelf preset validation, independent counts and migration policy |
| [PlatformDisplaySettings.kt](../app/src/main/java/com/ariy/retrolauncher/model/PlatformDisplaySettings.kt) | Legacy profile serialization/defaults; no longer applied or editable in the launcher |
| [NostalgicOverlays.kt](../app/src/main/java/com/ariy/retrolauncher/model/NostalgicOverlays.kt) | Template geometry and render behavior |
| [PlatformSvgIcons.kt](../app/src/main/java/com/ariy/retrolauncher/model/PlatformSvgIcons.kt) | SVG badge routing |
| [GalleryPaging.kt](../app/src/main/java/com/ariy/retrolauncher/ui/GalleryPaging.kt) | Progressive prefixes and direct-focus paging |
| [AdaptiveLauncherDialog.kt](../app/src/main/java/com/ariy/retrolauncher/ui/AdaptiveLauncherDialog.kt) | Bounded viewport and keyboard-safe insets |

The large activity is a refactoring candidate. Extract pure policies/small composables with tests,
rather than replacing the whole UI while changing unrelated behavior.

## Launch sources vs platforms

`SHORTCUT` identifies publisher + shortcut ID + user profile; Android owns its payload.
`ACTIVITY` opens an app component. `CUSTOM_INTENT` supplies a verified action/URI/extras contract.
Launch mechanisms are not platform labels.

Known packages classify standalone activities into Emulators & Launchers; other added activities use
Android Games. Empty extras alone do not make an app an emulator. Hub game shortcuts stay in game/store
categories. The All gallery hides standalone activities; explicit app galleries remain accessible.

## Invariants to preserve

- **Data:** retain manual titles/artwork, IDs, history, alternate targets and ordering on import/repair.
  Background results must merge against the latest state, not an old snapshot.
- **Identity:** use verified ROM/title identities. Shared cabinets or loose name prefixes do not justify
  merging sequels/editions. Preserve ambiguous conflicts.
- **Cancellation:** propagate cancellation and bound queries/downloads/decodes. No network or full-image
  processing in composition. Stop stale work on filter changes.
- **Shortcuts:** stripped extras are not a store identifier. Never reconstruct private publisher payloads;
  only Android-valid pin requests with explicit consent may be accepted.
- **Storage:** selected-document and authorization-root semantics differ. Do not invent filesystem paths
  from URIs or assume another app has the same grants.
- **Rendering:** preserve originals, respect fit/crop geometry, and version cache keys for changed transforms.
  The PS1 full-cover template no longer trims banners.
- **Gallery:** only global Grid (`gridstalgic`) and Shelf (`shelfstalgic`) are selectable. Each remembers
  its own count (1, 2, 3, 4 or 8); tile backgrounds and captions are shared. Filters never resolve profiles.
  Once migrated, saved global choices survive restarts; legacy ViewModel counts are not effective counts.
- **History:** personal popularity counts successful launches; guard counters/merges against overflow.

## Performance

Large lists filter/sort on `Dispatchers.Default`. Lazy rows expose stable growing 48-item prefixes,
extending 12 items ahead of the boundary. Focus jumps load the required page. Nearby artwork prefetch
checks cancellation. Same-canvas metrics inspect a bounded first-page sample, not the entire library.

This is progressive UI exposure, not database/server paging: metadata remains in memory. Do not claim
measured frame-rate gains without benchmarks. Test long scrolls and rapid filter changes on hardware.

## Extending integrations

1. Verify the installed package/public API, profiles and storage requirements on a real version.
2. Add normalization/filter identity; keep standalone apps out of ROM galleries.
3. Validate templates or shortcut classification conservatively and document limitations.
4. Add launch profiles/badges if needed, not display overrides. SVG resource names must be lowercase/resource-safe; simple paths avoid fonts.
5. Add parsing/merge/dedup/migration tests and device smoke checks.
6. Document asset creator/source/license before submitting art.

## Persistence changes

Setup uses an explicit `setup_completed` preference, initialized before auto-added emulator tiles:
legacy persisted library/ROM/grid/filter keys migrate as complete; a truly new installation starts
in welcome. Skip and successful root selection persist completion. `selected_rom_scan_root` stores
the exact SAF picker result independently of normalized authorization. Upgrades do not infer this
root from potentially broadened permission URIs; users select it once through setup.

The ViewModel owns one library scan job, independent of composition/rotation. Platforms are scanned
sequentially on IO and imports commit against fresh main-thread state in 64-entry chunks. Cancellation
checks also feed Android cancellation signals for child-directory queries, with a shared 30-second
query watchdog. A provider that ignores Android cancellation may still delay returning; no provider
thread is forcibly terminated. Recognized platforms without a bulk scanner are reported for manual add.
Cancellation
retains completed chunks and their counters; process death requires a manual retry. Strict discovery
does not use the manual resolver's unrecognized-root fallback. Matching installed standalone presets
are selected deterministically; private RetroArch cores are never presumed available. JSON entries
default `needsLaunchSetup` to false for compatibility; placeholders explicitly set it true and cannot
launch an implicit generic intent. Repair keeps identity, custom metadata/artwork and history.

Installed refresh publishes activities/standalone tiles first, then imports accessible verified hub
shortcuts using package/ID/profile identity across all targets, never app-label matching for automatic
inclusion. Coalesced refresh requests preserve later resume/Home-role changes. Artwork remains optional
and starts after local batch commits. ScummVM private lists/folders are not silently registered.

Global gallery preferences use a dedicated version marker and keys. Before initializing Compose gallery
state, the activity synchronously commits the first-use snapshot: Grid, both counts 2 and transparent
tiles, preserving the old global caption choice. Later writes save the complete validated snapshot.
Legacy display preferences are left intact but ignored; library/metadata and launcher wallpaper keys
are untouched. Artwork platform selection is independent of display settings. Old rendering helpers
and profile model tests remain for compatibility, not as additional selectable presets.

Read old JSON with defaults and make migrations idempotent. Test malformed/missing fields and repeated
imports. Do not drop records to repair a label. There is no dedicated in-app export today, so upgrade
data preservation and signing-key continuity are essential.
