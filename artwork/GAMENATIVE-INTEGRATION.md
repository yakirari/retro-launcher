# GameNative discovery

Verified against installed GameNative 1.2.1 and its upstream v1.2.1 sources on 2026-10-10.

GameNative exposes direct game launch via `app.gamenative.LAUNCH_GAME` with integer `app_id`
and `game_source` (`STEAM`, `GOG`, `EPIC`, `AMAZON`, `CUSTOM_GAME`). IDs are source-specific:
the observed Steam Tomb Raider shortcut uses Steam app ID 203160; do not assume all IDs are
distinct from store IDs. Android strips intents/extras from LauncherApps query results; the scanner
imports opaque game shortcuts and uses LauncherApps.startShortcut rather than guessing launch IDs or stores.

Its manifest does not expose a game-library provider; the FileProvider is not exported and is not
a database query interface. The external app folder only contained logs/control data on the inspected
device. Retro Launcher does not read GameNative's private database or account credentials.

## Using Scan

1. Set Retro Launcher as the default Home app.
2. Create a home-screen game shortcut inside GameNative, and allow Retro Launcher's pin confirmation.
3. Allowing a valid pin request now adds the published shortcut directly. Existing shared shortcuts can
	also be selected in Add → Shortcuts or scanned through Add → Games → GameNative → Scan GameNative.

## Digital stores and other hubs

Steam, GOG, Epic Games and Amazon Games have separate top badges. Known GameNative `game_source`
values classify games into those stores; redacted shortcuts stay under GameNative, never an inferred
store based on a numeric ID. A manually selected store is preserved when the payload is opaque.
GameHub and GameHub Lite shortcuts are grouped by their actual installed app label, not guessed
package names or undocumented launch actions. These categories and generic PC default to
Gallerystalgic with an original transparent digital-game card; saved appearance overrides remain intact.

Add → Shortcuts includes Home setup and buttons for installed GameNative/GameHub/GameHub Lite apps.
Retro Launcher accepts user-approved, Android-validated shortcut pin requests from any app and imports
the matching published shortcut by package, ID and user profile. Android owns the hidden launch payload.
For an unknown store, use Edit → Platform to select the correct store yourself.

Device verification 2026-10-10: GameHub Lite is `gamehub.lite`; GameHub is `com.xiaoji.egggame`.
GameHub Lite has published pinned shortcuts for The Cave and UFO 50, targeting its GameDetailActivity
with autoStartGame enabled. Android executes this opaque payload; Retro Launcher does not need to
reconstruct the hub's internal IDs. GameHub has no published shortcuts on the inspected device yet.
Add → Shortcuts now provides Import GameHub / Import GameHub Lite buttons and opens the installed apps.
Create a per-game home-screen shortcut inside the hub while Retro Launcher is Home, then Allow.
Shortcuts pinned only to another launcher need to be republished. Full private libraries remain inaccessible.

Digital artwork now prioritizes exact-title Steam portrait covers (600×900) over landscape headers,
other catalogues and web images, preserves subtitle colons, and uses a 2:3 artwork-selection ratio.
Existing generated autofill artwork and pixel-verified imported shortcut icons can be upgraded to an
exact portrait; unsuccessful lookups retain the old image, and manual replacements are not upgraded.
The digital frame has no lettering; Gallerystalgic remains the default.

Scan queries GameNative's enabled dynamic, cached, and launcher-owned pinned shortcuts, validates
their publisher/game-ID naming (and action/payload when available), and imports current-profile shortcut tiles.
Android preserves the hidden store/launch payload. Scan skips already-added shortcut IDs (and matching
manual launch identities when extras are available), and fetches
artwork using existing PC artwork lookup. It does not launch games while scanning.

Android does not let an ordinary Home app enumerate shortcuts pinned *only* by another launcher.
The observed Tomb Raider shortcut was pinned by a different launcher; it must be republished to
Retro Launcher if it is not returned by the scan. `FLAG_MATCH_PINNED_BY_ANY_LAUNCHER` is restricted
to the selected assistant/system permission and is not a workaround for a regular launcher.

This is a **shortcut scan**, not a full installed-library scan. GameNative would need a public
library export/provider/API for complete automatic discovery of unshared games.

Sources:
- [GameNative 1.2.1 manifest](https://github.com/utkarshdalal/GameNative/blob/v1.2.1/app/src/main/AndroidManifest.xml)
- [Shortcut publishing](https://github.com/utkarshdalal/GameNative/blob/v1.2.1/app/src/main/java/app/gamenative/utils/ShortcutUtils.kt)
- [Android shortcut query access](https://developer.android.com/reference/android/content/pm/LauncherApps.ShortcutQuery)

Device gameplay and scan UI remain unverified while its lockscreen is active.