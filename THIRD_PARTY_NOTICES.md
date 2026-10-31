# Third-party notices

This notice covers only components with license-notice obligations or artwork whose redistribution
terms need review. Detailed asset records are linked below.

## Required license notices

- **jsoup 1.18.3 — MIT:** include its copyright and MIT license notice when redistributing the
  dependency. It is declared in [app/build.gradle.kts](app/build.gradle.kts).
- **GB/GBC/GBA shell graphics — GPL-3.0-or-later:** retain the [Slot credits](app/src/main/assets/SLOT-ARTWORK-CREDITS.txt)
  and [bundled GPL text](app/src/main/assets/licenses/Slot-GPL-3.0.txt). Check source and notice
  obligations for the exact distribution.

## Artwork needing redistribution review

| Artwork | Where to check |
| --- | --- |
| Arcade cabinet fronts | [Credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [per-image index](app/src/main/assets/arcade_presentations/index.json). Some creators or redistribution permissions are not established. |
| NES box fronts | [Credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [per-image catalog](app/src/main/assets/nes_covers/index.json). The cited site terms do not expressly establish app-distribution permission. |
| SNES cartridge labels | [Per-image catalog](app/src/main/assets/snes_labels/index.json). Sources and redistribution permissions vary; some are unknown. |
| Optional downloaded cartridge labels | [Source notes](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt) and [cartridge metadata](app/src/main/assets/cartridge-artwork/). These images are fetched at runtime, not bundled; downloading or caching does not grant redistribution rights. |

For assets needing review, obtain documented permission for redistribution or remove the affected
content. A source citation, public availability or user-supplied image is not itself permission.

Before distributing a build, include required license notices and review the exact release dependency
set:

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```
