# Arcade cabinet preparation

## Authorship clarification

The project author states that all cabinets were AI-generated. Record the generator/output terms and
reference inputs as part of release provenance. Generated illustrations should not be presented as
historically verified photographs.

That statement must be distinguished from the processing pipeline below: the repository also contains
documented Arcade Italia/progetto-SNAPS reference downloads that override legacy cabinet inputs, plus
donor-image conversions. Do not erase their provenance or assume every current packaged image is an
independent AI output. A per-file reconciliation is still needed before public distribution.

## Build preparation

The finite cabinet collection is sourced from `overlay_mame`. The curated ROM/name
mapping is maintained in `ArcadeGameCatalog.kt`. Unlisted clones or sequels are not
automatically treated as the same game.

`prepareArcadeCabinets` runs the Java source generator before the Android build.
Prepared PNGs go to the packaged `cabinets_mame` asset directory:

- 960 × 768 transparent canvas;
- maximum cabinet footprint 760 × 700, centered, with a common bottom baseline;
- aspect ratio preserved;
- only edge-connected near-white/transparent background removed;
- interior whites and black cabinet details preserved;
- progressive bicubic downsampling for large inputs;
- original source files never modified.

This is image preparation, not historical reconstruction or AI upscaling. Source
perspective, controls, marquee, side art and screen content remain unchanged.
Low-resolution sources cannot acquire real detail through resampling.

Owned-game references in `artwork/cabinet-references` now override corresponding
legacy inputs during preparation. They use separately scoped black-background
cutout processing. See that folder's SOURCES.md for provenance and explicit
variant/redistribution caveats. Reference inputs are not bundled as opaque images;
only their processed transparent cabinets are packaged.

## Accuracy limits

The collection mixes photographs, renders, conversion cabinets and modern replicas.
For example, the current Pac-Man source visibly includes Arcade1Up branding and
multiple game logos. It is not an original dedicated Pac-Man cabinet. Do not describe
the generated collection as museum-accurate or all original hardware.

To improve fidelity, replace individual sources with appropriately licensed,
verified references for the intended cabinet variant (upright/cocktail/deluxe,
region, dedicated versus conversion). Preserve original controls and screen
orientation. Do not synthesize an invented cabinet and label it authentic.

The generic `mame.png` fallback is kept separate: it composites downloaded gameplay
art into a screen window at runtime. It is not a game-specific historic cabinet.