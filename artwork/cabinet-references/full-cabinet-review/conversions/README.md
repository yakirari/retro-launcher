# Imagined period-style operator conversions

These **131 complete-cabinet alternatives** fill the historical-photo gaps without
pretending to be exact historical cabinets. Every alternative and its catalogue
aliases use **`kind: "period-style-conversion"`**. They are photo-based composites,
with illustrative sign, paint, glass and control edits, **not photographs of
authentic machines running these games** and not factory design reconstructions.

## Coverage

| Inventory representation | Games |
|---|---:|
| Existing source-set real-photo cutouts, unchanged | 217 |
| Existing confirmed-parent real-photo alternatives, unchanged | 35 |
| New imagined operator conversions | 131 |
| **Total full-cabinet inventory coverage** | **383 / 383** |

- 250 existing unique photo assets remain byte-identical.
- 131 new outputs are named `{rom}__conversion.png`, under `artwork/full-cabinets`.
- All 485 catalogue lookup keys have explicit assets: 328 existing photo keys,
  plus 157 conversion keys (131 inventory ROMs and 26 additional aliases).
- All outputs are 960 × 768 true RGBA PNGs. Their silhouettes retain every alpha
  value of the donor photo cutout, including antialiased edges and the full base.
- 88 outputs use retrieved source marquees: 67 exact ROM URLs, 21 direct metadata
  parent/canonical identities with the same verified game title.
- 43 outputs use small, restrained typeset titles **only in the top marquee**.
  No source marquee was available for those candidate identities.
- No titles, covers, or arbitrary gameplay images are inserted into any CRT.
  Screens are deliberately dark reflective glass. No gameplay snapshots are used.

The parent [gaps.json](../gaps.json) remains unchanged: it records **missing or
rejected historical photos**, not missing alternatives. Existing source review
and photo-provenance files remain the authority for the original 252 games.

## Shell sources and deliberate edits

Original URLs (ArcadeItalia / MAME cabinet image archive):

| Donor reference | Assigned games | Geometry and use |
|---|---:|---|
| https://adb.arcadeitalia.net/media/mame.current/cabinets/frogger.png | 18 | Early wooden dedicated-upright shape, used for early/mid-eighties conversions; donor upper-side decal repainted, lower woodgrain, coin door and base retained. |
| https://adb.arcadeitalia.net/media/mame.current/cabinets/columns.png | 91 | Plain-sided dark upright, used for late-eighties/early-nineties operator/kit alternatives; no assertion of original JAMMA wiring. |
| https://adb.arcadeitalia.net/media/mame.current/cabinets/sonic.png | 22 | Sega Aero City photo shell, used for 1995+ candy-style alternatives; replacement operator sign insert across the sloped roof fascia, white body, door and base retained. |

All three original photos and approved cutouts were visually inspected before
measuring top-sign, screen and donor-decal coordinates. The generator reads copies
of existing approved transparent cutouts. It **never writes to donor files**.
Game-bearing donor signs, screen content, stickers, and control-deck art are
replaced. Plain sides stay plain, rather than acquiring invented franchise art.
Existing shell wear, geometry, reflections outside edited glass, coin mechanisms,
and bases remain photographic. Edits inside the shell are illustrative and can
appear smoother than the underlying low-resolution photograph.

Early uprights predate many of the assigned games: this is intentional reuse in
an imagined conversion, **not evidence that the game shipped in that cabinet**.
Candy shells are appropriate conversion alternatives, not a claim that every
1995+ title originally used an Aero City. The uncertain `198?` year of Fantasy
Zone 2 is explicitly disclosed. Factory controls, monitor orientation, wiring,
player count and electrical compatibility are not certified. Motorcycle titles
receive illustrative upright handlebar conversions, not replicas of their
ride-on machines. Gunbarich receives an illustrative spinner; fighting games use
six-button decks. No playability claim is made.

## Marquee sourcing

Requests use only `https://adb.arcadeitalia.net/media/mame.current/marquees/{rom}.png`,
with **at most six concurrent requests**, 15-second timeouts and bounded image
dimensions. Exact ROM is attempted first. A direct parent/canonical identity is
eligible only when existing catalogue metadata verifies the same game title.
The generator does not guess names or substitute unrelated games. For example,
Muscle Bomber Duo is not assigned its different-title parent Slam Masters' sign.

Retrieved images are cached under [marquees](marquees). Sign art is letterboxed
in the design plane and inverse-projectively sampled into four measured physical
top-sign corners. Perspective foreshortening and the small candy fascia limit
legibility. Fallback typography remains intentionally small; it is not enlarged
into a central-screen title card. Marquee identity is based on source ROM plus
verified metadata, not on a claim of archival/factory-authentic sign artwork.

## Regeneration and verification

[GenerateArcadeConversions.mjs](../../../../scripts/GenerateArcadeConversions.mjs)
orchestrates cached downloads, rendering and mapping. It requires Node 18+ and
JDK 17, not Gradle or third-party modules. `--offline` reuses the cache without
network access; `--verify` checks existing artifacts without regenerating them.
`--root=/absolute/workspace/path` selects another checkout. Set `JAVAC` to the
compiler path when not using the locally configured JDK.

[GenerateArcadeConversions.java](../../../../scripts/GenerateArcadeConversions.java)
performs rendering and regression fixtures. It reuses the existing preparation
tool's strict JSON reader, SHA-256 helper, canvas verifier and alpha audit; those
existing source files are not modified. Compilation goes into an automatically
removed temporary directory. Run the photo-preparation stage **before** this
conversion stage when regenerating both; that older stage intentionally writes
a historical-photo-only mapping.

Evidence:

- [plan.json](plan.json): every inventory game, metadata year/canonical identity,
  shell choice, control category, marquee source identity and fallback reason.
- [marquee-downloads.json](marquee-downloads.json): retrieved URLs, dimensions,
  SHA-256 and cached failures/404 outcomes.
- [preserved-photos.json](preserved-photos.json): pre-conversion photo mappings and
  all 250 original output SHA-256 values; reruns must preserve them exactly.
- [provenance.json](provenance.json): every output SHA-256, shell source/cutout
  hashes, alpha audit and explicit representation disclaimer.
- Five `review-*` sheets: all 131 final alternatives on contrasting backgrounds.
- Three `marquee-review-*` sheets: retrieved marquee identities before projection.

Validation checks all 383 inventory ROMs, every associated catalogue alias, the
five-field mapping schema, unique keys, non-null assets, source/output digests,
RGBA PNG headers, 960 × 768 dimensions, transparent borders, nonrectangular full
silhouettes and **pixel-exact preservation of donor alpha**. Renderer fixtures
check perspective placement, preservation outside the sign, and isolation of top
sign typography from the CRT. The Java renderer also independently decodes all
250 original and 131 saved conversion PNGs, rechecks transparency, donor alpha,
original hashes and dark CRT centers. Its `[workspace-root] --verify` mode runs
that decoded-pixel audit without rendering. Compiler lint passes. No Gradle build
is needed.

Original image/artwork rights remain with their respective owners. Source
attribution and compositing do not confer permission to redistribute the original
artwork. No claim of historical authenticity is attached to these alternatives.