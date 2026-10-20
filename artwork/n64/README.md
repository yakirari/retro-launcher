# Nintendo 64 label preparation

- [crop-overrides.tsv](crop-overrides.tsv): manually checked source sticker rectangles, including
  the original seven verified runtime crops and corrections to batch-detected boundaries.
- Local generated pack: `local-prepared/` (ignored by Git, never bundled automatically).
  It contains 313 original-resolution PNGs, seven review sheets, crop metadata, hashes and
  quality flags. The additional 306 scans are prepared images, not verified new ROM mappings.
- Runtime source metadata: [n64-scans.json](../../app/src/main/assets/cartridge-artwork/n64-scans.json).
- Preparation: [Node driver](../../scripts/prepare-cartridge-artwork.mjs) and
  [Java image helper](../../scripts/PrepareCartridgeArtwork.java).

See [the cartridge artwork workflow](../CARTRIDGE-ARTWORK.md) for reproduction, exact identity
matching, pixel verification, quality limitations and the separate redistribution approval gate.