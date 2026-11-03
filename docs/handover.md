# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 3 Nov 2026 (#156 merged: handover split, `docs/history.md`, project skills;
#157 content gaps open, waits on the owner).

## In flight

- Main: 1.12.5 (versionCode 54, content_version 54, 329 rows).
- **PR #157** (item 0a): CI green, mergeable. Needs three answers from the owner (asked 3 Nov):
  1. leave out an unvowelled «رضي الله عنه» too (only travel_09)? If no: remove that pattern
     from `HONORIFICS` in `tools/transliterate.py` and its test (Opus: transliterator).
  2. OK to merge (shipped content)?
  3. release 1.12.6 (suggested: corrections and gap fills) or 1.13.0? Then the `release` skill
     (What's New: English for 2 duas, funeral_01 audio, Latin for 16 rows, 5 corrected).

## Waiting on the owner

- **IslamHouse licence reply** (3 Nov; summary in `docs/islamhouse-licence-request.md`,
  "Reply"; details in history, "Finished work"). Credits done (1.12.5). Still asked: the date
  it arrived. Possible follow-up: a release-checklist step to re-check sources for corrections
  (`tools/check_sources.py` already is the gate in `docs/release.md`).
- **Oppo user's widget feedback** via Diagnostics (1.5.0): no reply recorded.
- Cloud environment: decided, stay on Default (see "Session setup").

## Open work, in order

Roadmap: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

0a. **Content gaps: done in PR #157 (`claude/content-gaps`, CI green, mergeable; waits on the owner).**
   content_version 55. Counts after it: 1 row without English (manners_05: its entry's
   English is Aisha's reply about "them"; no row has its Arabic), 3 without Indonesian
   (tasbih_03, tasbih_06, salam_04: no licensed source), 16 without audio (sunnah_01-16:
   HadeethEnc has none), 39 without Latin (other unvowelled words; never hand-written).
   - eating_01, waswas_04: English verbatim from rows with the same Arabic (travel_05,
     anger_01), recorded in `clean_entries.py`.
   - funeral_01: hisn_153.opus. The API's link for entry 153 has no number
     (`audio/ar/.mp3`); `fetch_audio.audio_url` uses `audio/ar/<ID>.mp3`. Whisper: title
     «تلقين المحتضر» (0-3.8 s), then the hadith; range 5.12-12.86 set in the CSV (not in
     `start_fixes.csv`: `start` needs an existing range). `verify_audio`: 10/10.
   - Latin: unvowelled «صلى الله عليه وسلم» / «رضي الله عنه» left out (owner approved;
     رضي الله عنه added as the same kind, owner to confirm): 16 rows gain Latin. Three rule
     fixes found while checking: النَّبيُّ without its kasra (now None: tasbih_10, news_04,
     hajj_02), هَللَّهُ read as Allah (hajj_06 now None), خَيْرَاً read "khairaan" (5 shipped
     rows corrected: adhan_02, adhan_04, istiftah_03, news_02, funeral_03).
1. **On hold (owner, 1 Nov: no budget; don't start until the owner says).** Play release: the
   owner's steps only (upload keystore, four `PLAY_UPLOAD_*` secrets, Play App Signing, listing,
   content rating, target audience; "Needs doing" in `docs/play-store.md`). Play ships the same
   content as GitHub (owner chose A, 3 Nov).
6. Empty `source_1_no`: 5 rows (ruku_01, sujud_01, tashahhud_13 have no single hadith;
   travel_12/13 cite Ibn as-Sunni). Reopen only if the owner can look them up on the Mac.
7. Widget QA left: 5.4c (lock-screen widget, needs a Pixel), 5.4's legacy half, a third-party
   launcher; TalkBack T1–T12 skipped by the owner. Needs the owner's Mac (emulators) or phone;
   cloud sessions can't. Details: history, "Closed open-work items", 7.
8. ~50 merged `claude/*` branches on origin; the cloud git proxy refuses `git push --delete`.
   Suggested to the owner: turn on "Automatically delete head branches".

Not to do: the Wear OS app (owner, 31 Oct). Deleting count notes inside duas (after_salah_01/04,
sleep_08, fear_05, news_05; shown muted since 1.7.0).

## Sessions

- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or
  artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- Last sessions: session_01W2ZFMu64SGUJb7ZB37ryAj (3 Nov, Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
