# Muslim Daily

Native Android app `id.haho.muslimdaily` (plus a Wear OS module in `wear/`). Kotlin 2.3,
Compose, Hilt, Room, DataStore, Glance. Sideloaded through GitHub Releases.

## Content rules (non-negotiable)

- Never write or generate religious content: Arabic, translations, virtues or sources come only
  from `content/source/*.csv` via `tools/build_content.py`. See `docs/content-guide.md`.
- Sahih/hasan narrations only. Show a count only where the Sunnah gives one.
- No gamification (streaks, points, badges). Fully offline. WCAG AA.

## Build and test

```
export ANDROID_HOME=/opt/android-sdk
python -m unittest discover -s tools          # content pipeline
./gradlew :app:lintDebug :app:testDebugUnitTest :wear:lintDebug :wear:testDebugUnitTest
./gradlew :app:recordRoborazziDebug           # refresh docs/screenshots after UI changes
```

- Screenshot tests run on Robolectric pinned to Indonesian (`id-rID`); new shots are
  numbered 100+ in `RoutineScreenshotTest`.
- Every new string goes in both `values/strings.xml` (Indonesian, default) and
  `values-en/strings.xml`. Lint fails on unused resources.
- Icons are generated: add to `tools/icons/build_icons.py`, don't hand-write drawables.
- `UserDatabase` changes need a Room auto-migration and the exported schema in `app/schemas/`.
  Data the user creates must also go into `BackupRepository` (bump its `VERSION`, keep old
  backups restorable).

## Release

- Bump `versionCode` and `versionName` in `app/build.gradle.kts`; merging to main publishes a
  GitHub Release when `versionName` changes. A name with `-` (e.g. `0.6.1-beta.1`) is a
  pre-release for the beta channel. See `docs/release.md`, `docs/beta.md`.
- Add the release's highlights to `feature/whatsnew/WhatsNew.kt` and a `whats_new_*` array.
- Feedback/report email: `muslimdaily.reportEmail` in `gradle.properties`.

## Workflow

Work on a branch, run lint and tests, push, open a PR to main, merge once CI is green.

More: `docs/architecture.md`, `docs/design-tokens.md`, `docs/accessibility.md`, `docs/fonts.md`.
