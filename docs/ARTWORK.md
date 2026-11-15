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

Platform-specific game artwork is organized under
[`app/src/main/assets/artwork/`](../app/src/main/assets/artwork/):

- Arcade fronts and cabinet attribution: [`arcade/`](../app/src/main/assets/artwork/arcade/)
- NES box covers and cartridge-scan metadata: [`nes/`](../app/src/main/assets/artwork/nes/)
- SNES labels: [`snes/labels/`](../app/src/main/assets/artwork/snes/labels/)
- GB, GBC and GBA labels and CRC indexes: [`gb/`](../app/src/main/assets/artwork/gb/),
  [`gbc/`](../app/src/main/assets/artwork/gbc/) and
  [`gba/`](../app/src/main/assets/artwork/gba/)
- N64 cartridge-scan metadata: [`n64/scans.json`](../app/src/main/assets/artwork/n64/scans.json)

Artwork preparation scripts are maintainer tools, not part of the Android build. Some require
original or review inputs that are not included in the cleaned project tree. The committed
runtime assets remain the source of truth for builds.
