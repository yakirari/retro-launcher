# Development setup

[Architecture](ARCHITECTURE.md) · [Contributing](../CONTRIBUTING.md) · [Releasing](RELEASING.md)

## Toolchain

| Component | Configuration |
| --- | --- |
| JDK | 17 |
| Gradle wrapper | 8.11.1 |
| Android Gradle Plugin | 8.9.1 |
| Kotlin / Compose compiler plugin | 2.1.20 |
| Android compile SDK | 36 |
| Minimum / target SDK | 33 / 35 |
| Compose BOM | 2024.06.00 |
| Root modules | `:app` only |

Install Android Studio or Android SDK command-line tools, platform 36, platform-tools, and build-tools
requested by AGP. Use the checked-in wrapper. No emulator/native-engine toolchain is required.
Initial sync requires access to Google/Maven/Gradle servers.

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to the SDK, or configure Studio's Gradle JDK and SDK location.
A local `sdk.dir` in local.properties also works; never commit that machine-specific file.

## Build and test

From the root:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Windows uses `gradlew.bat` with the same arguments. Output: `app/build/outputs/apk/debug/app-debug.apk`.
Do not commit build outputs. JVM tests live in [app/src/test](../app/src/test/); prefer pure Kotlin
policy/parser regression tests. Instrumented tests live in [app/src/androidTest](../app/src/androidTest/).
On an unlocked Android 13+ device/emulator:

```sh
./gradlew :app:connectedDebugAndroidTest --console=plain
```

Run lint separately:

```sh
./gradlew :app:lintDebug --console=plain
```

Lint has previously failed with a Kotlin metadata-reader tooling crash. Record the actual failure,
fix/upgrade compatible tooling, and do not silently disable it or report it as passed. JVM success
does not certify UI tests or gameplay. Locked devices can stall instrumented tests.

Gallery settings regression tests are in
[GlobalGallerySettingsTest.kt](../app/src/test/java/com/ariy/retrolauncher/model/GlobalGallerySettingsTest.kt).
They cover first-use migration, validation, independent Grid/Stacked/Shelf sizes, persistence and ignored
legacy platform profiles. On hardware, verify that platform/collection navigation leaves layout,
tile background and captions unchanged, switching presets restores each size, and Options → Artwork
still refreshes only the selected platform. The existing per-layout numeric preference keys remain
compatible: 1 maps to Large, 2/3 to Medium and 4/8 to Small; new choices save 1/2/4 respectively.
`GallerySizingTest` verifies pixel-rounded row heights and gaps; `ShelfPresentationTest` verifies
complete items at carousel ends. On hardware, check Small/Medium/Large fit at least 3/2/1 complete
rows in Grid/Stacked and complete horizontal items in Shelf. Large must also show at least two
tiles across in Grid/Stacked, including on tall/narrow viewports. Columns derive from the remaining
viewport and template proportions, never from the legacy ViewModel count flow.

## Deploy

```sh
adb devices -l
adb -s YOUR_DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s YOUR_DEVICE_SERIAL shell am start -n com.ariy.retrolauncher/.MainActivity
```

Use your own serial and authorize USB debugging on-device. On Linux, fix udev/group setup if ADB
reports `no permissions`; do not embed privileged commands or passwords in scripts. Updating requires
the same signing key; uninstalling to switch keys can delete library data/grants.

Optional bash/Linux helper:

```sh
scripts/deploy-on-change.sh --variant debug --serial YOUR_DEVICE_SERIAL --once
```

Omit `--once` to watch. The script supports `ADB_BIN`, `ADB_SERIAL`, `JAVA_HOME`, `BUILD_VARIANT`,
streaming and fast-deploy options. It can select release when signing properties exist; choose debug
explicitly for local work. It is not a cross-platform CI runner.

## Assets

The app packages its checked-in artwork directly from `app/src/main/assets/` and
`app/src/main/res/drawable-nodpi/`. Gradle does not regenerate or synchronize artwork from a
separate source directory. Update the runtime files and their catalog indexes/credits together.
See the [artwork guide](ARTWORK.md) and [third-party notices](../THIRD_PARTY_NOTICES.md).

The digital PC frame can be regenerated with JDK 17:

```sh
java -Djava.awt.headless=true scripts/PrepareDigitalGameTemplate.java app/src/main/res/drawable-nodpi/nostalgic_overlay_digital_pc.png
```

Other artwork scripts may need original inputs that are no longer retained. They are not invoked
by ordinary builds; verify their inputs and rights before using or publishing generated output.

## Dependency compatibility

[Root build configuration](../build.gradle.kts) pins AndroidX Core; the app currently uses a metadata
compatibility compiler flag. Do not remove constraints without compiling/testing the resolved dependency graph.
[settings.gradle.kts](../settings.gradle.kts) is authoritative: only `:app` is included. Emulator sources
are not part of this repository; see [integrations](INTEGRATIONS.md#standalone-emulators).
