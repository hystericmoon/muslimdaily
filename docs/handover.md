# Handover

The one place where a session leaves what the next session needs. Read it first; update it
before you stop, or when the conversation is getting long. Keep it current, not a diary: replace
what is done, keep what is open. Facts only (PR numbers, ids, counts, decisions), each with where
to check it.

Last updated: 1 Nov 2026 (PR #128: hadith numbers from islamweb.net, tasbih_09 audio, full Whisper check and 75 rows re-timed; waits on the owner because it changes shipped content; release 1.10.2 or 1.11.0 is the owner's call). All earlier Adwam
sessions were read and archived; everything still open from them is below.

## In flight (check first)

- **Content cleanup** (#113, shipped in 1.10.0): every row only what the reader says. Tools:
  `tools/clean_entries.py` (every text decision, with reasons; re-run only on a fresh main CSV,
  it is not idempotent on its own output) and `tools/align_audio.py` (wav2vec2 Arabic forced
  alignment; timings in `tools/align/word_timings.json`; the `timings` step needs a venv with
  torch, see its docstring; ~5 s a row on CPU). Review page (record):
  https://claude.ai/artifact/2Pm8Bu3GsYdhqG4YNWUZd1. It replaced the listening review
  (GzK5…). Left as they were: tasbih_09 (a #83 cut joining two runs, so no clip); 16 rows the
  aligner was unsure of keep their old audio; 87 rows that had phrases keep them.
  Update 1 Nov (PR #128, branch `claude/hadith-numbers-islamweb`): tasbih_09 has two phrases
  and the app skips the narration between them (`Dua.playableParts` in `AudioEngine`).
  **Full Whisper check of all 314 rows** (`tools/verify_audio.py`, results in
  `build/align/verify.csv`, ~2.5 h): 88 rows flagged; about 30 were real mis-cuts from the
  hisnmuslim.com recordings: the clip started on the reciter's chapter title ("دعاء ... يقول"),
  every phrase played one segment late (morning_06/evening_06, fear_06, tilawah_01,
  leave_home_01, night_01, after_salah_01 …), or a count note was read at the end ("ثلاث مرات",
  morning_02/09, evening_02/09, salam_03, witr_04). The rest were Whisper mishearing.
  `tools/recut_audio.py` re-timed **73 rows** from Whisper's word times (stored in
  `tools/align/whisper_words.json`) and the loudness; every one was re-checked by Whisper and
  passes or shows only mishearing inside the right cut. Left for a person (the tool refused, or
  can't tell): toilet_01, to_masjid_01, witr_01, fear_04, debt_01, wake_04 (clip starts on the
  title), waswas_05 (phrases shifted), shalawat_01 (the title shares five words with phrase 1),
  istiftah_05 and iftar_01 (listen). quran_16/18, dress_04, travel_01, sujud_03 are fine
  (Whisper mishearing). Whisper invents text on short or silent clips ("اشتركوا في القناة",
  "ترجمة نانسي قنقر", a word repeated over the silence): read such a flag as a mishearing.
- **Unreleased on main** (add to the next release's What's New): #122, "Hafal?" in routine
  recall is below the Arabic (owner's choice, 1 Nov), and its two buttons stack full-width at
  150% text and above (at 200% "Perlu lihat teks" used to wrap into a blob). #118 and #124 change
  only the store (`release`) build and the release workflow, nothing users of the GitHub build see.

- **Da'if grades found by #102**: the owner chose B (31 Oct, decision page
  https://claude.ai/artifact/JV9nAhPh1S4QmUJ5AegmLN): the five rows (between_sujud_02,
  leave_home_02, sleep_12, illness_05, illness_06) stay on al-Albani's grade and the Sources
  sheet lists Darussalam's grade per chain under "Penilaian lain". Done by
  `tools/alt_gradings.py` (copies from `tools/hadith_numbers_sunnah.csv`, refuses anything not
  in it), content_version 35, merged in #104, shipped in 1.10.0.
- **Plan B for Play** (open work 1): #118 merged 1 Nov with option A (owner's choice): the 5
  English-only Hisn rows are left out of the Play build. #124 (1 Nov): the release workflow
  builds the AAB (artifact `play-bundle`, unsigned until the `PLAY_UPLOAD_*` secrets exist, see
  `docs/release.md`); screenshot `143-play-hisn-english`; the old What's New Hisn-English items
  can't show in the Play build, so they stay. Checked locally: the AAB's `content/duas.json` is
  byte-identical to `content/play/duas.json`. See `docs/play-store.md`, "Plan B".
- **5 English-only Hisn rows** (eating_08, eating_09, tasbih_03, tasbih_06, salam_04): the owner
  asked (1 Nov) to find their Indonesian online. Nothing importable: HadeethEnc 5507
  (tasbih_03) has the two phrases in the other order, 4201 (tasbih_06) leaves out "Yang Maha
  Agung", 65997 / 66011 / 65345 (eating_08, eating_09, salam_04; 65345 is a different wording)
  are Arabic only so far. IslamHouse's Indonesian Hisn page (39092) is gone (404). Re-check
  HadeethEnc later, or import another licensed Indonesian edition if the owner supplies one.
- **Mac session** "Sunnah.com hadith lookups" (session_01W2ZFMuts1anPa7PAqpiUjD, on the
  owner's Mac via `claude remote-control`): did #102, the widget pass the evening of 31 Oct
  (results in `docs/device-qa.md` §5; issues #106, #107) and #116. Stopped 1 Nov. TalkBack was
  skipped at the owner's word (31 Oct). It can message cloud sessions; cloud sessions can't
  message it (the platform refuses cloud-to-Mac messages without a trust grant).
- **Hadith numbers**: 42 of the 58 empty rows filled (#99 dataset, #102 sunnah.com), 10 more on
  1 Nov from the islamweb.net library (`tools/hadith_numbers_islamweb.csv`, URL per row; it
  answers cloud sessions, sunnah.com and dorar.net don't): al-Hakim (morning_10, evening_10,
  night_01, travel_03, funeral_06), an-Nasa'i al-Kubra (wudu_04, manners_04), al-Baihaqi
  (witr_03), Ahmad 19109 (protection_02, plural wording), Abu Dawud 466 (enter_masjid_01, the
  a'udzu sentence). 7 left: ruku_01, sujud_01, tashahhud_13 (no single hadith, #102),
  morning_19/evening_19 (at-Tabarani by two chains, not in al-Mu'jam ash-Shaghir; al-Kabir and
  al-Awsat not searched), travel_12/13 (Ibn as-Sunni not on islamweb; their second source,
  al-Hakim, is no. 1883). islamweb book ids: Mustadrak 74, Musnad Ahmad 6, Baihaqi al-Kubra 71,
  Nasa'i al-Kubra 406 (its 'Amal al-Yawm is content ids 10054-11232), Tabarani Kabir 84, Awsat
  85, Saghir 86. Pages: `/ar/library/content/<book>/<id>/`; chapter id ranges are the
  `data-idfrom`/`data-idto` attributes of `index.php?page=bookcontents&bk_no=<book>`. The
  auto-mode classifier refused one bulk crawl of ~9000 pages; fetch a chapter at a time.
- Previous session: session_01W2ZFMscuG3LNhNF4PPQ8Gn (#124; archived when it started the next
  one). Before it: session_01W2ZFMtyVHfVpmCd9Vz2uc7 (1.10.1, #118, #122).
- Branch deletes: the cloud git proxy refuses `git push --delete`; the owner can turn on
  "Automatically delete head branches" in the repo settings.

## Decisions waiting on the owner

Decided 31 Oct (1.10.0): listening review, virtue hadiths (reading cards), evening wording
(footnote), source errors (fixed from the book's own data; HadeethEnc 5491/6163 were never
imported), microphone in Memorise (no), #107 (disable the old widget).

- **IslamHouse licence**: request sent 23 Oct (`docs/islamhouse-licence-request.md`), no reply.
  IslamHouse publishes no email address (only the 500-character contact form). Owner's choice
  (31 Oct): plan B, a Play build without the hisnmuslim.com English; see `docs/play-store.md`,
  "Plan B". Merged in #118 (option A, 1 Nov); the rest is open work item 1.
- **Cloud environment**: add haho-design under the environment's Repositories and a setup
  script (SDK, haho-design publish) so sessions skip the manual setup below. Not verified done.
- **Oppo user's widget feedback** via Diagnostics (shipped in 1.5.0): no reply recorded.

## Shipped

1.10.1 (versionCode 45, merged 1 Nov, #120): #116, the Android 8–11 widget no longer shows
"Problem loading widget" (no location set, "moved to Adwam" notice).

1.10.0 (versionCode 44, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.10.0):
#104 (Darussalam under "Penilaian lain"), #109 (city list during first import), #112 (one widget
entry on Android 8–11, #107), #113 (content cleanup, reading cards, footnotes, timed audio).

1.9.0 (versionCode 43, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.9.0):
#94, #96, #97, #98, #99, #102 (details below).

#97 (31 Oct): the PDF's footnote markers (NUL bytes plus stray dots) removed from 7 rows'
Indonesian (morning_04/06/13, evening_04/06/13, distress_05; morning_04/evening_04 and
distress_05 showed them in the main translation). `build_content.strip_markers` does it, used by
`parse_pdf.py`; the build now fails on any control character.

#96 (31 Oct): morning_13 / evening_13 drop the trailing أَوْ, the "or" that
introduced the book's note "(or once, when lazy)"; the clip ends at 11.71 s (the pause after
قَدِيرٌ) instead of 14.07, which ran on into the note. `split_context.py` now drops an
"or" left in front of a dropped note. The owner approved the change; the new clip end comes
from the pauses, not from listening, so it is in the listening review's next round.

In 1.9.0 with #96, #97 above: #94 Memorise Listen/Build in the focus
layout; practising missed words clears the hint level and always shows the reveal button; the
focus layout's Back/Next row no longer breaks at 200% text (it also broke on the dua page in
1.8.0).


1.8.0 (versionCode 42, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.8.0): #89 Memorise Cek marks missed
words (suggested rating, practise only those); #90 dua layouts A/B/C (`PhraseLayout`); #91 the
build drops `(( ))` before bracketed notes (8 rows, content_version 30); #92 phrase fills
(`PhraseFill`, hide while playing) and the Aa reading sheet on the dua page (`ReadingSheet`,
shared with Display settings).

1.7.0 (versionCode 41, released 30 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.7.0):
#82 Today without a location, onboarding city picker and text size, 200% fixes, audio error
message, search ranking (content DB v6); #83 six Hisnul Muslim rows reduced to the recited
words; #84 reading view (one phrase per line, tap to play from a phrase, muted book notes);
#85 ATF checks in `AccessibilityChecksTest` (add new screens to it); #86 this file; #87 release.

## Open work, in order

Roadmap with reasons and effort: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

1. Plan B for Play (owner's choice): a Play build without the hisnmuslim.com English. Code side
   done (#118, #124). Left, all the owner's: create the upload keystore and add the four
   `PLAY_UPLOAD_*` secrets, enrol in Play App Signing, store listing, content rating, target
   audience ("Needs doing" in `docs/play-store.md`).
2. Make the book's `،` more visible (accent colour, larger) was offered, not chosen. Decided
   31 Oct: no ۝ in non-Qur'anic duas (the owner checked: sunnah.com and dorar.net don't use it);
   the text keeps the book's punctuation.
3. Done in 1.10.0 (#113) and on 1 Nov (#128: tasbih_09; full Whisper check; 75 rows re-timed).
   Left: the ten rows listed under In flight, for a person to listen to.
6. Empty `source_1_no`: 7 rows left (see In flight). sunnah.com and dorar.net answer 403 to
   cloud sessions (also through the web-fetch tool); islamweb.net answers.
7. Widget pass for 1.9.0 done (31 Oct, `docs/device-qa.md` §5). Left: 5.4c (lock-screen
   widget: the Redmi's PixelOS has none, so it needs a real Pixel), 5.4's legacy-widget half, and a third-party
   launcher. All three can run on emulators on the Mac (an Android 16 QPR2+ Pixel image for
   5.4c; install 1.5.0, place the old widget, upgrade, for 5.4; Lawnchair from its GitHub
   releases for a third-party launcher), told to the owner 31 Oct. TalkBack T1–T12: skipped
   by the owner (31 Oct); a later pass belongs on `qa_api37_profile`.
   Filed from it: #106 (fixed by #109) and #107 (fixed by #112: the old receiver is disabled
   below Android 12, owner's choice); both shipped in 1.10.0. #112 verified on Android 8 (one
   picker entry, the placed widget kept working). #116 (found on `qa_api26_widget`) merged
   1 Nov. Cloud sessions can't do it: the container has no /dev/kvm (no emulator) and
   `list_computer_folders` is empty until the owner runs `claude remote-control` in the clone
   on the Mac; then a cloud session can attach it with `request_computer_folder` (checked 1 Nov).
8. Merged branches: only `claude/handover-docs-ntkjix` is left. The owner said go ahead
   (31 Oct), but the cloud git proxy refuses `git push --delete`; suggested to the owner:
   turn on "Automatically delete head branches" in the repo settings and delete the leftover
   once by hand.

Not to do: the Wear OS app (decided by the owner, 31 Oct). The module is already gone from the
repo; don't bring it back or plan Wear work.

Not to do: delete count notes inside duas (after_salah_01/04, sleep_08, fear_05, news_05);
1.7.0 shows them muted.

## Facts that cost time to find

- Content (main after 1.7.0): 323 rows; 87 with phrase breaks; 17 without audio
  (sunnah_01–16, funeral_01); 52 with a grade; rows without Latin are mostly the ones whose
  `arabic` still carries unvowelled narration (صلى الله عليه وسلم) or basmalah.
- The English "( 1 )" / "[Soorah …]" are mostly verse numbers and surah headings in the Qur'an
  translations: information, not junk.
- sunnah.com, api.sunnah.com and dorar.net return 403 from cloud sessions (checked 31 Oct);
  hadeethenc.com and cdn.jsdelivr.net (fawazahmed0/hadith-api, Arabic of the six books with
  numbers and Al-Albani grades, public domain) answer 200.
- Screenshots workflow race: it commits to the branch it checked out, so a push to the branch
  while it runs makes its push fail. Push first, then start it (or cancel and restart it).
- Phrase rendering is all in `feature/duas/components/HighlightedArabic.kt` (`PhraseLines`:
  column, flow, focus; fills and the gold active phrase). Reading settings composables are in
  `feature/settings/ReadingSettings.kt`, used by both Display settings and the dua page sheet.
- Connecting the owner's Mac: the desktop-app path "Settings → Claude Code → Remote Control"
  does not exist in the owner's app (31 Oct); `claude remote-control` in a terminal in the
  clone is the way.
- `split_context.py` proposals are often wrong (it only drops the narrator); check every row.
  Values are cut from the source between anchors, never typed: see #83's approach.
- Empty occasions (entering_home, drinking) are already hidden in the library.
- HadeethEnc (#81, from the fitrahive/dua-dhikr analysis): its grades name no grader, 4 entries
  have no hadith numbers; 8 candidates were left out (e.g. no. 2932 is an instruction).
- Routine recall: a correct recital that is not due leaves the schedule alone, so daily adhkar
  don't climb a box every day (#79).

## Devices

- The owner's phone: Redmi Note 10 Pro on PixelOS 17 (AOSP, not MIUI); runs the 1.9.0 debug
  build with exact alarms allowed (1 Nov). It has no lock-screen widgets (no switch under Lock
  screen, no hub mode under Screen saver, SystemUI's widget editor isn't exported), so 5.4c
  needs a real Pixel. Device QA needs a desktop
  or Remote Control session on the owner's MacBook (cloud sessions can't reach USB adb).
- Xiaomi battery manager / launcher and Samsung font checks need stock phones; Android 8–11
  checks (5.4b) need an emulator.
- Emulators on the Mac: set both `ANDROID_HOME` and `ANDROID_SDK_ROOT` to
  `~/Library/Android/sdk` (the Homebrew SDK has no system images). `qa_api26_widget` is
  Android 8; `adb root`, `settings put global auto_time 0` and `date MMDDhhmmYYYY.ss` set its
  clock to just before a prayer (put `auto_time` back to 1 after). Never clear data or run
  connected tests on the owner's phone.
- On the Mac: `handoff.md` in the local clone is git-excluded, and an older handover is in
  `~/Library/Application Support/Claude/side-session-notes/`; this file supersedes both.

## Sessions

- One session at a time. When its work is done, or the conversation is getting long: update
  this file, merge it, start a new cloud session on this repo with the prompt "Continue from
  docs/handover.md", and archive the finished session.

- Standing instruction from the owner (31 Oct): work on Adwam only. Don't read, archive or
  change sessions, repos or artifacts of other projects; the archiving notes below were
  one-off requests.
- 31 Oct (after #94): the owner asked to archive the other sessions. Archived Inverse
  Construction S3, Whittle S3, Drive Folder Sync S2, Speed Meter S2 and Smol; kept F052 Harbour
  (Crust, working) and OpenRef S3 (a long re-take job). Their open items are in the doc
  https://claude.ai/artifact/TnBH5bNS1iAFZNm1BtQHSP ("Pending across projects").
- 31 Oct: the owner asked for idle sessions in other projects to be archived. Archived the
  ones a later session in the same project had replaced (cRust 1.2, OpenRef S1/S2, Whittle
  S1/S2, Drive Folder Sync S1, Speed Meter S1, Inverse Construction S1/S2) and two stale
  one-offs (skills setup, installed skills list). Kept the latest session of each project.

## Session setup (cloud)

- haho-design: `add_repo hystericmoon/haho-design`, then
  `git clone --depth 1 --branch v0.1.0 https://github.com/hystericmoon/haho-design ../haho-design`
  and `./gradlew publishToMavenLocal` there (version in `gradle/libs.versions.toml`).
- `export ANDROID_HOME=/opt/android-sdk`; the first Gradle build takes several minutes.
- Maven Central and sunnah.com may refuse direct `curl` (429 / 403); Gradle resolves fine.
