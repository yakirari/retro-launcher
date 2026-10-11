# Contributing

Start with [development setup](docs/DEVELOPMENT.md), [architecture](docs/ARCHITECTURE.md), and
[third-party rights](THIRD_PARTY_NOTICES.md).

## Reports and proposals

- Discuss substantial UX/storage/migration/dependency/integration work in an issue first.
- Provide versions, reproducible steps and expected/actual behavior through
   [GitHub Issues](https://github.com/yakirari/retro-launcher/issues).
- Redact screenshots/logs. Do not upload games, BIOS/firmware, account credentials, private game databases,
  or full personal library/preferences dumps.
- Check whether a boot failure also occurs inside the emulator.

## Pull requests

1. Use a focused branch and scope changes to one concern.
2. Follow existing Kotlin/Compose style; extract pure policy helpers where practical.
3. Add regression tests for parsing, merging, scans, classification, paging and migrations.
4. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`.
5. Run relevant lint/instrumented tests and state actual outcomes, including unavailable devices/tooling failures.
6. For UI/input/launch changes, test on Android 13+ hardware/emulator and include redacted screenshots
   and controller/keyboard behavior. JVM tests are not visual or gameplay validation.
7. Update documentation and [CHANGELOG.md](CHANGELOG.md) when behavior changes.

Describe what/why, verification, limitations, migration impact, new dependencies and provenance in the PR.
Do not commit SDK paths, signing material, build outputs, temporary files, device IDs or unreviewed downloads.

## Licensing and conduct

Original contributions are submitted under MIT unless separately agreed. Do not relabel third-party
material as MIT. Include source, author, permission/license evidence, modifications and required attribution
for art, fonts, metadata and copied source. A public URL or “fair use” note is not redistribution permission.
No CLA/DCO process is established; maintainers must resolve ownership questions before accepting changes.

Cabinet source credits and optional upstream support links are recorded in the
[packaged artwork notice](app/src/main/assets/ARTWORK-CREDITS.txt). Submit filename-specific attribution
corrections with source evidence. Artwork replacements should include explicit sharing terms; do not
label a publicly accessible image as public domain without evidence.

Prioritize data safety, conservative identity, verified APIs, responsiveness and bounded/accessible dialogs.
See [maintenance invariants](docs/ARCHITECTURE.md#invariants-to-preserve).
Be respectful; no harassment, personal-information disclosure or requests for game downloads.
Maintainers may remove inappropriate content and decline out-of-scope contributions.
