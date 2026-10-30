# Artwork and packaged assets

The app's checked-in runtime artwork is stored directly in
[`app/src/main/assets/`](../app/src/main/assets/) and
[`app/src/main/res/drawable-nodpi/`](../app/src/main/res/drawable-nodpi/).
Gradle packages these files as-is; it does not copy from a parallel `artwork/` source tree.
Update the packaged asset and its index/credits together when changing artwork.

Asset attribution and rights notes ship in
[`ARTWORK-CREDITS.txt`](../app/src/main/assets/ARTWORK-CREDITS.txt),
[`CARTRIDGE-LABEL-SOURCES.txt`](../app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt),
and the per-catalog JSON indexes. Credits and public availability do not establish
redistribution permission. See [third-party notices](../THIRD_PARTY_NOTICES.md) and
the [release checklist](RELEASING.md) before publishing assets or APKs.

Review current artwork and catalog contents:

- Arcade fronts: [`arcade_presentations/index.json`](../app/src/main/assets/arcade_presentations/index.json)
- NES covers: [`nes_covers/index.json`](../app/src/main/assets/nes_covers/index.json)
- SNES labels: [`snes_labels/index.json`](../app/src/main/assets/snes_labels/index.json)
- NES/SNES exact-ROM metadata: [`cartridge-artwork/nes-scans.json`](../app/src/main/assets/cartridge-artwork/nes-scans.json)
- Cabinet attribution: [`arcade-metadata/cabinet-art.json`](../app/src/main/assets/arcade-metadata/cabinet-art.json)

Artwork preparation scripts are maintainer tools, not part of the Android build. Some require
original or review inputs that are not included in the cleaned project tree. The committed
runtime assets remain the source of truth for builds.
