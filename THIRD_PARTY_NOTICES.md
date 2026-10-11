# Licensing scope and third-party notices

## Original code

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

The current repository also contains documented external cabinet downloads and donor-image
derivatives. Build preparation uses reviewed reference images as overrides, and the full-cabinet
collection includes processed images/conversions. The author's statement does not establish that
these external build inputs have been replaced by independently generated alternatives.

Documented external sources include progetto-SNAPS through Arcade Italia:

- https://www.progettosnaps.net/cabinets/
- https://adb.arcadeitalia.net/
- [Packaged cabinet credits and support links](app/src/main/assets/ARTWORK-CREDITS.txt)
- [Source inventory/rights caveat](artwork/cabinet-references/SOURCES.md)
- [Expanded provenance](artwork/cabinet-references/EXPANDED-COVERAGE.md)
- [Processing and accuracy notes](artwork/ARCADE-CABINETS.md)

Checked on 2026-10-11: the collection page states that images are "free to use under fair use"
and that copyright remains with the game developers/publishers. Its readme credits AntoPISA and
the collection's contributors. The packaged credits preserve those source references, explain
modifications, and link the collection's optional support page. Donations do not grant image rights.
The images remain unchanged by this attribution update; the notice ships automatically as an APK asset.

Existing notes do not establish a blanket open redistribution license. Attribution is not permission.
Obtain documented rights, remove assets, or replace them with appropriately licensed/original alternatives
**before affected sources or APKs are public**. Audit packaged copies and source images; transformations
do not remove underlying rights.

Reconcile the per-file provenance with the AI-generation statement before labeling the entire packaged
collection AI-generated. Donor-image derivatives retain their source-rights review requirements. Generated
or converted illustrations must not be represented as photographs of historically authentic cabinets.

## Metadata

Arcade provenance references libretro MAME2003-plus XML:
https://github.com/libretro/mame2003-plus-libretro . Review metadata-specific terms and attribution,
not just upstream code licensing. Preserve source URLs/hashes/review tables. Public metadata is not
automatically licensed under this project's MIT terms.

## Other assets and branding

The project author, [@yakirari](https://github.com/yakirari), confirms authorship of the launcher SVGs,
backgrounds, exported templates and original app artwork. Original contributions in
[app/src/main/res/raw](app/src/main/res/raw/), [backgrounds](backgrounds/) and [overlay](overlay/),
plus their packaged copies, are included under the project's MIT license.
This confirmation does not claim ownership of the documented external cabinet downloads or donor images
described above, nor of third-party game covers downloaded at runtime.
Brands remain their owners' property; no endorsement or trademark license is implied by SVG authorship.

Unused GIMP working files and unused PNG template alternatives have been removed from the working tree.
Exported templates needed by Gradle and the PS1 regeneration source are retained. Deleting working files
does not clear rights in their exported images or remove those files from earlier Git history.

Original simple hub symbols/digital frame generators do not grant rights to covers placed inside them.
The PS1 generator derives a shell from the author's exported template.

## Runtime artwork

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
