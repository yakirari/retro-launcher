# ScummVM integration

## Android launch contract

ScummVM Android passes the `scummvm:` URI's scheme-specific part as **one** native argument.
The payload must be an existing configuration target (the exact section name in ScummVM's own config),
not a folder path, a string containing multiple command-line arguments, or necessarily an engine game ID.
Do not rewrite target IDs to friendly aliases or replace ScummVM's private configuration.

The exported `ACTION_CREATE_SHORTCUT` activity provides a registered-game picker. Its result contains
`android.intent.extra.shortcut.INTENT` with the exact launch URI and `android.intent.extra.shortcut.NAME`
with the game's description. Retro Launcher uses that URI through ScummVM's exported entry activity.
Recent Android shortcuts are not a complete game inventory.

## One-time setup

1. In Retro Launcher: Add → Games → ScummVM → **Open ScummVM**.
2. In ScummVM, authorize the game folder using **Add a new folder**. Retro Launcher's SAF permission
   does not grant ScummVM its own persistent permission.
3. Add games inside ScummVM. Hold **Add Game** for its Mass Add workflow when available.
4. Return to Retro Launcher and use **Choose registered game**.
5. For bulk import, export the existing `scummvm.ini` through ScummVM's LAN server, then use
   **Import ScummVM config**. Do not use a config generated for a different device or obsolete paths.
   For folder Scan, place that export in the authorized ScummVM ROM folder; entries match exact folder names.

Folder scanning reads marker contents before filenames, handles known aliases conservatively, and lists
the selected SAF document rather than its authorization tree's root. A marker identifies data; it does
not register a game. Unregistered folders are skipped with setup guidance, rather than presented as playable.
ScummVM itself still verifies actual game data and compatibility. Incomplete files cannot be repaired by
a launch intent (the inspected `dig` folder, for example, contained only `DIG.LA0`).

## Titles and artwork

Known short IDs are expanded to player-facing titles without changing custom names. Legacy key-based
autofill artwork is invalidated when correcting a known title; manually chosen artwork is retained.
Cover lookup uses exact Libretro ScummVM title matches, tolerating its platform/edition suffixes and
trailing `, The` convention. It preserves subtitle colons and does not guess generic web artwork.

## Verified references (2026-10-10)

- [ScummVM Android documentation](https://docs.scummvm.org/en/latest/other_platforms/android.html)
- [Android activity argument handling](https://github.com/scummvm/scummvm/blob/master/backends/platform/android/org/scummvm/scummvm/ScummVMActivity.java)
- [Registered-game picker](https://github.com/scummvm/scummvm/blob/master/backends/platform/android/org/scummvm/scummvm/ShortcutCreatorActivity.java)
- [Shortcut result compatibility](https://github.com/scummvm/scummvm/blob/master/backends/platform/android/org/scummvm/scummvm/CompatHelpers.java)
- [Libretro ScummVM cover index](https://thumbnails.libretro.com/ScummVM/Named_Boxarts/)

Device inspected: RG477M, ScummVM 2026.3.0. Published shortcuts only listed two demos; they are not evidence
that the inspected ROM folders are registered. The private config is inaccessible to Retro Launcher.
Device lockscreen prevented end-to-end picker/gameplay validation during implementation.