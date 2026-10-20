# Handheld cartridge presentation prototype

GB, GBC and GBA nostalgic views now separate three concerns:

1. Actual Slot neutral cartridge graphics by Markus, preserving their texture, embossing and shape.
2. A separately prepared sticker: a complete custom label image, or the complete existing cover
   fitted to the entire label well. No generated game title, platform heading or reserved text bands
   reduce the sticker area. Artwork is never stretched or cropped; aspect-ratio differences leave
   artwork-colored space (neutral label stock for custom labels). Existing lettering inside the
   source artwork is retained. A portrait box cover cannot fill a wide GBA label without either
   cropping, distortion or unused space; a full cartridge-label image is preferable there.
3. Decorative shell color applied only to the mould. Auto samples each game's artwork using a
   saturation-weighted hue histogram, ignoring white margins and dark outlines. Monochrome/missing
   art falls back to gray GB, indigo GBC and graphite GBA. Explicit palette choices always override
   Auto. No claim is made that these are original cartridge colors of individual releases.

Edit game → Artwork → Handheld cartridge offers an independent image picker and shell palette.
Ordinary cover views still use the existing cover. Picking or clearing a label never replaces it.
Library JSON retains label URI/color, and duplicate reconciliation preserves these choices.
Unreadable custom labels fall back to the existing cover. Rendering never rewrites source images.

## Reproduction and provenance

The earlier procedural shell prototype has been replaced with Slot's actual GB/GBC/GBA neutral PNGs.
[Source inventory and license](slot/README.md) record the pinned revision, artist, hashes and terms.
These third-party graphics are GPL-3.0-or-later, **not MIT**; credits and the full license ship in the APK.
[PrepareSlotHandheldShells.java](../scripts/PrepareSlotHandheldShells.java) opens only the blank label
wells and verifies that the original plastic pixels are otherwise unchanged. Gradle invokes it before
packaging. The earlier author-owned legacy GB/GBC/GBA source templates remain untouched.
The independent label composition and color controls remain in place. No hosted media are bundled;
Slot executable code, clear-shell interiors and separately composed game logos are not imported.

## Art sourcing and limits

GB, GBC and GBA nostalgic views now prefer complete cartridge labels from the public Slot art index at
https://art.slot-cfw.fyi/index.json. Lookup uses CRC32 of the exact authorized ROM bytes (or the
single matching ROM member of a ZIP), not the archive CRC, title similarity, or another game's label.
GB accepts .gb; GBC accepts .gbc and dual-compatible .gb; GBA accepts .gba. Ambiguous archives are rejected.
Only each system's indexed support-texture PNGs are accepted: wide for GBA, near-square for GB/GBC.
System-specific cache keys keep labels and misses separate. Two background workers
limit I/O; checksum, index, misses and decoded image downloads are cached locally. No ROM bytes
are uploaded. Downloads use timeouts and byte limits; failures retain the existing cover.
Manual labels take priority. Existing entries need no rescan.

While a sticker is loading, its well shows neutral label stock, not a miniature cover under the
incoming art. Lookup and composition publish one settled result. A bounded memory cache restores
completed stickers immediately when scrolling back; recycled slots reset by game/source identity
instead of briefly showing the previous game's label. Cover fallback is used only after lookup
finishes unsuccessfully, never as a temporary loading image.

The hosted media are sourced by Slot Cart Studio from ScreenScraper, whose upstream notice
identifies CC BY-NC-SA 4.0 artwork and CC BY-SA 4.0 database information. Runtime downloads are
not bundled or relicensed as MIT; see the packaged cartridge artwork source notice. No account,
direct ScreenScraper API, title guessing, fake publisher seals or invented label details are used.
Unmatched ROM hacks may need a manually supplied full label. A wide real label already includes
its own logo, rating/publisher printing and background: no square box cover is pasted on top.

Composed-logo alternatives and additional printed layers remain future work,
not silently filled with invented publisher seals, serials, region marks or authenticity claims.
There are no simulated translucent shells/PCBs or per-game original-hardware color detection yet.

Android pixel tests verify untouched inputs, alpha/label-window preservation, tint separation,
full-window cover retention, no generated printing, and artwork-driven automatic color. They also export a palette contact
sheet, `handheld-review.png`, to the app's external cache for visual review.