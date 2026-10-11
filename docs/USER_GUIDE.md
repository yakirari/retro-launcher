# User guide

[Project overview](../README.md) · [Integrations](INTEGRATIONS.md)

## Installation and Home setup

You need Android 13+. Install a trusted APK, allowing installation from that source if Android
asks, and open Retro Launcher. Select it under **Settings → Apps → Default apps → Home app**;
setting names vary by device. No root access is required.

The launcher can be opened for testing without becoming Home, but shortcut enumeration/launching
depend on the Home role. Keep a working alternative launcher installed. To leave Retro Launcher,
change the default Home app in Android settings.

## First run and automatic library setup

On a new installation, the adaptive welcome screen offers **Choose ROM folder** and
**Skip / I have no ROMs**. Installed known emulators and hubs appear without waiting for game
shortcut enumeration or artwork downloads. Existing installations are not forced through welcome;
an explicit completion flag is independent of whether the library is empty.

Choose the root containing your recognized platform folders (or a single supported platform folder)
once through Android's folder picker. After persistent read permission succeeds, a ViewModel-owned
background scan visits supported scanners sequentially. It scans the **exact selected root**, not a
broader normalized permission ancestor. Games are committed in small batches; custom titles, covers,
collections, play history and alternate targets are retained. Repeated scans are additive, not resets.

**Options → General → Scan library / Setup** shows progress, discovered/imported/skipped counts,
unconfigured tiles and platform errors. Cancel keeps committed games; retry is safe. Closing the
screen or rotating does not cancel the scan. A process restart stops the scan; retry from Options.
Upgraded installations must select the intended scan root once rather than silently scan their
previously normalized authorization ancestor.

Automatic emulator selection uses a matching installed **standalone preset**, preferring an existing
configured platform entry's launch package when available; ties use package-name/key order. It does
not guess RetroArch core availability in private storage. Without a verified standalone preset,
supported ROMs still become **Needs emulator setup** tiles and cannot launch a generic intent.
Use the tile menu → **Edit** to select one of the installed system-specific presets, then **Save**.
For RetroArch, explicitly select the displayed system/core and install that core inside RetroArch.
Installing a supported standalone emulator and rescanning also repairs these entries.

ScummVM imports config-linked folder targets only. Unlinked folders are reported, not registered
silently: register/authorize them inside ScummVM and use **Add → Games → ScummVM → Choose registered
game**, or import an exported configuration. Private registered lists are not generally enumerable.

Artwork autofill starts after the import batch, uses the existing bounded background queue, and
preserves custom artwork. Offline lookup failures do not remove imported tiles or shortcut icons.
Retry missing covers when online; the Artwork tab's explicit refresh can replace existing covers.
Discovery covers supported formats/recognized folders, not every possible emulator format or layout.
Recognized DOS/Vita folders without a supported bulk scanner are reported with manual-add guidance.
Emulators may still need separate file grants, BIOS files and internal configuration.

## Add emulated games

1. Install an emulator and confirm the game runs inside it first.
2. Open **Add → Games**, choose a platform and the appropriate installed emulator.
3. Select/authorize a ROM directory through Android's folder picker. Use lawfully obtained
   files in a directory you can access, not another app's private database directory.
4. Run the available scan or add an individual supported ROM/launch target.
5. Test the tile. Scanning does not prove the emulator has its own file access, BIOS or settings.

Scanning varies by platform. If no scan is available, use a published shortcut or manual intent
with a verified contract. Both launcher and emulator may need separate access to the same files.

## Android games and standalone launchers

Use **Add → Android** to choose an installed app/activity. Known emulator/hub packages are
also added automatically when installed targets refresh.

- **Android Games:** ordinary added Android apps/games. The picker cannot prove every unknown app
  is a game; choose only what you want in this gallery.
- **Emulators & Launchers:** known emulator apps and GameNative/GameHub/GameHub Lite.
- A hub's individual game shortcut is a game entry, not a standalone launcher entry.

Standalone Android activities are hidden in the mixed **All gallery**, but remain in their own
galleries. The All grid may still contain them.

## PC game hubs

With Retro Launcher as Home, create a game's **home-screen shortcut inside the hub** and tap
**Allow** in Retro Launcher's confirmation. It is added automatically; manual “Add your own game”
tiles are hidden for hub/store categories.

Existing accessible game shortcuts from verified GameNative/GameHub/GameHub Lite packages are
also imported automatically when installed targets refresh, including on return to Home or after a
Home-role change. Shortcut identity includes package, shortcut ID and Android profile; identical
titles are not collapsed across profiles. Unrelated Android shortcuts are never auto-imported.

Other shared shortcuts can be selected through **Add → Shortcuts**. **Shortcut setup** expands
the controls; **Refresh** re-queries Android. Hub Import buttons only see shortcuts Android shares
with this launcher, not the hub's full private library.

Pins belonging **only to another Home launcher** are inaccessible. Some hubs update an old pin
instead of requesting a new one. Remove that shortcut in the old launcher, return to Retro Launcher
as Home, then create it again. Do not uninstall the game. See [integration troubleshooting](INTEGRATIONS.md#pc-game-hubs).

## Navigation and appearance

- Tap a game to launch it; use edit mode to edit its tile.
- In galleries, controller left/right moves between games, up/down changes filters, and the
  activation button launches the focused game. Device/controller mappings can vary.
- Large galleries extend automatically in 48-item pages; no Next-page button is needed.
- **Options → General** offers two global presets: **Grid** (static Gridstalgic layout) and
  **Shelf** (Shelfstalgic layout). Each independently remembers a tile count of **1, 2, 3, 4 or 8**.
- The default is **Grid, 2 tiles, transparent tile backgrounds**. On first use of these simplified
  options, old layout/count/background settings are reset and platform display profiles are ignored.
  Existing library records, metadata, artwork, launcher wallpaper and the global caption choice are retained.
- Layout, tile backgrounds and captions apply everywhere, including platform views and mixed collections;
  changing filters does not change appearance. Switching Grid/Shelf restores that preset's tile count.
- **Options → Artwork** refreshes images for a selected platform without editing gallery settings or
  rescanning ROMs. Existing covers are replaced only when a new image is found, after confirmation.
- Sorting offers alphabetical, release year and personal popularity. Played games group first;
  unknown years are last within their group. Popularity counts successful launches on this library.

## Artwork and editing

Automatic lookup sends game-title searches to third-party services and needs network access.
It is not required to launch configured games. A lookup may fail or return the wrong edition;
edit the artwork to select a suggestion or your own image.

Digital games prefer exact-title portrait covers. Hidden shortcut payloads are not guessed from
numeric IDs; unknown stores stay under their hub. Select a known store yourself in the platform editor.
The PS1 template displays the full cover in a jewel case without a second printed PlayStation banner.
Display processing does not rewrite original files.

Removing a tile is a library operation, not a request to delete its ROM or uninstall its application.

## Data and privacy

### Reset launcher

Open **Options → Reset launcher** in the global destructive section. Nothing is cleared until
you explicitly confirm **Reset launcher** in the warning dialog. **Cancel** is the initial
keyboard/controller choice; Back or tapping outside the confirmation cancels without resetting.

Reset permanently removes the library, play history, custom titles/collections/launch targets,
imported/downloaded artwork and app-private cache, all launcher settings and saved ROM scan root.
It releases this launcher's saved file/folder permissions, including ROM, cover and background
access. You must choose/authorize your ROM folder again. Shared ROMs, shared artwork/background
files, installed applications and other apps' permissions are **not deleted**. Android's default
Home selection is unchanged. There is **no undo or in-app backup/export**; custom metadata and
history cannot be recovered by simply rescanning.

Running scans, refreshes and artwork work are cancelled and drained before private-file cleanup;
a blocked provider/network request can delay reset. Welcome returns in place, without an app
restart, with **Grid, 2 tiles, transparent backgrounds**, default captions and no custom wallpaper,
filter or unlocked edit state. Installed emulator/hub **app tiles** may reappear through discovery;
shared hub **game shortcuts** are not silently restored during welcome. Completing/skipping setup,
then refreshing/importing, can add accessible shared games again. ROM import requires a newly
chosen folder. Reset does not remove Android's shared/pinned shortcuts from their source apps.

If a preference write, grant release or private-file deletion fails, **Reset incomplete** lists
the failures instead of claiming success. Some saved data/access may remain; acknowledge the
message and retry from Options. ROMs and installed apps remain untouched even on failure.

Library records/settings are in app-private local storage; downloaded covers are local files.
Clearing storage or uninstalling can remove them and persisted folder grants. Android backup depends
on device policy and is not a reliable substitute for an explicit export. No dedicated in-app library
backup/export feature currently exists.

Retro Launcher does not need Steam/GOG/hub credentials or read those applications' private game
databases. Never put passwords/tokens into intent extras or bug reports.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Missing shortcuts | Default Home role, Refresh, and pins owned by another launcher. |
| Import says already added | Check the hub/store gallery; the picker hides existing entries. |
| Emulator opens but game doesn't | Verify in-emulator launch, permissions and version-specific contract. |
| ScummVM folders won't launch | Register games inside ScummVM, then use its registered picker or exported config. |
| SD card games disappear | Reinsert and reauthorize the correct directory; saved URIs can become invalid. |
| Wrong cover | Check network access; choose a manual image or another suggestion. |
| Tile count changes when switching Grid/Shelf | Each preset remembers its own global count; set both in Options → General. |
| APK won't update | Signing keys may differ. Understand data-loss risk before uninstalling. |

Reports should include app/device/Android/emulator versions, steps and expected/actual behavior,
with redacted screenshots when useful. Never attach games, account information or unredacted logs.
