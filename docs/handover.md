# Handover

The one place where a session leaves what the next session needs. Read it first; update it
before you stop, or when the conversation is getting long. Keep it current, not a diary: replace
what is done, keep what is open. Facts only (PR numbers, ids, counts, decisions), each with where
to check it.

Last updated: 31 Oct 2026, after #95 (the أَوْ fix). All earlier Adwam sessions were read and
archived; everything still open from them is below.

## Decisions waiting on the owner

- **Listening review** (items 3, 4, 5 below and the virtue-hadith question):
  https://claude.ai/artifact/GzK5Sa2Z61nSEtgDmZcSGy. 34 dua-part proposals (from
  `split_context.py suggest`, count-note rows left out) and 86 phrase-break proposals (from
  `segment_audio.py suggest`, status match/close/partial, rows waiting on a split left out).
  Answers are in its `reviews` collection (`ArtifactData list`), one doc per `<split|seg>__<id>`
  with `verdict` ok / fix / virtue / later, `note` and the row's `sha`. Apply only `ok` rows whose
  sha still matches; ask before merging (shipped content). Regenerate the page from
  fresh suggestions if `duas.csv` changes first. The owner will do it later (31 Oct: "gonna
  take hours"); check the collection at the start of each session.
  Answers so far (31 Oct, 9 rows, still in progress; checked again at the start of the
  after-#94 session, no new rows, every sha still matched): ok seg adhan_04, evening_15,
  morning_15; ok split morning_18; virtue split adhan_01; fix split morning_03 (no note);
  fix seg after_salah_05: three parts (one per surah); the proposed break puts the end of
  al-Falaq in front of the next basmalah; and drop the closing instruction بَعْدَ كُلِّ صَلاَةٍ
  ("after every prayer") from text and audio. Done: morning_13 / evening_13's trailing أَوْ
  (owner said drop it, 31 Oct): see the أَوْ PR below. Their seg proposals are stale now
  (the sha changed); regenerate them with the rest.

- **Virtue hadiths shown as duas** (shalawat_01–05, salam_01/03, istighfar_01/02/04/05/06,
  illness_06, travel_09, night_03, tasbih_05): show them as virtue cards instead? Details in
  the body of #83.
- **Evening rows with the morning wording**, the evening form only in the book's note
  (evening_03, evening_11, morning_18 "3× in the evening"). Idea offered: show the note as a
  footnote under the dua, as surahquran.com does.
- **Report source errors?** tasbih_02 English translates the 100× hadith while the Arabic is
  the 10× one; after_salah_06 English has no translation of Ayat al-Kursi; adhan_02 English
  "His salve"; HadeethEnc no. 5491 and 6163.
- **Microphone recording in Memorise** (record yourself, compare with the reciter): adds the
  `RECORD_AUDIO` permission, so the owner decides.
- **"Hafal?" above or below the Arabic** in routine recall (#79): moving it below was offered,
  not decided.
- **IslamHouse licence**: request sent 23 Oct (`docs/islamhouse-licence-request.md`), no reply
  yet. It blocks a Play Store release (`docs/play-store.md`) and decides whether the English
  Hisn text from hisnmuslim.com can stay.
- **Cloud environment**: add haho-design under the environment's Repositories and a setup
  script (SDK, haho-design publish) so sessions skip the manual setup below. Not verified done.
- **Oppo user's widget feedback** via Diagnostics (shipped in 1.5.0): no reply recorded.

## Shipped

PR (31 Oct): the PDF's footnote markers (NUL bytes plus stray dots) removed from 7 rows'
Indonesian (morning_04/06/13, evening_04/06/13, distress_05; morning_04/evening_04 and
distress_05 showed them in the main translation). `build_content.strip_markers` does it, used by
`parse_pdf.py`; the build now fails on any control character.

PR (31 Oct, after #95): morning_13 / evening_13 drop the trailing أَوْ, the "or" that
introduced the book's note "(or once, when lazy)"; the clip ends at 11.71 s (the pause after
قَدِيرٌ) instead of 14.07, which ran on into the note. `split_context.py` now drops an
"or" left in front of a dropped note. The owner approved the change; the new clip end comes
from the pauses, not from listening, so it is in the listening review's next round.

On main, not released yet (goes into the next minor): #94 Memorise Listen/Build in the focus
layout; practising missed words clears the hint level and always shows the reveal button; the
focus layout's Back/Next row no longer breaks at 200% text (it also broke on the dua page in
1.8.0). Add these to `WhatsNew.kt` with the next release.


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

1. Optional, offer at large text: in the focus layout, Next wraps under Back at 200%; it could
   sit at the line's end instead, at the cost of the counter sitting slightly off-centre at
   normal sizes. Not offered as a decision yet.
2. Make the book's `،` more visible (accent colour, larger) was offered, not chosen. Decided
   31 Oct: no ۝ in non-Qur'anic duas (the owner checked: sunnah.com and dorar.net don't use it);
   the text keeps the book's punctuation.
3. Phrase marks (`|`) for the rest: 87 of 323 rows have them. Batch 2 rows that failed the
   timing check need listening (`tools/segment_audio.py`).
4. Dua/context splits: about 39 of the original 66 proposals are unapplied; 37 Hisn rows still
   carry قَالَ / مَنْ قَالَ narration in `arabic` with no `arabic_full`. Many are the virtue
   hadiths above (owner decision first). Never ship a split without the owner's approval.
5. Audio ranges for the 6 rows in #83 and the ~22 clips longer than their text: needs listening
   in `build/split/review.html` (`python3 -I tools/split_context.py suggest`).
6. 58 rows with an empty `source_1_no`: the book gives no number; sunnah.com returns 403 from
   cloud sessions, so this needs a session that can reach it, or the owner.
7. Device passes (need the owner's phone, see Devices): `docs/talkback-checklist.md` T1–T12
   and widget QA `docs/device-qa.md` §5 (5.1–5.4c); no results recorded yet.
8. Delete the 10 merged `claude/*` branches on origin (and this session's merged ones). The
   auto-mode classifier refused `git push origin --delete`: needs the owner's go-ahead or a
   permission rule.

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
- sunnah.com and api.sunnah.com return 403 from cloud sessions (checked 31 Oct); hadeethenc.com
  answers 200.
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

- The owner's phone: Redmi Note 10 Pro on PixelOS (AOSP, not MIUI). Device QA needs a desktop
  or Remote Control session on the owner's MacBook (cloud sessions can't reach USB adb).
- Xiaomi battery manager / launcher and Samsung font checks need stock phones; Android 8–11
  checks (5.4b) need an emulator.
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
