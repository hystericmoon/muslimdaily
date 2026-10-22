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

* Every entry in `content/*.json` ships (there is no draft state). `ContentAssetsTask` fails
  the build if an entry has placeholder Arabic, no source, a grade without its grader, or
  names an audio file that does not exist, naming each entry and field.
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

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and push.
2. `.github/workflows/release.yml` sees a version with no release yet, builds the debug APK,
   and creates the `vX.Y.Z` tag and GitHub release with the APK attached. (Pushing a `vX.Y.Z`
   tag yourself, or running the workflow by hand, does the same.)
3. Phones check once a day on launch (Settings → Pembaruan aplikasi → Periksa otomatis), or on
   demand from that screen, then download and hand the APK to the system installer.

Notes:

* The updater reads `https://api.github.com/repos/<UPDATE_REPO>/releases/latest` without a token,
  so the repository (or a separate public releases repository, set with the Gradle property
  `muslimdaily.updateRepo`) must be **public**.
* All sideload builds are signed with the shared, non-secret `app/debug.keystore` so CI builds
  install over local ones. Android refuses an update signed with any other key.

## Wear OS tile

Each release also attaches `muslimdaily-wear-<version>.apk`. It has the same package name and
signing key as the phone's sideload build, which the Wear data layer requires. Install it on
the watch with adb over Wi-Fi (Settings → Developer options → Wireless debugging on the
watch):

```bash
adb pair <watch-ip>:<pairing-port>     # code shown on the watch
adb connect <watch-ip>:<port>
adb -s <watch-ip>:<port> install muslimdaily-wear-<version>.apk
```

Then add the "Shalat berikutnya" tile on the watch. The phone sends a week of prayer times
whenever its widgets refresh (at each prayer time and on settings changes); opening the phone
app once after installing is enough to fill the tile. The phone's in-app updater ignores the
watch APK.

