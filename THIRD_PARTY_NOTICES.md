# Third-party notices

This notice covers components with license-notice obligations and records the provenance of bundled
artwork.

## Required license notices

- **jsoup 1.18.3 — MIT:** include its copyright and MIT license notice when redistributing the
  dependency. The notice is bundled in [JSOUP-LICENSE.txt](app/src/main/assets/JSOUP-LICENSE.txt);
  the dependency is declared in [app/build.gradle.kts](app/build.gradle.kts).
- **GB/GBC/GBA shell graphics — GPL-3.0-or-later:** retain the [Slot credits](app/src/main/assets/SLOT-ARTWORK-CREDITS.txt)
  and [bundled GPL text](app/src/main/assets/licenses/Slot-GPL-3.0.txt). Check source and notice
  obligations for the exact distribution.

## Artwork redistribution

The project maintainer confirmed that redistribution rights have been verified and authorized for
all artwork bundled in v0.1.6. Some provenance records below do not contain the permission evidence;
retain supporting records separately. This confirmation does not extend to optional artwork fetched
by users at runtime.

| Artwork records | Where to check |
| --- | --- |
| Arcade cabinet fronts | [Credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [per-image index](app/src/main/assets/artwork/arcade/presentations/index.json). Some creator attributions or permission evidence are not captured in these records. |
| NES box fronts | [Credits](app/src/main/assets/ARTWORK-CREDITS.txt) and [per-image catalog](app/src/main/assets/artwork/nes/covers/index.json). The cited site terms do not themselves document app-distribution permission. |
| SNES cartridge labels | [Per-image catalog](app/src/main/assets/artwork/snes/labels/index.json). Source and permission evidence varies across records. |
| Optional downloaded cartridge labels | [Source notes](app/src/main/assets/CARTRIDGE-LABEL-SOURCES.txt) and [cartridge metadata](app/src/main/assets/artwork/nes/scans.json), [N64 metadata](app/src/main/assets/artwork/n64/scans.json). These images are fetched at runtime, not bundled; downloading or caching does not grant redistribution rights. |

Before distributing a build, include required license notices and review the exact release dependency
set:

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```
