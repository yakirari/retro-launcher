# Release checklist

[Development](DEVELOPMENT.md) · [Licensing scope](../THIRD_PARTY_NOTICES.md)

## Before public hosting

- [ ] Confirm rights/contributor permission for code under [MIT](../LICENSE).
- [ ] Resolve redistribution rights for every photo, cover, overlay, background, icon and derivative.
      Attribution alone does not clear affected files for publication.
- [ ] Record AI cabinet generator/output terms and reference inputs; reconcile the author's AI-generation
      statement with documented downloaded overrides/donor images in the current build.
- [ ] Audit source and synchronized assets; exclude personal screenshots/unreviewed downloads.
- [x] Remove the unused embedded RPCSX wrapper and vendored emulator sources from the current tree.
      Historical revisions retain their original third-party notices; no history rewrite is implied.
- [ ] Review metadata rights and resolved dependency notices; plan binary notice distribution.
- [ ] Verify the APK includes [cabinet credits](../app/src/main/assets/ARTWORK-CREDITS.txt) and retain
      source/processing manifests. Credits and donations do not establish redistribution permission.
- [ ] Review tracked files **and Git history** for secrets, keystores, machine paths/device IDs,
      game/firmware files and personal data. Ignore rules do not remove tracked history.
- [ ] Rotate exposed secrets and coordinate history cleanup before inviting contributors.
- [x] Establish repository URL/name and maintainer: https://github.com/yakirari/retro-launcher, @yakirari.
- [ ] Activate private security reporting and repository protections; use the [hosting checklist](HOSTING.md).
- [ ] Add screenshots containing redistributable content and accurate compatibility statements.
- [ ] Keep local GIMP projects out of version control; consider LFS/separate storage for large retained
      PNG sources without losing build inputs. Removed files may still exist in Git history.
- [ ] Verify a clean checkout builds without the original developer's machine paths/cached artifacts.

GitHub Releases is the planned artifact channel. No release availability or missing artwork permission
is implied by this checklist. A public security mailbox is not configured.

## First release draft

[0.1.0 — First preview](releases/0.1.0.md) is a GitHub draft prerelease with a clearly labelled
debug-signed preview APK and SHA256SUMS attached for maintainer testing. The repository is public,
but a draft and its downloads require repository write access until published. Do not represent the
debug-signed APK as a production release or treat repository visibility as artwork clearance.
Complete signing and redistribution review before publishing an APK, then add its checksum,
signature/upgrade verification and actual device test results. Version 0.1.0/code 1 is retained
for this first release preparation; subsequent application releases must increase the version code.

## Release procedure

1. Review the commit/diff and update [CHANGELOG.md](../CHANGELOG.md).
2. Increase `versionCode` and set `versionName` in [app/build.gradle.kts](../app/build.gradle.kts).
3. Run JVM tests/debug assembly and lint/relevant instrumented tests; record failures honestly.
4. Test an upgrade without clearing data: titles/artwork/history/targets/settings/grants must survive.
5. Complete the device smoke checks below, noting device/Android/emulator versions.
6. Generate/verify reviewed assets; do not implicitly fetch new third-party art during release.
7. Build/sign with the release key; verify signature, version/package ID and upgrade installation.
8. Publish APK, checksum, changelog, source tag and required notices through the selected trusted channel.
9. State known issues/tested versions; avoid unsupported gameplay or benchmark claims.

## Signing

The local release-keystore.properties file holds `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
It and the keystore directory are ignored. Use encrypted backups/access control; never place passwords
in Git, issues, documentation, assistant prompts or logs.

```sh
./gradlew :app:assembleRelease --console=plain
```

Without signing properties the output can be unsigned. A successful assembly does not prove an installable
release. Inspect `app/build/outputs/apk/release/` and verify with the SDK's `apksigner`. Keep the same key
for updates; replacing it can require uninstalling and lose library data.

## Device smoke tests

- Home activation and switching away; touch/controller input.
- All/main vs Android Games vs Emulators & Launchers separation.
- Long gallery scrolling, page boundaries, direct focus jumps, rapid filters and memory behavior.
- Add/edit dialogs with keyboard, short landscape viewport and reachable actions.
- Folder authorization/cancellation, repeat scans and conservative duplicates.
- Supported emulator game boot and clear error for missing/invalid targets.
- ScummVM registered picker/config import, no silent library-screen fallback.
- Hub pin confirmation, auto-add, repeat-pin dedup and pins owned by another launcher.
- Artwork online/offline/failure and manual-edit preservation during background jobs.
- Full-cover PS1 template, profile override/reset, upgrade persistence.

## CI and ongoing maintenance

[Android checks](../.github/workflows/build.yml) uses JDK 17/SDK 36 and the wrapper for JVM tests and
debug assembly, with read-only permissions and no release credentials. Its first GitHub run still needs
verification. Lint remains a separate check because of the documented metadata-reader issue; no lint pass
is implied. UI jobs need an unlocked Android 13+ emulator and timeouts. Do not expose release signing
credentials to untrusted fork PRs.

Review dependencies, launch contracts, Home/SAF behavior and artwork service failures regularly.
Separate verified maintainer notes from user-facing compatibility promises.
