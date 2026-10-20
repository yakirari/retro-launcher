# Slot cartridge artwork

Actual neutral cartridge shell artwork from [Slot](https://github.com/BrandonKowalski/slot), pinned
at **f4b7bc3b1f7240c00d245c82fe123181a2d5c590** (2026-10-19).
Slot credits [Markus / MarkusMaribu](https://github.com/MarkusMaribu) for its skeuomorphic artwork.
See the [upstream credit](https://github.com/BrandonKowalski/slot/blob/f4b7bc3b1f7240c00d245c82fe123181a2d5c590/README.md).

## License and distribution

Upstream declares **GPL-3.0-or-later** in Cargo.toml and ships the GNU GPL v3 root license.
No separate permissive license was found for these cartridge images. Retain that license for the
originals and modified graphics; do not treat these assets as original MIT artwork.
The [full license](../../app/src/main/assets/licenses/Slot-GPL-3.0.txt) and
[credits/modification notice](../../app/src/main/assets/SLOT-ARTWORK-CREDITS.txt) ship in the APK.
Upstream's neutral PNGs are retained unmodified under [source](source/).
Before public distribution, provide the exact corresponding source assets/preparation code and
review the combined distribution's GPL obligations; attribution alone is not compliance. The root
MIT license continues to describe original contributions only and does not override these terms.
Nintendo branding remains third-party property; no endorsement is implied.

## Source inventory

Source URL prefix:
https://raw.githubusercontent.com/BrandonKowalski/slot/f4b7bc3b1f7240c00d245c82fe123181a2d5c590/assets/

| Original | Dimensions | SHA-256 |
| --- | --- | --- |
| gb_neutral.png | 312 × 337 | 0771e3ac10a0d89c74609ce0081d1af0fb76d4b314ccff17d8306661e32487a2 |
| gbc_neutral.png | 312 × 337 | 5244d369f029132db74f28fd51f65bbfdebbf62cd869b36e9449177cdf4ad9cf |
| gba_neutral.png | 360 × 212 | 9cb83dc6f5c9529cb7582e869f9afcdb546d5ea4b5bf667cbd271fc05beae8a3 |

## Preparation

[PrepareSlotHandheldShells.java](../../scripts/PrepareSlotHandheldShells.java) verifies these hashes
and dimensions, then opens only each blank label well. It verifies that all pixels outside the well
are unchanged. No resizing, replacement lettering or procedural redrawing is performed.
GB label: (44,95) 223×199; GBC: (45,110) 223×199; GBA: (48,50) 264×136.
Prepared PNGs go to [the handheld overlay directory](../overlays/handheld/) and are copied into nostalgic_shell_* Android resources.
Gradle runs preparation before syncing resources. Runtime tinting affects the plastic only.

This import replaces the earlier original procedural cartridge shells, not the independent cover,
label picker, or palette. Slot's real-label images are not bundled: the separate runtime lookup
uses its hosted system-wide CRC index for GB/GBC/GBA and caches matching stickers on demand.
This is not restricted to games in a test library. PCB/clear finishes are not imported.
See [system-wide cartridge artwork policy and preparation](../CARTRIDGE-ARTWORK.md).