# Handover

The one place where a session leaves what the next session needs. Read it first; update it
before you stop, or when the conversation is getting long. Keep it current, not a diary: replace
what is done, keep what is open. Facts only (PR numbers, ids, counts, decisions), each with where
to check it.

Last updated: 31 Oct 2026, after the "Now / Next" roadmap session.

## Waiting on the owner

| PR | What | Why it waits |
|---|---|---|
| #83 | Content: the recited words alone for 6 Hisnul Muslim rows (adhan_02, marriage_02, gathering_01, hajj_05, tasbih_01, tasbih_09), after_salah_06 Indonesian | Shipped religious content. Its body lists every row left out and why |
| #85 | Accessibility Test Framework checks (`roborazzi-accessibility-check`, test only) | New dependency |

Decisions only the owner can make (details in #83):

- Hadiths about a virtue that sit in the library as duas (shalawat_01–05, salam_01/03,
  istighfar_01/02/04/05/06, illness_06, travel_09, night_03, tasbih_05): show them as virtue
  cards rather than duas?
- Evening rows that carry the morning wording, with the evening form only in the book's note
  (evening_03, evening_11, morning_18 "3× in the evening").
- Report source errors? tasbih_02 English translates the 100× hadith while the Arabic is the
  10× one; after_salah_06 English has no translation of Ayat al-Kursi; adhan_02 English
  "His salve". Also HadeethEnc no. 5491 and 6163 (from an earlier session).
- Which cue layout besides A: the A/B/C prototype is at
  https://claude.ai/artifact/7sXgDbssoA1zWgF9WeWm6F (A is built, in #84).

## In flight (merge without asking once CI is green)

- #84 Reading view: one phrase per line, tap a phrase to play from it, muted book notes.
  Main merged in after #82; its screenshots are re-recorded on CI.

Merged: #82 (Today without a location, onboarding city picker and text size, 200% fixes,
audio error message, search ranking with content DB v6).

None of these bump the version. When they are in, ship a minor release (1.7.0) with a
What's New entry; say the version before merging (it publishes a release).

## Open work, in order

Roadmap with reasons and effort: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

1. After #84: memorise in focus mode (layout C), if the owner picks it.
2. Audio ranges for the 6 rows in #83 and the ~22 clips longer than their text: needs listening
   in `build/split/review.html` (`python3 -I tools/split_context.py suggest`).
3. 58 rows with an empty `source_1_no`: the book gives no number; sunnah.com returns 403 from
   cloud sessions, so this needs a session that can reach it, or the owner.
4. Count notes inside duas (after_salah_01/04, sleep_08, fear_05, news_05) stay in the text; #84
   shows them muted. Not to be deleted.
5. Device pass: `docs/talkback-checklist.md` T1–T12 on a phone.

## Facts that cost time to find

- Content counts (main at c4d2de7): 323 rows; 87 with phrase breaks; 74 without Latin, mostly
  because unvowelled narration (صلى الله عليه وسلم) or basmalah sits in `arabic`; 17 without
  audio (sunnah_01–16, funeral_01); 52 with a grade.
- The English "( 1 )" / "[Soorah …]" leftovers are mostly verse numbers and surah headings in
  the Qur'an translations: information, not junk.
- `split_context.py` proposals are often wrong (it only drops the narrator); check every row.
  Values are cut from the source between anchors, never typed: see #83's approach.
- Empty occasions (entering_home, drinking) are already hidden in the library.

## Session setup (cloud)

- haho-design: `add_repo hystericmoon/haho-design`, then
  `git clone --depth 1 --branch v0.1.0 https://github.com/hystericmoon/haho-design ../haho-design`
  and `./gradlew publishToMavenLocal` there (version in `gradle/libs.versions.toml`).
- `export ANDROID_HOME=/opt/android-sdk`; the first Gradle build takes several minutes.
- Maven Central and sunnah.com may refuse direct `curl` (429 / 403); Gradle resolves fine.
