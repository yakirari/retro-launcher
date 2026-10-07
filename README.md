# Retro Launcher

Android home launcher for retro games, targeting ARM devices on Android 13+.

## What is implemented

- Home launcher activity with `HOME` and `DEFAULT` categories.
- Scrollable game grid with fixed density modes: `1`, `4`, or `8` columns.
- Persisted game tiles.
- Add tiles from launcher-visible shortcuts when this app is the active Home launcher.
- Add tiles from normal launcher activities.
- Add custom emulator/game intents for systems that do not expose shortcuts.
- Edit title/subtitle.
- Pick a custom image for each tile from device storage.
- Remove tiles.

## Android shortcut constraint

This app can enumerate and launch shortcuts from other apps only when Android treats it as the active launcher. That is an Android platform rule, not a project limitation.

It does **not** read arbitrary pinned shortcuts created by some other launcher app. Instead, it uses `LauncherApps` as a real launcher would.

## Emulator strategy

There are two integration paths:

1. If Daijishō, Dolphin, or another emulator/frontend exposes launcher shortcuts, import those from the `Shortcuts` tab.
2. If a game is visible inside an emulator but no launcher shortcut exists, create a `Custom` tile and enter the package, optional activity class, action, data URI, MIME type, categories, and extras JSON for the emulator’s launch intent.

The repo includes template seeds for:

- Dolphin
- Daijishō
- Blank custom intent
- Explicit emulator activity

`RPCSX` now has a preset seed (`net.rpcsx/.MainActivity`) with both `path` and `game` extras. If your installed build expects a different contract, edit the tile's intent fields.

## RPCSX Direct Boot Integration

Stock RPCSX Android builds may open the app but not auto-boot a game from third-party launchers.

This workspace now includes a vendored RPCSX source tree at:

- `vendor/rpcsx-ui-android`

Patched behavior in the vendored copy:

- `MainActivity` now accepts launch payloads (`path`, `game`, or `data`) and forwards them to `RPCSXActivity`.
- `MainActivity` also handles `onNewIntent` so repeated launches can boot another title.
- Added `rpcsx.intent.action.Emulator` intent filter on `MainActivity`.

Patched files:

- `vendor/rpcsx-ui-android/app/src/main/java/net/rpcsx/MainActivity.kt`
- `vendor/rpcsx-ui-android/app/src/main/AndroidManifest.xml`

To use this flow on-device, install the patched RPCSX app build from `vendor/rpcsx-ui-android`, then re-add the RPCSX tile in Retro Launcher.

## Build

This workspace did not include a JDK, Gradle, or Android SDK, so no APK was produced here.

To build locally in Android Studio:

1. Open this folder as a Gradle project.
2. Install Android SDK 35 and JDK 17.
3. Sync the project.
4. Build `app` for a release or debug APK.

## Auto deploy on change

For local iteration against the current ADB target, run:

```bash
scripts/deploy-on-change.sh
```

What it does:

- Watches `app/src`, root/module Gradle files, and the embedded RPCSX sources under `vendor/rpcsx-ui-android/app/src/main`.
- Watches the top-level source art folders `backgrounds/`, `overlay/`, and `overlay_mame/`.
- Builds `assembleRelease` by default with the normal warm Gradle cache under `~/.gradle`.
- Installs the matching APK to `ADB_SERIAL`, or auto-picks a single connected device.
- Prefers a single USB ADB device automatically when both USB and TCP/IP devices are visible.

Useful overrides:

- `scripts/deploy-on-change.sh --once` runs one build/install cycle and exits.
- `scripts/deploy-on-change.sh --serial 192.168.68.119:41561` overrides the target device.
- `ADB_SERIAL=192.168.68.119:41561 scripts/deploy-on-change.sh` sets the default device via environment.
- `BUILD_VARIANT=debug scripts/deploy-on-change.sh --once` forces a debug deploy instead.
- `scripts/deploy-on-change.sh --fastdeploy --variant debug --once` enables adb fast deploy for quicker USB debug installs.
- `scripts/deploy-on-change.sh --no-streaming --once` forces push-then-install mode, which can be more reliable on flaky wireless ADB links.
- If you switch signing keys and get `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall `com.ariy.retrolauncher` on the device before redeploying.

## Overlay source art

The top-level folders are the editable source of truth for launcher overlays:

- `overlay_mame/` for MAME cabinet art, synced into `app/src/main/assets/overlay_mame`
- `overlay/` for nostalgic console/device overlays, synced into `app/src/main/res/drawable-nodpi`

Those sync automatically during app builds via `preBuild`, so changing `overlay_mame/mame.png` or a file under `overlay/` now gets picked up without a separate manual copy step.

## Platform badge SVGs

Platform badge SVGs now live in `app/src/main/res/raw`.

To add a new one:

1. Drop the SVG file into `app/src/main/res/raw` without editing the SVG contents.
2. Use an Android resource-safe filename: lowercase letters, numbers, and underscores only.
3. Link the platform name to `R.raw.your_file_name` in `app/src/main/java/com/ariy/retrolauncher/model/PlatformSvgIcons.kt`.

## Likely next work

- Replace manual custom intent entry with emulator-specific ROM pickers.
- Add icon extraction for imported shortcuts and activities.
- Add backup/export of launcher configuration.
- Validate actual Dolphin / RPCSX per-game launch contracts on-device and keep improving one-tap integrations.
