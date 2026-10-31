# Handover

The one place where a session leaves what the next session needs. Read it first; update it
before you stop, or when the conversation is getting long. Keep it current, not a diary: replace
what is done, keep what is open. Facts only (PR numbers, ids, counts, decisions), each with where
to check it.

Last updated: 31 Oct 2026, after 1.7.0 shipped. All earlier Adwam sessions were read and
archived; everything still open from them is below.

## Decisions waiting on the owner

- **Virtue hadiths shown as duas** (shalawat_01–05, salam_01/03, istighfar_01/02/04/05/06,
  illness_06, travel_09, night_03, tasbih_05): show them as virtue cards instead? Details in
  the body of #83.
- **Evening rows with the morning wording**, the evening form only in the book's note
  (evening_03, evening_11, morning_18 "3× in the evening").
- **Report source errors?** tasbih_02 English translates the 100× hadith while the Arabic is
  the 10× one; after_salah_06 English has no translation of Ayat al-Kursi; adhan_02 English
  "His salve"; HadeethEnc no. 5491 and 6163.
- **Cue layout besides A** (A shipped in 1.7.0): prototype at
  https://claude.ai/artifact/7sXgDbssoA1zWgF9WeWm6F. C is the candidate for memorising.
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

1.7.0 (versionCode 41, released 30 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.7.0):
#82 Today without a location, onboarding city picker and text size, 200% fixes, audio error
message, search ranking (content DB v6); #83 six Hisnul Muslim rows reduced to the recited
words; #84 reading view (one phrase per line, tap to play from a phrase, muted book notes);
#85 ATF checks in `AccessibilityChecksTest` (add new screens to it); #86 this file; #87 release.

## Open work, in order

Roadmap with reasons and effort: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

1. Memorise in focus mode (layout C), once the owner picks it.
2. Mark missed words in Memorise's Cek step (the next Memorise idea after #79/#80).
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
8. Delete the 9 merged `claude/*` branches left on origin (and this session's merged ones).

Not to do: delete count notes inside duas (after_salah_01/04, sleep_08, fear_05, news_05);
1.7.0 shows them muted.

## Facts that cost time to find

- Content (main after 1.7.0): 323 rows; 87 with phrase breaks; 17 without audio
  (sunnah_01–16, funeral_01); 52 with a grade; rows without Latin are mostly the ones whose
  `arabic` still carries unvowelled narration (صلى الله عليه وسلم) or basmalah.
- The English "( 1 )" / "[Soorah …]" are mostly verse numbers and surah headings in the Qur'an
  translations: information, not junk.
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

## Session setup (cloud)

- haho-design: `add_repo hystericmoon/haho-design`, then
  `git clone --depth 1 --branch v0.1.0 https://github.com/hystericmoon/haho-design ../haho-design`
  and `./gradlew publishToMavenLocal` there (version in `gradle/libs.versions.toml`).
- `export ANDROID_HOME=/opt/android-sdk`; the first Gradle build takes several minutes.
- Maven Central and sunnah.com may refuse direct `curl` (429 / 403); Gradle resolves fine.
