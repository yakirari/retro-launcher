# Retro Launcher

An Android Home launcher for retro and PC game libraries, with controller navigation,
platform galleries, nostalgic cartridge/case templates, ROM scanning, and Android game shortcuts.

**Requirements:** Android 13 or newer. Emulators and PC game hubs are installed separately.
Retro Launcher does not include emulator engines, ROMs, BIOS files, firmware, or games.

> **Licensing:** original launcher code and author-created artwork are MIT-licensed. Third-party cabinet
> references retain their source terms. Asset redistribution review is still required before
> publishing this repository or an APK. See [third-party notices](THIRD_PARTY_NOTICES.md) and the
> [release checklist](docs/RELEASING.md). Not every included asset is open source.

## Features

- Two global nostalgic presets: **Grid** and **Shelf**, each remembering its own tile count.
- Default: Grid, 2 tiles and transparent tile backgrounds; appearance stays the same across filters.
- Separate **Android Games** and **Emulators & Launchers** galleries.
- Touch/controller navigation and incremental gallery pages that extend ahead of scrolling.
- Authorized-folder ROM scans, conservative duplicate handling, and editable launch targets.
- Optional first-run setup: choose one ROM root for automatic recognized-platform import and
   later artwork lookup; cancel/retry from Options. Missing emulator/core setup is clearly marked.
- Android-managed game shortcuts from GameNative, GameHub, GameHub Lite and other publishers.
- ScummVM registered-game picker and exported configuration import.
- Cover lookup, custom artwork, global captions and release-year sorting.
- Popularity sorting based on **your successful launch count**, not a global ranking.

Integration depends on each emulator's version and public launch contract. A preset/platform
name is not a guarantee that every game will boot.

## Start here

1. Install an APK from a trusted maintainer-published release **if one is available**, or
   [build a debug APK](docs/DEVELOPMENT.md). Check [GitHub Releases](https://github.com/yakirari/retro-launcher/releases)
   for published artifacts; documentation does not imply a release already exists.
2. Open Retro Launcher and choose it in Android's **default Home app** settings.
3. Choose your ROM root in welcome to import supported platform folders automatically, or skip
   for installed apps/shared hub games. Install/configure emulators separately; repair marked
   unconfigured tiles or retry **Options → Scan library / Setup** afterward.
4. Follow the [user guide](docs/USER_GUIDE.md) and [integration guide](docs/INTEGRATIONS.md).

Keep another Home launcher installed so you can switch back during testing.

## Documentation

| Audience | Guide |
| --- | --- |
| New users | [Installation, libraries, appearance and troubleshooting](docs/USER_GUIDE.md) |
| Emulator/hub users | [ROM, ScummVM and PC shortcut workflows](docs/INTEGRATIONS.md) |
| Developers | [Build, tests, deployment and asset regeneration](docs/DEVELOPMENT.md) |
| Maintainers | [Architecture and data-safety invariants](docs/ARCHITECTURE.md) |
| Contributors | [Contribution workflow](CONTRIBUTING.md) |
| Release maintainers | [Publication and release checklist](docs/RELEASING.md) |
| First preview | [0.1.0 release notes and known limitations](docs/releases/0.1.0.md) |
| Repository administrators | [Hosting, security settings and audit scope](docs/HOSTING.md) |
| Security reporters | [Security reporting policy](SECURITY.md) |
| Asset contributors | [Licensing scope and third-party notices](THIRD_PARTY_NOTICES.md) |

## Status and licensing

Experimental, version 0.1.0. The root Gradle project builds only `:app`; no emulator source tree
or engine is bundled. JVM tests cover policies/parsing/merging/artwork/paging;
real-device UI and game-launch tests remain necessary.

There is currently no dedicated in-app library backup/export feature. Uninstalling or clearing
storage can lose the library and folder permissions.

Original launcher code and documentation: [MIT](LICENSE). Third-party material is excluded from
that grant where separate rights apply; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Do not submit games,
account credentials, BIOS/firmware or artwork without documented distribution rights.

## Project and maintainer

- Repository: https://github.com/yakirari/retro-launcher
- Maintainer: [@yakirari](https://github.com/yakirari)
- Bugs and proposals: [Issues](https://github.com/yakirari/retro-launcher/issues)
- Vulnerabilities: follow [SECURITY.md](SECURITY.md), not public bug reports.
