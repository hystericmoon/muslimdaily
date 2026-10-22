# Release

## Signing

Create a keystore once (keep it and its passwords outside the repo):

```bash
keytool -genkeypair -v -keystore muslimdaily-release.jks -alias muslimdaily \
  -keyalg RSA -keysize 4096 -validity 10000
```

Add `keystore.properties` in the repo root (ignored by git):

```properties
storeFile=../muslimdaily-release.jks
storePassword=…
keyAlias=muslimdaily
keyPassword=…
```

Without the file, `assembleRelease` produces an unsigned APK. For Google Play, prefer Play App
Signing and upload an AAB: `./gradlew :app:bundleRelease`.

Set the address that "Report an error" pre-fills in `gradle.properties`:
`muslimdaily.reportEmail=…`.

## Release gate

* Only `status: published` entries reach the release APK (`ContentAssetsTask`). With today's
  placeholder content the release build contains no duas and no rawatib timeline: that is
  expected.
* The build fails if a published entry lacks Arabic text, source, grade, grader, audio file or
  segments, naming each entry and field.
* Before tagging: `python3 tools/build_content.py`, `./gradlew lintDebug testDebugUnitTest
  assembleRelease`, then a TalkBack pass on a device (see accessibility.md, "Known gaps").

## Store listing notes

* Data safety: no data collected or shared. Location (approximate) is used on-device only, on
  request, and never transmitted. No account. No ads. No analytics SDKs.
* Permissions to explain: approximate location (prayer times), notifications (reminders),
  exact alarms (on-time adhan reminders; the app works without them), foreground media
  playback (background recitation and listen-only memorisation).
* Exact alarms: the app requests `SCHEDULE_EXACT_ALARM` (user-granted), not
  `USE_EXACT_ALARM`, which Play restricts to alarm and calendar apps.

## Sideload builds and the in-app updater

Debug / sideload builds (`SELF_UPDATE = true`) can update themselves from GitHub Releases;
release (store) builds have no updater and no INTERNET permission.

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Tag and push: `git tag v0.2.0 && git push origin v0.2.0`.
3. `.github/workflows/release.yml` builds the debug APK and attaches it to a GitHub release.
4. Phones check once a day on launch (Settings → Pembaruan aplikasi → Periksa otomatis), or on
   demand from that screen, then download and hand the APK to the system installer.

Notes:

* The updater reads `https://api.github.com/repos/<UPDATE_REPO>/releases/latest` without a token,
  so the repository (or a separate public releases repository, set with the Gradle property
  `muslimdaily.updateRepo`) must be **public**.
* All sideload builds are signed with the shared, non-secret `app/debug.keystore` so CI builds
  install over local ones. Android refuses an update signed with any other key.
