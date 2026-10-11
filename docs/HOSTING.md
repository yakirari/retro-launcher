# GitHub hosting setup

Repository: https://github.com/yakirari/retro-launcher
Maintainer: [@yakirari](https://github.com/yakirari)

## Administrator steps

These settings need an authenticated repository administrator. Local documentation and workflow files
do not activate GitHub settings. The browser session used for preparation was signed out, so the following
settings have not been verified or changed.

1. Confirm the repository exists under the intended owner and review its current visibility.
2. In **Settings → General**, enable Issues and add a description such as:
   “Android Home launcher for retro and PC game libraries with controller-friendly galleries.”
   Suggested topics: `android`, `launcher`, `kotlin`, `jetpack-compose`, `retrogaming`.
3. In **Settings → Security** (or **Code security**), enable private vulnerability reporting.
   Verify https://github.com/yakirari/retro-launcher/security/advisories/new works for reporters.
4. Enable available dependency alerts and secret scanning/push protection. Feature availability varies
   by repository visibility and GitHub plan; do not assume every option is available.
5. Protect the default branch with a ruleset: prevent force pushes/deletion and require the **Build and
   unit tests** check after the workflow has run. For a solo maintainer, a mandatory second-person review
   may be impractical; define exceptions explicitly rather than locking yourself out.
6. Review collaborators and Actions permissions. The supplied CI has read-only repository permissions,
   no signing credentials, and does not publish or deploy APKs.
7. Review the [publication checklist](RELEASING.md), the complete proposed source changes and history
   before changing visibility to public. Commit/push only the reviewed changes; do not blindly stage
   local credentials, caches or personal downloads.
8. Use **Releases** for reviewed signed APKs, source tags, checksums and notices. A repository URL does
   not imply a public APK release exists.

## Local audit and history

On 2026-10-11, a read-only pattern audit inspected 1,623 existing non-ignored candidate file paths and
the 485 paths in the single revision reachable from local Git refs, before subsequent hosting files
were added. Text inspection excluded binary files and files larger than 5 MiB.
Patterns covered private-key markers, common service tokens, credential-bearing URLs, suspicious literal
password assignments, known developer device/storage identifiers and developer home paths.
No matches remained after current-source redaction. The removed identifiers/hardcoded JDK fallback were
uncommitted, not present in that local revision. Local signing/SDK files were ignored; no tracked game,
firmware or signing-file extensions were found. The Gradle wrapper JAR is an intentional build input.

This is not proof that all secrets are absent. Remote-only refs, Git LFS objects, hosting artifacts,
reflogs/unreachable objects, images/archives and unrecognized secret formats were not certified.
Audit remote history and release artifacts separately before publication. The old revision still contains
the removed GIMP projects and GPL-licensed vendor sources; if sharing history, their original notices remain
applicable. No history rewrite or force push was performed.

## Source-copy verification

The development verification uses a separate source copy selected from existing non-ignored Git
candidate paths, including reviewed but uncommitted source changes. It excludes local SDK/signing files,
build outputs and `.git`; a separate Gradle user home avoids the original dependency/build cache.
The JDK and Android SDK remain external prerequisites. This tests portable source inputs, not a remote
fresh clone or the correctness of GitHub settings. Repeat from a fresh clone after the reviewed changes
are committed and pushed.
