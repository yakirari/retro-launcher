# Owned-game cabinet references

Retrieved 2026-10-09 from Arcade Italia's individual MAME cabinet images:

`https://adb.arcadeitalia.net/media/mame.current/cabinets/{rom}.png`

Game information and reference pages:
`https://adb.arcadeitalia.net/dettaglio_mame.php?game_name={rom}`

## Expanded inventory (2026-10-09)

See [EXPANDED-COVERAGE.md](EXPANDED-COVERAGE.md) for the 383-ROM inventory,
source-by-source manifest, new reviewed silhouettes, exclusions and remaining
gaps. The original 36 references were retained without redownloading.
New opaque downloads are deliberately isolated in the downloads subdirectory;
only 37 visually reviewed, pre-cut transparent images were promoted to top-level
build inputs. Already-cut alpha images bypass all color-key removal so genuine
black bodies and white outer details survive the second framing pass.

## Attribution and rights

The related progetto-SNAPS cabinet collection describes its pictures as reference
coin-op photographs, sometimes of the same set variant. Its site states use under
fair use, with copyrights retained by the game companies. This is **not a blanket
open redistribution license**. Review asset rights before public/commercial
distribution; maintain attribution and do not imply these images are owned by us.

Collection and attribution: https://www.progettosnaps.net/cabinets/
Readme: https://www.progettosnaps.net/cabinets/docs/ReadMe_Cabinets.txt

These published statements were checked again on 2026-10-11. Collection credits,
modification notes and optional upstream support links are included in the APK's
[artwork credits](../../app/src/main/assets/ARTWORK-CREDITS.txt). This preserves
attribution without presenting the upstream fair-use statement as an open license.

## Included references

1942, 1943, Asteroids, BurgerTime, Contra, Double Dragon (and US variant), Defender,
Dig Dug, Donkey Kong 3, Final Fight, Frogger, Galaga, Galaxian, Ghouls 'n Ghosts,
Golden Axe, Space Invaders, Joust, Mario Bros., Mortal Kombat II, Ms. Pac-Man,
Marvel vs. Capcom, Out Run (sit-down and upright variant), Pac-Man, Paperboy,
Q*bert, R-Type II, Street Fighter II, Shinobi, Star Wars cockpit, Tapper, Tekken 3,
Turtles in Time, TRON, X-Men.

## Important accuracy distinctions

- Cabinet photo identifiers are not a guarantee of untouched original hardware.
  They can show restorations, conversions, contemporary reproductions or modern
  software running in an original-style shell.
- Pac-Man reference has the classic Midway-style upright shape/art rather than
  Arcade1Up branding, but its screen shows a multigame menu. Treat as a period-style
  alternative, not an unmodified factory Pac-Man machine.
- 1942 uses a Romstar conversion, historically plausible for a kit release.
- `outrunb` is an upright Out Run reference; the parent reference is sit-down.
  A bootleg ROM does not identify the particular cabinet it was installed in.
- `tmnt22p` (two-player Turtles in Time) and `xmen2p` have no direct cabinet image
  at this source. We use parent game's four-player cabinet photos as same-game
  alternatives. Player/control layout is NOT an exact match.
- `dkong3` and `rtype2` are separate sequels, never reuse `dkong`/`rtype` images.
- Star Wars reference shows the Atari cockpit variant, not the upright variant.

## Processing

Original downloads stay in this folder. Build preparation removes edge-connected
flat black (RGB <= 2) background **only for these reviewed black-backed sources**.
Shinobi's compressed near-black border uses a reviewed threshold of 16. Street
Fighter II preserves original black front-panel pixels between its visible side
rails; these would otherwise leak into the background mask through its open base.
Neither exception applies to other cabinets.
Tiny disconnected border artifacts are removed, then images are aspect-preserving
scaled and bottom-aligned on a transparent 960x768 canvas. No colors, logos,
controls or cabinet branding are invented; no generative upscaling is used.

Use CabinetContactSheet.java on generated images to inspect alpha silhouettes
against a non-black background. Generic runtime cabinets and other source images
must not apply this black-key processing: it would erase genuine dark hardware.