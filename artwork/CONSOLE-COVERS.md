# GC, N64 and PS2 cover presentation

See [system-wide cartridge artwork and preparation](CARTRIDGE-ARTWORK.md) for external coverage,
the full N64 scan inventory/review workflow, and the distinction between prepared and distributable assets.

- PlayStation full-cover jewel cases preserve the original CD shell's four cover-retaining tabs
  and right-side clips. The prepared template removes baked-in branding, not the molded plastic
  details. Gradle regenerates it from the untouched PS1 and CD originals; Dreamcast retains the
  same detailed CD shell. Cover artwork sits behind these details in both nostalgic layouts.
- GameCube and PS2 use full-cover shell derivatives. The original templates are untouched.
  `scripts/PrepareConsoleTemplates.java` clears only the inner printed face, retaining the case
  perimeter. The actual cover supplies its own platform heading, region and rating instead of
  inheriting a baked-in GameCube banner or the wrong generic PS2 ESRB E badge.
- GameCube no longer blindly removes the first 5% of every cover. GC, N64 and PS2 fit complete
  artwork, retaining edge text and ratings; source aspect differences can leave dark space.
- N64 retains the original cartridge template. The separate nostalgic label lookup uses the
  [archived Analogue 3D label database](https://archive.org/details/Analogue3Dcartdatabase),
  matched by CRC32 of the first 8192 big-endian ROM bytes. Byte- and word-swapped ROMs and
  single-ROM ZIPs are supported; ambiguous ZIPs, invalid headers and absent signatures are
  not guessed. Seven reviewed USA signatures preferentially download the original cartridge
  scans from the [N64 Cartridge Art Collection](https://archive.org/details/N64_Cartridge_Art_Collection).
  Reviewed source/crop/identity metadata lives in the packaged platform manifest, not a hardcoded
  device-library list; the offline preparation tools inventory all 313 available cartridge scans.
  Each source is SHA-256/dimension verified, then its manually reviewed sticker rectangle is
  extracted (305–343 pixels wide, 350–381 high). Plastic outside the sticker is excluded;
  ordinary box covers are not cropped. Existing thumbnail caches upgrade automatically.
  Sources are downloaded individually, not as the entire 113 MiB archive. These are real scan
  pixels, not AI reconstruction or sharpened thumbnails; print/scan texture remains visible.
  Unknown signatures, the badly noisy Harvest Moon scan and the unavailable Japanese Shindou
  scan retain the exact 74×86 database label (database cached, about 22 MiB). Failed/offline
  high-resolution downloads retain existing labels and retry after a day. No title/region guessing.
  If neither source matches, whole-cover fallback remains without cropping or stretching.
  Matching/composition settles atomically, like handheld labels. No artificial ageing or
  scanlines are applied. Ordinary cover views and saved artwork remain unchanged.
- Explicit PS2 placement and transparency-mask coordinates share the same bounds. Masking no
  longer rescales an independently detected window onto a different artwork rectangle.
- Exact Libretro title matching prefers a region present in the ROM filename. Unknown regions
  are not guessed. N64 box-art preference is landscape, while GC/PS2 prefer portrait front scans.
  Comparable-aspect catalog images prefer higher resolution; tiny GC/N64/PS2 catalog downloads
  below 240 pixels on the short edge are rejected. Web results retain their relevance ordering.

Existing manual artwork is never replaced. These rendering improvements apply immediately;
source ranking applies to new downloads / Find images / artwork refresh. Generated autofill
covers on these three systems also receive a conservative exact-catalog refresh on startup,
saved separately from their originals. Failed matches keep the current image, and a concurrent
manual change wins at commit time. N64 cartridge labels are a separate cache, never box-art
replacements. The archive declares no media license: public accessibility is not permission
to redistribute copyrighted media. Source/rights notes are included in the app's cartridge
artwork attribution. No upscaling creates missing detail.

Pixel tests inspect packaged windows, removal of conflicting printed details and PS2 mask bounds,
and export previews using existing device-library covers when present. These are renderer previews,
not a claim that every game/region has been visually reviewed in the live UI.