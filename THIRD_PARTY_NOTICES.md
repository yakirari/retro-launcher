# Licensing scope and third-party notices

## Original code

### jsoup HTML parser

The app uses [jsoup 1.18.3](https://jsoup.org/) under the
[MIT license](https://github.com/jhy/jsoup/blob/jsoup-1.18.3/LICENSE).
Copyright © 2009–2024 Jonathan Hedley. Preserve its upstream license and copyright
notice with redistributed dependency copies. This software license does not apply
to images retrieved from LaunchBox Games Database. Community fanart, reconstructed
covers and publisher artwork retain their creators'/rightsholders' rights; no blanket
redistribution permission is established. See the [bundled NES catalog](app/src/main/assets/nes_covers/index.json).

[LICENSE](LICENSE) grants MIT terms for original code/documentation contributed by their copyright holders.
It does not grant rights to third-party source, photos, game covers, fonts, metadata, trademarks or
unresolved-author content. Contributor rights must be verified.

This inventory is a maintenance starting point, **not legal clearance or an exhaustive dependency audit**.
Resolve the [release checklist](docs/RELEASING.md) before publishing source/assets/binaries.

## Emulator source removal

The unused embedded wrapper and vendored RPCSX sources have been removed from the current project.
No emulator engine/source is bundled. Older Git revisions may still contain separately licensed source;
their original notices and license obligations apply if those revisions are distributed. Removal does
not relicense historical third-party files under MIT.

## Cabinet artwork — AI-generation statement and external inputs

The project author states that all cabinet artwork was AI-generated. This is recorded as
author-supplied provenance, not a claim that the images are photographs of authentic hardware.
Record the generator/model, applicable output terms and any reference-image inputs before release;
AI generation alone does not establish a redistribution license or rights to depicted game artwork.

The current app includes a user-supplied collection of cabinet fronts. Their creator, generation
method and redistribution permissions were not supplied. The author's statement does not establish
that these images are independently generated or cleared for redistribution.

Documented external sources include progetto-SNAPS through Arcade Italia:

- https://www.progettosnaps.net/cabinets/
- https://adb.arcadeitalia.net/
- [Packaged cabinet credits and support links](app/src/main/assets/ARTWORK-CREDITS.txt)
- [Cabinet-to-ROM mappings](app/src/main/assets/arcade-metadata/cabinet-art.json)

Checked on 2026-10-11: the collection page states that images are "free to use under fair use"
and that copyright remains with the game developers/publishers. Its readme credits AntoPISA and
the collection's contributors. The packaged credits preserve those source references, explain
modifications, and link the collection's optional support page. Donations do not grant image rights.
The images remain unchanged by this attribution update; the notice ships automatically as an APK asset.

Existing notes do not establish a blanket open redistribution license. Attribution is not permission.
Obtain documented rights, remove assets, or replace them with appropriately licensed/original alternatives
**before affected sources or APKs are public**. Audit packaged copies and source images; transformations
do not remove underlying rights.

The removed source/review folders are not retained in the current tree. The packaged fronts still need
rights review; their source and review metadata does not establish redistribution permission. Do not
represent generated or converted illustrations as photographs of historically authentic cabinets.

## Gameplay-lit cabinets and Stacked frontal presentations

The packaged presentation index records 381
user-supplied `retro_launcher_cabinets_v20/front` images, imported on 2026-10-25.
379 PNGs were copied byte-for-byte; two mislabeled JPEGs (Batman and Simpsons) were losslessly
encoded as PNG at their original resolution with decoded pixel equality verified.
Their creator, generation method and redistribution permissions were not supplied. They are not
certified photographs of factory-original cabinets or established MIT-licensed project artwork.
Per-file hashes/dimensions are recorded in the [packaged index](app/src/main/assets/arcade_presentations/index.json).
Obsolete generated fronts, lit alternatives and full-cabinet photos are not packaged. Their removal
does not resolve rights in earlier Git history or other distributions. Game images and panel art
remain third-party copyrighted material, not MIT. Attribution and public availability do not establish
permission; review the current fronts before redistribution.

## Arcade metadata

Arcade provenance references libretro MAME2003-plus XML:
https://github.com/libretro/mame2003-plus-libretro . Review metadata-specific terms and attribution,
not just upstream code licensing. Preserve source URLs/hashes/review tables. Public metadata is not
automatically licensed under this project's MIT terms.

## Other assets and branding

### Bundled NES Front Scans

84 reviewed NES box fronts from [Video Game Museum](https://www.vgmuseum.com/scans/nes.html)
are bundled in normalized form. The packaged artwork credits acknowledge the site
and its named scan contributors. Per-file provenance, normalization and release
matching are recorded in [the bundled NES catalog](app/src/main/assets/nes_covers/index.json).
Its FAQ permits credited website reuse but does not expressly cover app distribution;
bundling follows the project owner's direction, not a claim of additional permission.
Original publisher artwork and scan contributions are not relicensed under MIT.

### Bundled SNES Cartridge Labels

The unified [runtime catalog](app/src/main/assets/snes_labels/index.json) contains
48 JPEGs (42 user-supplied AI recreations and six composite-image extracts) and 58
user-supplied PNG labels (44 USA-shell and 14 Japan-shell). Per-image source
descriptions, URLs, hashes and dimensions remain in that index; source notes are in the
[packaged cartridge-label sources](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt).
The PNG pack excludes full-cartridge preview
renders. Some source and regional assignments are supplied claims, not independently
authenticated, and these records do not establish a redistribution license.
Bundling follows the project owner's direction and does not relicense third-party
game artwork under MIT.

The project author, [@yakirari](https://github.com/yakirari), confirms authorship of the launcher SVGs,
backgrounds, exported templates and original app artwork. Original contributions in
[app/src/main/res/drawable-nodpi](app/src/main/res/drawable-nodpi/) and
[backgrounds](app/src/main/assets/backgrounds/) are included under the project's MIT license.
This confirmation does not claim ownership of the documented external cabinet downloads or donor images
described above, nor of third-party game covers downloaded at runtime.
Brands remain their owners' property; no endorsement or trademark license is implied by SVG authorship.

Runtime artwork is checked in under the app's assets and resources and is packaged directly by Gradle.
The cleaned tree does not retain a parallel source-artwork directory. Deleting source or working files
does not clear rights in exported images or remove those files from earlier Git history.

Original simple hub symbols/digital frame generators do not grant rights to covers placed inside them.
The checked-in app resource contains the current PS1 shell template.

## Slot cartridge graphics

NES labels are separate from Slot graphics: the original USA Zelda release can
download a reviewed label crop from [NES-ZL-USA cartridge scans](https://archive.org/details/TheLegendofZelda-NES-CartScans).
Only [reviewed source and identity metadata](app/src/main/assets/cartridge-artwork/nes-scans.json)
is packaged; no scan image is bundled or MIT-relicensed. The source declares no
media license; download availability is not a redistribution grant. Exact full-ROM
SHA-1 metadata is attributed to libretro-database under CC BY-SA 4.0, with links in
the [packaged media notice](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt).
Manual labels retain priority; other games and regions do not inherit this image.

GB/GBC/GBA neutral shell graphics are from Slot revision f4b7bc3b1f7240c00d245c82fe123181a2d5c590,
not original MIT artwork. Slot credits Markus / MarkusMaribu for its skeuomorphic art and declares
GPL-3.0-or-later. The imports preserve the plastic pixels and open only the blank label wells;
runtime tinting adds decorative shell colors. The runtime shell resources are retained in the app.

- [Packaged Slot credits](app/src/main/assets/SLOT-ARTWORK-CREDITS.txt)
- [Packaged full GPL license](app/src/main/assets/licenses/Slot-GPL-3.0.txt)

These graphics and their derivatives are not covered by the MIT grant. Preserve their GPL terms
and exact corresponding sources when distributing; review combined-distribution obligations before
a public source/APK release. No blanket relicensing of other third-party material is implied.

## Runtime artwork

GB/GBC/GBA nostalgic stickers optionally download exact-ROM CRC matches from Slot's hosted art set.
Slot Cart Studio attributes ScreenScraper media to CC BY-NC-SA 4.0 and libretro database data
to CC BY-SA 4.0. These downloaded images are not MIT-licensed or bundled here; noncommercial,
attribution and share-alike conditions need separate review for any redistribution/use.
See [packaged sources and license links](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt).

N64 cartridge stickers use the separately downloaded
[archived Analogue 3D label database](https://archive.org/details/Analogue3Dcartdatabase).
Its 74×86 images are matched by normalized ROM-header CRC, cached locally and not bundled.
The archive does not declare a media license; public availability is not public-domain status
or permission to redistribute. These images remain subject to their original owners' rights.
Seven reviewed USA signatures prefer individually fetched original cartridge scans from
[N64 Cartridge Art Collection](https://archive.org/details/N64_Cartridge_Art_Collection),
uploaded by Jason Scott (2016). Sources are hash-verified and only the manually reviewed
sticker rectangle is extracted. These scans also declare no media license and are not
bundled or relicensed. Exact source filenames/hashes/crops are in
[N64 scan manifest](app/src/main/assets/cartridge-artwork/n64-scans.json).
Reviewed source and identity metadata is recorded in the packaged manifests; preparation alone does
not grant permission to bundle or host third-party images.
No Analogue/Nintendo affiliation or MIT relicensing is implied. Format documentation was
consulted, not copied; source and format references are in the packaged artwork notices above.

Lookup uses Steam Store/CDN, Libretro/Eden catalogues and web search providers. These services' availability,
terms and image rights are independent of the launcher license. Do not commit user-downloaded covers
or promise search results are redistributable.

## Resolved dependencies

Main dependencies include AndroidX/Compose/Material, AndroidSVG and test libraries. Audit the actual
transitive release graph and verified upstream license texts:

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

Produce a versioned notices inventory before release. Packaging excludes some META-INF license resources;
do not assume required notices automatically survive into the APK. Decide and verify how they ship.
