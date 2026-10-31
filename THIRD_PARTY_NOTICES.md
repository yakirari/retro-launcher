# Third-party notices

This page summarizes third-party material in Retro Launcher and where to find its detailed
attribution. It is not a blanket license for included or downloaded artwork.

## License scope

- Original launcher code and project-author-created artwork are covered by the project
  [MIT license](LICENSE).
- Third-party images, metadata, trademarks, dependencies and content downloaded at runtime are not
  automatically covered by that license.
- Public availability, attribution, user supply or AI generation does not by itself grant
  redistribution rights.

## Bundled material

| Material | License and provenance |
| --- | --- |
| jsoup 1.18.3 | MIT-licensed dependency. Preserve its upstream copyright and license notice when redistributing dependency copies. Declared in [app/build.gradle.kts](app/build.gradle.kts). |
| Arcade cabinet fronts | Source-pack and per-image records are in the [artwork credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [presentation index](app/src/main/assets/arcade_presentations/index.json). Some creator and redistribution permissions are not established; these images are not relicensed under MIT. |
| NES box fronts | Source, contributor credits and file hashes are in the [artwork credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [NES catalog](app/src/main/assets/nes_covers/index.json). The cited site terms do not expressly establish app-distribution permission. |
| SNES cartridge labels | The [SNES catalog](app/src/main/assets/snes_labels/index.json) records supplied recreations, composites, originals and per-image source information. Some sources or redistribution permissions are unknown; these labels are not relicensed under MIT. |
| GB/GBC/GBA shell graphics | Adapted from Slot artwork under GPL-3.0-or-later. Preserve the [Slot credits](app/src/main/assets/SLOT-ARTWORK-CREDITS.txt) and [bundled GPL license](app/src/main/assets/licenses/Slot-GPL-3.0.txt), and meet applicable source and notice obligations. |

## Optional downloaded artwork

Some cartridge labels and covers are fetched by the app when requested; they are cached on the
device and are not bundled in the APK. Their source and license information is recorded in
[cartridge-label sources](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt) and the
[cartridge artwork metadata](app/src/main/assets/cartridge-artwork/). Downloading or caching an
image does not grant permission to redistribute it.

## Before redistributing

1. Check the per-file records linked above for the exact assets in the build.
2. Obtain permission for assets whose redistribution terms are unclear, or remove those assets.
3. Include all required attribution and license texts. For GPL-covered graphics, verify the
   corresponding-source and notice requirements for the exact distribution.
4. Review the resolved release dependencies and include their required notices:

   ```sh
   ./gradlew :app:dependencies --configuration releaseRuntimeClasspath
   ```

Attribution and this inventory are not legal clearance; verify rights for each release.
