# Play Store readiness

The app ships today as a sideload build from GitHub Releases. This page is what a Play release
would need, so the decision can be made with the costs in view. Nothing here is done yet unless
it says so.

## What already fits

- **Release build is offline.** `app/src/main/AndroidManifest.xml` has no `INTERNET`; only the
  debug (sideload) manifest adds `INTERNET` and `REQUEST_INSTALL_PACKAGES` for the updater, and
  `SELF_UPDATE` is false in release. Play forbids self-updating apps, so this split is required.
- **Target SDK** is current (compileSdk 37); Play's yearly target requirement is met.
- **No ads, accounts, analytics or data collection**, which keeps the Data safety form short.
- **Privacy policy** text is in [privacy-policy.md](privacy-policy.md); it needs a public URL
  (GitHub Pages or the repository's rendered file both work).

## Needs doing

| Item | Work | Notes |
|---|---|---|
| Upload key + Play App Signing | Create an upload keystore (`keystore.properties`, see release.md) and enrol in Play App Signing. | The GitHub build uses the shared debug key, so Play and GitHub installs can't update each other. Say so on the listing. |
| App bundle | `./gradlew :app:bundleRelease` in the release workflow. | Play requires AAB. |
| Store listing | Short and full description in Indonesian and English, 8 phone screenshots (take them from `docs/screenshots`), a 1024×500 feature graphic. | |
| Content rating | IARC questionnaire: no violence, no user content, no purchases. | Expected: Everyone / 3+. |
| Target audience | 13+ is the simple choice; "includes children" pulls in the Families policy. | |

## Permission declarations

Play reviews these against its policies. The answers below are what the code actually does.

| Permission | Why | Policy notes |
|---|---|---|
| `SCHEDULE_EXACT_ALARM` | Prayer-time reminders at the exact minute. | Play restricts `USE_EXACT_ALARM` to alarm/calendar apps; the app uses `SCHEDULE_EXACT_ALARM`, which the user grants, and falls back to inexact (10-minute window) alarms when denied. Declare it under "Exact alarm" with the reminders as the core use. |
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Prayer times and qibla (coarse); nearby mosques while that screen is open (fine). | Foreground only, no background location, so no background-location declaration is needed. |
| `POST_NOTIFICATIONS` | Reminders the user turns on. | Requested at the moment a reminder is switched on. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Playing dua audio with the screen off. | Declare the media-playback foreground service type with a short video of audio playback, if asked. |
| `POST_PROMOTED_NOTIFICATIONS` | The optional next-prayer Live Update on Android 16. | Off by default; user turns it on. |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` | Re-arm reminders after a reboot; keep audio playing. | No declaration. |

## Data safety form (draft answers)

- Does the app collect or share user data? **No.**
- Location: processed on device only, not collected (Play's definition of "collected" is data
  leaving the device).
- Is data encrypted in transit? Not applicable (no transmission).
- Can users request deletion? Yes: clearing app data or uninstalling removes everything.

## Plan B: Play without the hisnmuslim.com English (owner's choice, 31 Oct)

IslamHouse hasn't answered the licence request (23 Oct). The audio and the Indonesian rest on
IslamHouse's published FAQ (republish unchanged, with credit), as in the GitHub build. The
English Hisn text from hisnmuslim.com names no translator or licence, so the Play build leaves
it out until IslamHouse answers:

1. Done: `build_content.py` also writes `content/play/duas.json`, the same rows with the
   English of every Hisn al-Muslim row emptied (`translations.en`, `translations_full.en`; the
   rows are the ones whose `notes` start "Hisnul Muslim", which includes fasting_01's English
   from IslamHouse's 2025 edition). Qur'an and HadeethEnc English stay (47 rows). The 5 Hisn
   rows that have only English (eating_08, eating_09, tasbih_03, tasbih_06, salam_04: no
   Indonesian at any source) are left out of the Play copy, since they would show the Arabic
   alone. CI's "content is up to date" check covers the file.
2. Done, without a flavor: the `release` build type is already the store build (offline, no
   updater), and GitHub ships `sideload`. `ContentAssetsTask.duasFile` makes `release` package
   `play/duas.json` as `content/duas.json`; debug and sideload keep `duas.json`. A flavor would
   have doubled every variant and renamed the CI tasks for no gain. Still to add: AAB in the
   release workflow (see "Needs doing").
3. Checked in code: with English chosen, a row with no English shows its Indonesian
   (`DuaTranslation` in `DuaComponents.kt`; the "Lihat teks lengkap" section in
   `DuaDetailScreen.kt` does the same). Search, Memorise and the widget don't read the
   English. Not yet: a screenshot test of a Hisn row with English chosen on the Play content.
4. Done: `src/release/res/values*/strings.xml` override `methodology_reviewer`: no
   hisnmuslim.com English credit, and a line saying Hisn duas show the Indonesian in English.
   Open: the What's New history (three items from 1.4.0 on about the Hisn English) still ships in
   the Play build; drop or override those items before the first Play release.

If IslamHouse says yes, delete the flavor's content step; if they say no, apply step 1 to the
GitHub build too.

## Risks to plan for

- **Religious content review.** Play has no special policy, but Hisn al-Muslim audio and text
  licences should be confirmed in writing first: see [islamhouse-licence-request.md](islamhouse-licence-request.md).
- **Mosque data.** OpenStreetMap ODbL attribution is in About; keep it there in the Play build.
- **Two channels.** If both GitHub and Play builds exist, they are different signers. Decide whether
  the GitHub build continues; if it does, keep its updater pointing only at GitHub.
