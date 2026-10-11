# Reviewed source-specific cabinet photo cutouts

Reviewed on 2026-10-09 against blue/tan contrasting backgrounds. These are real
source-photo pixels, not a generic cabinet with pasted-in title art. Raw photos,
the original source manifest, and existing reviewed references are unchanged.

## Subsequent operator-conversion alternatives

The historical-photo statistics below remain unchanged. A separate
[conversion stage](conversions/README.md) now supplies **131 explicitly imagined
period-style operator conversions**, so the current runtime map covers **383/383
inventory ROMs and all 485 lookup keys**. Every conversion and its aliases have
`kind: "period-style-conversion"`; none is promoted to an authentic historical
cabinet. Existing 250 photo files and 328 photo mappings remain unchanged.
The original gap list still records absent/rejected historical photographs.
Regenerate this photo stage first, then the conversion stage, to retain alternatives.

## Deliverables and coverage

- **250 unique PNG cutouts**, 960 × 768 RGBA, in [../../full-cabinets](../../full-cabinets).
- **252 of 383 inventory ROMs** covered: 217 source-set references and 35 explicitly
  confirmed parent alternatives. This exceeds 250 inventory games, not 250 new downloads.
- **485 lookup keys**, including existing catalogue canonical aliases; 328 have an asset.
- **131 inventory gaps**: 99 had no located source; 32 remain rejected/incomplete.
- 280 manifest-selected photos were evaluated, plus one existing full Double Dragon
  parent photo. 31 distinct photos are rejected; the rejected US Double Dragon photo
  is replaced for `ddragonu` by its manifest-confirmed parent `ddragon`.
- No alternate-source downloads or GameTDB coverage claims were made.

## Integration API

The generated [cabinet-art.json](../../../app/src/main/assets/arcade-metadata/cabinet-art.json)
has exactly the root object `{"mappings": [...]}`. Each entry has `rom`, `asset`,
`kind`, `source`, and `notes`.

- `rom`: an inventory ROM name or existing metadata canonical/alias lookup key.
- `asset`: a PNG basename, such as `ddragon.png`, relative to the full-cabinets
  source folder. It is **not** an Android asset-directory prefix. Packaging is owned
  by the main integration agent.
- `asset: null`: no approved real-photo cutout; preserve the explicit gap rather
  than substituting a fabricated cabinet.
- `kind`: `exact-set-reference`, `confirmed-parent-alternative`,
  `catalogue-canonical-alias`, `missing`, or `rejected`.
- `source`: the source photo URL, or null when no photo was located.
- `notes`: source identity, processing/crop caveats, and parent/alias relation.

No runtime, Gradle, catalogue, or existing normalization-helper changes are needed
to regenerate these files. Main integration must use the map, not assume an asset
has the same name as the game ROM or catalogue canonical ID.

## Generator

[PrepareAllArcadeCabinets.java](../../../scripts/PrepareAllArcadeCabinets.java) uses
JDK 17 and the unchanged normalization helper for fitting only. Compile both Java
sources into a temporary class directory. Entry point:

- `PrepareAllArcadeCabinets [workspace-root]`: regenerate accepted PNGs, mapping,
  provenance, alpha audit, contrasting cutout sheets, and explicit gap lists.
- `PrepareAllArcadeCabinets [workspace-root] --raw-review`: render raw source sheets.
- `PrepareAllArcadeCabinets --self-test`: run segmentation regression fixtures.

[recipes.json](recipes.json) is the explicit reviewed source registry. Continuous
uprights restore original dark interior pixels between visible outer rails.
Ambiguous silhouettes have polygons measured from their own source photos. Open
seating frames, wheels, and cocktail-table leg gaps are not globally row/hull-filled.
Source-specific polygon masks retain dark cockpit interior panels rather than
confusing them with exterior background. Existing deliberate alpha is retained.
White-background edge removal is supported and covered by regression fixtures.

The generator fails if a selected source has no review policy, a parent override
is not confirmed by the manifest, or the normalized alpha fails validation.

## Review and evidence

- [provenance.json](provenance.json): source/output SHA-256, source dimensions,
  segmentation policy, source identity, and per-image alpha audit.
- [gaps.json](gaps.json): all 131 inventory gaps with reasons. The same list is
  emitted at the temporary path `/tmp/full-cabinet-gaps.json` for the integration agent.
- The eight `raw-*` sheets record the initial 280 manifest-selected photos.
  The full parent Double Dragon photo was also inspected separately.
- The eight `cutouts-*` sheets show all final candidates against contrasting
  backgrounds; blank labelled cells are deliberate rejections.

Validated: all output PNGs are 960 × 768 RGBA; all borders are transparent; no fully
opaque photo rectangles pass; every accepted image has only one substantial alpha
component (over 500 pixels); no image has more than five disconnected components.
The mapping has unique keys and no dangling asset references. All current input
and output digests match recorded provenance. Compiler lint and segmentation
regression fixtures pass. Gradle was not run.

## Accuracy limits

“Exact-set-reference” describes the source lookup, **not** a claim that the machine
is factory-original, unmodified, or has exact regional controls. Photos can show
operator conversions, restoration, reflections, and replacement parts. Parent and
catalogue aliases can have different control layouts. Some otherwise usable photos
clip feet or part of the lower/base edge; those limits are retained in per-source
notes. No missing cabinet pixels, texture, titles, or base geometry were invented.
Manual polygons approximate visible source contours at the source resolution.

Severe upper-body-only photos and obstructed/ambiguous candidates were rejected,
even when an older manifest called them build-ready. Rejected source IDs:

1943kai, astorm, batrider, bbros, bombjack, ctribe, ddragonu, ddsom, endurobl,
gaiden, gngt, hooku, mainevt, metamrph, mshvsf, nslasher, punkshot, ramprt2p,
renegade, rocnrope, rtypeleo, s1945, shangonb, shdancer, slammast, slyspy,
sonson, strider, varth, vendetta, willow.

Original copyrights remain with photo/artwork owners and manufacturers; sourcing
and preparation are not a redistribution licence grant.