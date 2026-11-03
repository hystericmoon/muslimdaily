---
name: release
description: Ship an Adwam release or version bump (versionCode/versionName, What's New, release gate). Use when asked to release, bump the version, or prepare a stable or beta build.
---

# Release

Sonnet is enough when the contents are known.

1. Version: stable by default, minor for features, patch for fixes; beta (`-beta.N`, a
   pre-release) only when the owner asks. Tell the owner the version before merging.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Highlights: an entry in `feature/whatsnew/WhatsNew.kt` and a `whats_new_*` string array in
   both `values/strings.xml` (Indonesian) and `values-en/strings.xml`.
4. Gate (`docs/release.md`, "Release gate"): content touched → `python3 tools/build_content.py`
   and `python3 -I tools/check_sources.py`; then
   `./gradlew :app:lintDebug :app:testDebugUnitTest`.
5. UI changed → run the Screenshots workflow on the branch right after pushing (push first:
   a push during the run makes its commit fail).
6. PR to main. A `versionName` change publishes a GitHub Release on merge, so ask the owner
   before merging (CLAUDE.md, "Workflow").
7. After merge: add the release to `docs/history.md`, "Shipped" (version, versionCode, date in
   WIB, PR, one line of contents), and update the handover.

Signing, sideload updater and the old package id: `docs/release.md`. Beta: `docs/beta.md`.
