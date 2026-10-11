# Emulator and game-hub integrations

[User guide](USER_GUIDE.md) · [Development](DEVELOPMENT.md)

## Launch mechanisms

| Path | Use | Limitation |
| --- | --- | --- |
| Android shortcut | Publisher exposes a game shortcut; Android executes its hidden payload. | Needs Home role and a shortcut visible to this launcher. |
| Android activity | Opens an installed app/emulator/hub. | Standalone app launch is not a per-game contract. |
| Custom intent | Emulator documents a game action, URI or extras. | Must match the installed package/build and storage access. |

[CustomIntentTemplates.kt](../app/src/main/java/com/ariy/retrolauncher/launch/CustomIntentTemplates.kt)
provides starting points, not universal compatibility promises. Do not invent game IDs or claim
full-library discovery from a shortcut-only interface. Engines and system files are not bundled.

## ROM folders

Install and configure the selected emulator **before** importing its games. Retro Launcher detects
installed packages and passes launch requests; it does not install or configure the emulator.
First test the game in the emulator, then select the ROM root in welcome or through
**Options → General → Scan library / Setup**. Recognized platform folders are scanned sequentially,
with installed emulator/core selection where required. Missing emulators and user-skipped platforms
are not imported. After installing an emulator or adding ROMs, issue the scan again; repeated scans
are additive and retain existing metadata. Folder scanning is not a continuous watcher.

Android's Storage Access Framework authorizes selected directories. Retro Launcher and emulators
can have different grants. A document URI is not necessarily a filesystem path. Scanning should
use the selected directory rather than assume its authorization root is the desired subfolder.

Arcade imports do not require a cabinet image. Conservative clone/dedup policy preserves distinct
games/ROM identities; shared art is not identity. Switch detection is best-effort metadata/filename
matching, not cryptographic game verification.

## ScummVM

1. Register games and authorize their folders **inside ScummVM**.
2. Use Retro Launcher's **Choose registered game** to open the official picker.
3. Use **Import ScummVM config** with an exported configuration for bulk linking.
4. Scans can link known targets from exported config/available shortcuts; a marker alone does not
   make a private target launchable.

A configured target differs from an engine ID. ScummVM Android treats its scheme-specific URI payload
as one argument; an invented combined `--path` command is not a replacement. The entire private registered
list cannot be enumerated by Retro Launcher. See [verified ScummVM notes](../artwork/SCUMMVM-INTEGRATION.md).

## PC game hubs

### GameNative

Create a game's home-screen shortcut while Retro Launcher is Home, then Allow. Published pins/dynamic
shortcuts use Android's `LauncherApps.startShortcut`. The manual preset needs GameNative's source-specific
`app_id` and `game_source`; Steam may use its store app ID, but other IDs must not be guessed.

Known source metadata can classify Steam/GOG/Epic/Amazon. Android generally redacts shortcut intents/extras,
so opaque shortcuts stay GameNative unless you assign a store yourself. Publishing avoids manual IDs.

### GameHub and GameHub Lite

Verified installed identities are `com.xiaoji.egggame` and `gamehub.lite`. Lite has been observed publishing
per-game shortcuts. Both packages use the same Android shortcut path here; this does not certify every
version or establish a full-library API.

Create a shortcut inside the hub and Allow for automatic addition. Existing shared shortcuts can be queried
via **Add → Shortcuts → Shortcut setup → Refresh**, using the appropriate Import button.

Installing the hub or downloading a game inside it is not enough to expose that game's launch target.
Use the hub's per-game menu to create a **home-screen shortcut** (the wording varies by version),
with Retro Launcher already set as the default Home app. Repeat for each desired game and approve
the Android pin confirmation. Return to Home to refresh accessible shared shortcuts if necessary.
**Scan library / Setup** searches ROM folders; it cannot enumerate these hubs' private game lists
and is not a substitute for publishing shortcuts. You do not need to rescan ROMs after every hub pin.

**Old pins:** a shortcut can exist on the device but belong only to another launcher. Android prevents
Retro Launcher reading it. Some hubs only update an existing pin. Remove the shortcut in the old Home,
return to Retro Launcher as Home, and recreate it. Do not delete the game or reset the hub library.

See [GameNative and hub notes](../artwork/GAMENATIVE-INTEGRATION.md) for facts and limitations.

## Standalone emulators

The root build includes only `:app`; emulator engines and sources are not bundled. Install emulators
separately. RPCSX can still be opened as an installed Android application, but per-game boot depends
on the installed version's public contract. This project no longer supplies a patched or embedded RPCSX build.

## Supported emulator presets

The following names correspond to built-in presets and platform choices, not bundled apps or
verified gameplay coverage for all versions. Package/build variants may have different Android
launch contracts; use a compatible installed build or a manually verified launch target.

| Emulator | Platform choices |
| --- | --- |
| Dolphin / Dolphin MMJR2 | GameCube, Wii |
| Eden / Eden Optimized | Switch (same package/launch contract; the choice does not install a different build) |
| Citra / Azahar | Nintendo 3DS |
| PPSSPP | PSP (preset targets the standard PPSSPP package) |
| DuckStation | PlayStation 1 |
| NetherSX2 | PlayStation 2; preset uses the compatible AetherSX2 package identity |
| Vita3K | PS Vita; manual setup, no supported automatic bulk folder scanner |
| NES.emu / Snes9x EX+ | NES / Super Nintendo |
| Mupen64Plus AE / DraStic | Nintendo 64 / Nintendo DS |
| Redream / Flycast | Dreamcast |
| MD.emu | Genesis/Mega Drive, Master System, Sega CD, Sega 32X choices; installed build/system support still applies |
| PCE.emu / Neo.emu | PC Engine / TurboGrafx-16 / Neo Geo |
| GBC.emu / GBA.emu | Game Boy and Game Boy Color / Game Boy Advance |
| NGP.emu | Neo Geo Pocket and Neo Geo Pocket Color |
| ScummVM | Registered targets, not arbitrary folder-to-game registration |
| RetroArch / RetroArch64 | Multi-system core-based launch; see below |

PC hubs **GameNative, GameHub and GameHub Lite** integrate through Android-published game shortcuts,
not a full-library folder scanner. GameNative also has a documented manual intent preset, but
publishing a shortcut avoids entering or guessing game IDs.

### RetroArch cores

Install the required core **inside RetroArch**, then explicitly choose the matching system/core
in Retro Launcher. Detecting RetroArch's package cannot prove that its private core is installed.
Built-in system choices include:

| System | Core selected by the preset |
| --- | --- |
| MAME Arcade | MAME 2003-Plus |
| Final Burn Neo | FinalBurn Neo |
| DOS | DOSBox Pure (manual-add guidance; no supported automatic bulk folder scanner) |
| NES / SNES | Mesen / Snes9x |
| Sega Saturn | YabaSanshiro |
| Nintendo 64 | Mupen64Plus-Next (GLES3) |
| Nintendo DS | melonDS |
| PlayStation 1 | Beetle PSX HW |
| PSP | PPSSPP |
| Genesis/Mega Drive | Genesis Plus GX |

Platform recommendation mappings also include Genesis Plus GX for Master System/Sega CD,
Gambatte for Game Boy/Game Boy Color, mGBA for Game Boy Advance and Flycast for Dreamcast.
Core availability/architecture and ROM-set compatibility must be checked in the installed
RetroArch build. In particular, arcade ROM sets must match the selected MAME/FinalBurn core;
an installed RetroArch app alone does not guarantee either is ready.

For the exact current launch definitions, see
[presets](../app/src/main/java/com/ariy/retrolauncher/launch/CustomIntentTemplates.kt) and
[platform recommendation mappings](../app/src/main/java/com/ariy/retrolauncher/launch/GameRecommendations.kt).
