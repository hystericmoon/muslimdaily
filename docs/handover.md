# Handover

The one place where a session leaves what the next session needs. Read it first; update it
before you stop, or when the conversation is getting long. Keep it current, not a diary: replace
what is done, keep what is open. Facts only (PR numbers, ids, counts, decisions), each with where
to check it.

Last updated: 31 Oct 2026, evening (1.9.0 released; owner chose B for the da'if grades). All earlier Adwam
sessions were read and archived; everything still open from them is below.

## In flight (check first)

- **Da'if grades found by #102**: the owner chose B (31 Oct, decision page
  https://claude.ai/artifact/JV9nAhPh1S4QmUJ5AegmLN): the five rows (between_sujud_02,
  leave_home_02, sleep_12, illness_05, illness_06) stay on al-Albani's grade and the Sources
  sheet lists Darussalam's grade per chain under "Penilaian lain". Done by
  `tools/alt_gradings.py` (copies from `tools/hadith_numbers_sunnah.csv`, refuses anything not
  in it), content_version 35, PR claude/handover-docs-ntkjix. Shipped content: merge only after
  the owner's OK. Not released yet: add a What's New line with the next release.
- **Mac session** "Sunnah.com hadith lookups" (session_01W2ZFMuts1anPa7PAqpiUjD, on the
  owner's Mac via `claude remote-control`): did #102. Planned next, the evening of 31 Oct: the
  device passes (TalkBack T1–T12, widget §5). Cloud sessions can't message it (the platform
  refuses cloud-to-Mac messages without a trust grant); relay through the owner.
- **Hadith numbers**: 42 of the 58 empty rows filled (#99 dataset, #102 sunnah.com, both via
  `tools/hadith_numbers.py`). 16 left: books not on sunnah.com (Al-Hakim, At-Thabrani,
  Al-Baihaqi, Ibn as-Sunni, Nasa'i's 'Amal al-Yawm, part of Ahmad) and ruku_01, sujud_01,
  tashahhud_13 (no single matching hadith; see #102).
- Current session: session_01W2ZFMrUt3FG2NbnkiZDcqo (started by hand by the owner after the
  depth-8 session).

## Decisions waiting on the owner

- **Listening review** (items 3, 4, 5 below and the virtue-hadith question):
  https://claude.ai/artifact/GzK5Sa2Z61nSEtgDmZcSGy. 34 dua-part proposals (from
  `split_context.py suggest`, count-note rows left out) and 86 phrase-break proposals (from
  `segment_audio.py suggest`, status match/close/partial, rows waiting on a split left out).
  Answers are in its `reviews` collection (`ArtifactData list`), one doc per `<split|seg>__<id>`
  with `verdict` ok / fix / virtue / later, `note` and the row's `sha`. Apply only `ok` rows whose
  sha still matches; ask before merging (shipped content). Regenerate the page from
  fresh suggestions if `duas.csv` changes first. The owner will do it later (31 Oct: "gonna
  take hours"; afternoon: "I will check on that"); check the collection at the start of each
  session. The owner declined auto-shipping `match` phrase rows without listening.
  Answers so far (31 Oct, 9 rows, still in progress; checked again at the start of the
  after-1.9.0 session, still the same 9 rows): ok seg adhan_04, evening_15,
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
- **IslamHouse licence**: request sent 23 Oct (`docs/islamhouse-licence-request.md`), no reply.
  IslamHouse publishes no email address (only the 500-character contact form). Owner's choice
  (31 Oct): plan B, a Play build without the hisnmuslim.com English; see `docs/play-store.md`,
  "Plan B". It is open work item 1.
- **Cloud environment**: add haho-design under the environment's Repositories and a setup
  script (SDK, haho-design publish) so sessions skip the manual setup below. Not verified done.
- **Oppo user's widget feedback** via Diagnostics (shipped in 1.5.0): no reply recorded.

## Shipped

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

1. Plan B for Play (owner's choice): a Play build without the hisnmuslim.com English. Plan in
   `docs/play-store.md`, "Plan B". Not started.
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
6. Empty `source_1_no`: 16 rows left (see In flight). sunnah.com and dorar.net answer 403 to
   cloud sessions; a Mac session can reach sunnah.com.
7. Device passes (need the owner's phone, see Devices): `docs/talkback-checklist.md` T1–T12
   and widget QA `docs/device-qa.md` §5 (5.1–5.4c); no results recorded yet. Planned for the
   Mac session the evening of 31 Oct.
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
