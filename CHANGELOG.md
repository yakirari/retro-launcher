# Changelog

Release preparation and development history. A draft is not a published APK release.

## Unreleased

No additional changes beyond the first-preview draft below.

## 0.1.0 — First preview (draft)

Prepared 2026-10-11. See the [first release page](docs/releases/0.1.0.md) for requirements,
validation, known limitations and publication blockers.

### User-facing
- MAME setup includes explicit installed RetroArch core presets and prompts for an unconfigured
	single emulator too; skipping a platform imports no games from its folder.
- Manual and automatic imports prepare merges/deduplication/serialization off the UI thread in
	small durable batches, with cancellable progress and a bounded artwork drain afterward.
- Compact short-landscape modals move detailed progress, help and navigation rows into scrollable
	content where possible, leaving more usable space for choices and forms.
- Player One Lounge (formerly Basement 7) is the unset-preference default wallpaper. Every bundled
	background has a descriptive scene name; existing saved choices are preserved.
- Options now includes a prominent **Reset launcher** destructive section with an explicit,
  Cancel-focused confirmation. Reset stops/drains background jobs, clears library/history/custom
  metadata, private artwork/cache, all settings and saved folder/image grants, then returns to welcome
  with Grid, 2 tiles and transparent backgrounds without restarting. ROMs/shared files and installed
  apps are never deleted; emulator/hub app discovery remains available but old hub games are not
  silently reimported during welcome. Cleanup failures are reported; there is no undo or in-app export.
- Adaptive first-run welcome with optional skip, Home setup and one-root automatic ROM scanning.
	Sequential cancellable scans survive rotation, preserve existing library data, report progress/errors,
	and can be retried through Options → Scan library / Setup. Existing users are not forced into welcome.
- Platforms without a supported installed emulator preset are skipped with manual-add guidance;
	private RetroArch core availability is not guessed. Existing Needs emulator setup tiles retain
	an explicit preset/system/core repair path. Unlinked ScummVM folders are reported with
	registered-game/config instructions rather than silently registered.
- Installed emulator/hub tiles appear before slower shortcut/icon tasks. Accessible shortcuts from
	verified GameNative/GameHub/GameHub Lite packages auto-import on refresh, retaining profile identity;
	private libraries and pins belonging only to another Home launcher remain inaccessible.
- Artwork starts after local library import; offline failures retain imported entries and shortcut icons.
- Two global gallery presets: Grid (Gridstalgic) and Shelf (Shelfstalgic), each remembering a tile
	count of 1, 2, 3, 4 or 8. First use resets legacy display settings to Grid, 2 tiles and transparent
	tile backgrounds; library/metadata remain unchanged. Platform navigation no longer overrides appearance.
- Global tile backgrounds and captions; dedicated Artwork tab retains platform image refresh.
- Progressive gallery pages, asynchronous list preparation and bounded artwork/layout work.
- Separate Android Games and Emulators & Launchers galleries.
- Android-valid pin confirmation and automatic game shortcut addition.
- GameNative/GameHub/GameHub Lite launcher entries and shortcut setup/import controls.
- ScummVM registered-game picker/exported configuration import.
- ROM scan cancellation/cache improvements and conservative Switch/arcade identities.
- Exact-title digital portrait covers and full-cover PS1 jewel-case template.
- Alphabetical/year/personal-popularity sorting, captions and played-first grouping.

### Project
- MIT license for original launcher code/documentation.
- User, integration, developer, architecture, contribution, security and release guides.
- Explicit third-party artwork/publication checklist and separate vendor licensing scope.
- Recorded project-author ownership of launcher SVGs; removed obsolete GIMP working files and unused
	PNG template alternatives while retaining build and regeneration inputs.
- Packaged cabinet source/contributor credits, modification notes and optional upstream support link
	without changing the artwork or relabeling third-party images as MIT.
- Recorded authorship of original raster artwork, corrected the duplicated README, and removed unused
	embedded/vendored RPCSX sources and watcher references.
- Redacted developer device/storage identifiers and made the conversion script's JDK selection portable.
- Added GitHub build/test workflow, issue/PR templates, maintainer links and hosting/security setup checklist.

### Validation

- 190 JVM tests passed; debug app and instrumentation-test APKs compiled.
- Upgrade installation and application launch succeeded on RG477M with no inspected crash entries.
- Device visual/instrumentation checks, gameplay and performance measurements remain unverified.
- No production signing key configured; third-party artwork redistribution review remains pending.
