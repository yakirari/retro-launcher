# System-wide cartridge artwork

Artwork support must be based on a platform catalog, not the games on a developer's device.
Prefer externally available original labels; prepare/cache derived assets only when necessary.
Never replace a missing regional sticker with another game's or region's artwork.

## Current coverage and policy

| Platform | General source | Runtime matching | Prepared/bundled game images |
| --- | --- | --- | --- |
| GB | Slot external index: 1,898 label mappings | Exact full-ROM CRC; raw GB or single-ROM ZIP | Download/cache only; no game stickers bundled |
| GBC | Slot external index: 1,756 label mappings | Exact full-ROM CRC; GBC/dual-compatible GB or ZIP | Download/cache only; no game stickers bundled |
| GBA | Slot external index: 3,404 label mappings | Exact full-ROM CRC; GBA or ZIP | Download/cache only; no game stickers bundled |
| N64 | Broad Analogue 3D thumbnail database; original cartridge scan archive | Exact normalized first-8-KiB CRC; Z64/V64/N64 or ZIP | Reviewed original sticker crops prepared on demand; no game stickers bundled |

Slot counts are **CRC-to-label mappings**, not counts of distinct games or a claim of complete
regional/revision coverage. They were measured from https://art.slot-cfw.fyi/index.json on
2026-10-20. GB/GBC/GBA lookup was generic from its introduction; the device games were test
cases, not a whitelist. Plastic shell PNGs are bundled separately under their own GPL terms.
See [Slot shell documentation](slot/README.md) and
[downloaded media notices](../app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt).

N64 differs: its original seven sharp upgrades were curated for seven verified USA header
signatures. The broad 74×86 fallback is not restricted to these games, but high-resolution
coverage is **not yet broad**. The original scan archive contains **313 cartridge scans**,
including regions and duplicates, not 313 approved unique game labels. Current manifest:
[N64 scan sources](../app/src/main/assets/cartridge-artwork/n64-scans.json).
The app reads this manifest directly, so adding reviewed games does not require editing Kotlin.
Multiple independently verified signatures may share one identical sticker source.

## Reproducible preparation and review

Tools:
- [Preparation driver](../scripts/prepare-cartridge-artwork.mjs): Node 18+.
- [Image processing helper](../scripts/PrepareCartridgeArtwork.java): JDK 17+, standard libraries.

1. Obtain the archive linked by the manifest for local review, respecting the source terms.
   The driver verifies its SHA-1 against the fixed source inventory before processing it.
2. Run the driver with explicit `--archive` and `--output` arguments; output must be outside
   the repository, except the ignored local batch folder described below. `--help` lists syntax.
   No connected device, library export or ROM is used.
3. Inventory mode produces source dimensions/hashes, seven contact sheets, and a draft JSON
   inventory for **all 313 scans**. It does not invent crop coordinates or ROM signatures.
4. Review sticker edges, printing, region/revision and actual source quality. Verify the exact
   header signatures independently. Add only approved metadata to the runtime manifest with
   `reviewed: true`. Unreviewed entries are never selected. No bulk title/fuzzy matching.
5. `--prepare` verifies reviewed image hashes/dimensions and writes ready-made label PNGs
   plus an index containing identities, dimensions, source hashes and output hashes. It
   extracts the original sticker pixels without resizing, sharpening or redrawing text.
6. Review prepared PNGs before adding coverage. A poor source is not automatically an upgrade:
   the noisy Harvest Moon scan and mismatched USA/Japanese Mario variants remain excluded.

The runtime uses the same reviewed metadata and performs the same original-image crop on demand.
The shared manifest is the authoritative recipe; local PNG exports are useful for visual review,
an approved offline pack or a future separately hosted prepared-art catalog. Existing source/crop
changes invalidate the runtime cache automatically. A failed lookup keeps existing fallback art.

## Complete local N64 crop pack

`--batch-crop` prepares **all 313 scans: seven existing crops plus the remaining 306**.
The generated pack is available locally at [local-prepared/index.json](n64/local-prepared/index.json),
with PNGs under its `labels/` directory and seven contact sheets under `crop-review/`.
IDs are the sorted source-archive positions; titles and regional variants are recorded in the index.
These are scans/variants, not a claim of 313 distinct games.

The driver verifies the pinned archive hash, detects sticker boundaries, applies
[manual crop corrections](n64/crop-overrides.tsv), and exports original pixels without resizing,
sharpening, stretching or invented printing. It then independently reopens every PNG and checks
every pixel against its source crop, along with source hashes, dimensions and complete coverage.
All seven contact sheets were visually inspected; ambiguous boundaries were checked against full
cartridge photographs and corrected. Source resolution, moire, fading and damage are explicitly
flagged in the index, not represented as restoration or universal high-resolution quality.

To reproduce, use the preparation driver with `--batch-crop`, the original `--archive`, and
`--output artwork/n64/local-prepared` (or an external directory), with `JAVA_HOME` selecting JDK 17+.
The local pack is ignored by Git and is not an Android build input. Do not distribute it without
separate media-rights approval. The crops are ready as image assets; **306 still need independently
verified exact ROM identities before automatic runtime selection**. The seven existing verified
mappings remain unchanged; no fuzzy regional substitution was added.

## Distribution is separate from preparation

The archive declares no media license. Being publicly downloadable is not permission to bundle
or host its images. No scans or generated game labels are checked into this repository/APK.
Local output is marked `local-review-only-not-redistribution-cleared`. `--distribution` fails
unless every exported entry has both `redistributionApproved: true` and nonempty
`redistributionEvidence` recording a reviewed grant/license. Merely toggling a flag is not a grant.
Distribution output is still outside the repository and is not picked up by Gradle automatically.

For future supported cartridge systems, use this order:
1. Verify a general external original-label source and its identity scheme.
2. Use exact regional identities and bounded downloads/cache; keep manual artwork authoritative.
3. If no adequate external source exists, maintain a platform manifest plus reviewed preparation
   recipes and coverage report, as for N64/arcade assets—not device-specific code branches.
4. Bundle/host ready-made game images only once their redistribution terms are established.

No current claim is made that NES/SNES/Mega Drive or every supported console already has
an authentic cartridge-sticker catalog. Existing box-art fallback is not such a catalog.