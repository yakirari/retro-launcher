# Artwork directory guide

Project artwork lives here. Keep original/reference material, prepared build inputs, local
exports and packaged Android copies distinct. Moving a file does not change its license.

## Directory map

| Directory | Role | Build behavior |
| --- | --- | --- |
| [overlays/](overlays/) | Console/cartridge/case templates; original and derived PNGs | Exact filenames mapped to Android drawable resources |
| [overlays/handheld/](overlays/handheld/) | Prepared GB/GBC/GBA shell overlays | Regenerated from Slot sources, then copied to drawables |
| [arcade-overlays/](arcade-overlays/) | Original exported arcade overlay inputs | Copied as legacy overlays; also normalized into cabinet assets |
| [backgrounds/](backgrounds/) | Launcher background images | Copied into Android assets |
| [cabinet-references/](cabinet-references/) | Cabinet source references, provenance, review images and manifests | Only top-level reviewed PNGs feed cabinet normalization |
| [full-cabinets/](full-cabinets/) | Prepared full-cabinet silhouettes/conversions | Top-level PNGs copied into Android assets |
| [slot/](slot/README.md) | Attributed upstream shell originals, license and adaptation notes | Neutral source PNGs feed shell generation |
| [n64/](n64/README.md) | Reproducible N64 sticker-crop corrections and local exports | Not automatically copied into the APK |

The former root directories `overlay`, `overlay_mame` and `backgrounds` now live at
`artwork/overlays`, `artwork/arcade-overlays` and `artwork/backgrounds`. No image names or
Android runtime asset paths were changed. Do not recreate the old root copies.

## Sources → preparation → Android

- Build recipes and explicit overlay filename mappings: [app/build.gradle.kts](../app/build.gradle.kts).
- Preparation and verification programs stay together in [scripts/](../scripts/); the
  [development guide](../docs/DEVELOPMENT.md#assets) documents build tasks and manual regeneration.
- Android destinations remain `app/src/main/assets/{overlay_mame,cabinets_mame,full_cabinets,backgrounds}`
  and `app/src/main/res/drawable-nodpi/nostalgic_*`. Edit upstream artwork, not synchronized copies.
- Keep originals required by generators, even if the app uses a derived template. Do not remove
  a similarly named image as a duplicate without checking the build recipe.
- In-app vectors/icons and runtime metadata stay in Android resources/assets. Recorded demos
  and screenshots stay in [vids/](../vids/README.md), separate from launcher artwork inputs.

## Guides by subject

- [Arcade cabinets, matching and framing](ARCADE-CABINETS.md)
- [Handheld shells and real label sources](HANDHELD-CARTRIDGES.md)
- [Console cover templates](CONSOLE-COVERS.md)
- [System-wide cartridge catalogs and local N64 pack](CARTRIDGE-ARTWORK.md)
- [ScummVM artwork integration](SCUMMVM-INTEGRATION.md)
- [GameNative integration](GAMENATIVE-INTEGRATION.md)
- [Cabinet reference provenance](cabinet-references/SOURCES.md)
- [License scope and third-party notices](../THIRD_PARTY_NOTICES.md)

## Local-only and external media

N64 prepared game labels live in the ignored `n64/local-prepared/` directory; crop recipes
are retained separately so they can be reproduced. They are not redistribution-cleared or
Android build inputs. GB/GBC/GBA labels and N64 runtime scans remain external downloads/cache.
Keep editable GIMP projects outside version control; exported PNGs are the build inputs.
Never infer redistribution permission from public availability, a directory name or this index.