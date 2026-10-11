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
via **Add → Shortcuts → Shortcut setup**, using the appropriate Import button.

**Old pins:** a shortcut can exist on the device but belong only to another launcher. Android prevents
Retro Launcher reading it. Some hubs only update an existing pin. Remove the shortcut in the old Home,
return to Retro Launcher as Home, and recreate it. Do not delete the game or reset the hub library.

See [GameNative and hub notes](../artwork/GAMENATIVE-INTEGRATION.md) for facts and limitations.

## Standalone emulators

The root build includes only `:app`; emulator engines and sources are not bundled. Install emulators
separately. RPCSX can still be opened as an installed Android application, but per-game boot depends
on the installed version's public contract. This project no longer supplies a patched or embedded RPCSX build.
