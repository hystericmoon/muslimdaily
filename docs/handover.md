# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 6 Nov 2026 09:15 WIB (promo batch 2: widgets, reminders, Video 3, calendar done; 1.12.6 is shipped).

## In flight

- Main: 1.12.6 (versionCode 55, content_version 55, 329 rows). Promo PRs #163, #165-#171 merged.

## Promo campaign: batch 1 done, batch 2 started (6 Nov); next task, Sonnet

Batch 1 is built, QA'd and ready to post; the owner posts. Show the owner the results first.
- Review page (videos play, ID/EN switch): https://claude.ai/artifact/MTfXg3ptJcNMLLqRhVSMsj
- Full-quality media: orphan branch `promo-assets` (never merge it; keeps `main` small).
- Drive folder "Adwam promo" (private): Google Doc "Batch 1 plan and results", Sheet "Calendar".
  The Drive connector only takes inline content, so media is not in Drive.
- Source: `marketing/` (PR #163): `README.md` (how to render), `brief.md`, `copy.md`,
  `alt-text.md`, `calendar.csv`, scenes, engine. 2 videos x 4 formats (ID/EN, 9:16/4:5) + .srt,
  8 images, 2 carousels (+ PDFs). Calendar: 17 posts, Mon 9 Nov to Sat 5 Dec.
- Decisions (owner, 5 Nov): direction A (parchment, product first); memorization first; light and
  dark both shown; Arabic only as empty hidden-word boxes (option 3); silent; a voiceover, if any,
  is a male voice.
- Rules still hold: no IslamHouse/HadeethEnc material, no religious text, no faces, no streaks.
- **Notion works** (tested 6 Nov): `notion-create-pages` loads via ToolSearch. The Google Doc is
  copied to a private Notion draft: https://app.notion.com/p/40feb9fc21558177b761e849159cd6f3
  (owner can move it). Nothing owed.
- **Batch 2 (plan: `marketing/batch2.md`).** Done 6 Nov: English-UI media for batch 1 (shots 150-159,
  `PromoEnglishScreenshotTest`; `prep_screens.py` builds `-en`; scenes read them for `lang=en`);
  widgets + reminders first (owner's pick): carousels C, D, images imgW1/W2/imgR1, EN PDFs, and
  Video 3 "Di layar utama" (~22 s, 4 files, scene `v3-widgets.html`, EN shots 160-164). Captions and
  alt text in `copy.md`/`alt-text.md`. All media on `promo-assets` (8121b9f); review page updated.
  Calendar weeks 5-8 (7 Dec to 2 Jan) are in `calendar.csv` (not in the Drive Sheet, which has
  weeks 1-4). **Not done:** routines (N) needs Arabic boxes in `prep_screens.py` (111, 113, 31 show
  Arabic; 112 shows placeholder text); "how it's built" (H) image; 4 calendar slots are the owner's pick.
  Tips: stills need `build/mark.js` (`prep_mark.py`); a video takes ~15-25 min, run 2-4 in parallel.
  The Screenshots workflow does a plain `git push`: push your own commits after it finishes.

## Waiting on the owner

- **IslamHouse licence reply**: arrived ~01:00 WIB 3 Nov (recorded in `docs/islamhouse-licence-request.md`,
  "Reply"). Credits done (1.12.5). Nothing owed. Possible follow-up: a release-checklist step to
  re-check sources for corrections (`tools/check_sources.py` already is the gate in `docs/release.md`).
- **Oppo user's widget feedback** via Diagnostics (1.5.0): no reply recorded.
- Cloud environment: decided, stay on Default (see "Session setup").

## Open work, in order

Roadmap: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

0a. **Done (3 Nov, shipped in 1.12.6, #157/#159).** Gaps left: 1 row without English (manners_05: no
   row has its Arabic), 3 without Indonesian (tasbih_03, tasbih_06, salam_04: no licensed source),
   16 without audio (sunnah_01-16: HadeethEnc has none), 39 without Latin (unvowelled words; never
   hand-written). Details: history, "Shipped" 1.12.6.
1. **On hold (owner, 1 Nov: no budget; don't start until the owner says).** Play release: the
   owner's steps only (upload keystore, four `PLAY_UPLOAD_*` secrets, Play App Signing, listing,
   content rating, target audience; "Needs doing" in `docs/play-store.md`). Play ships the same
   content as GitHub (owner chose A, 3 Nov).
6. Empty `source_1_no`: 5 rows (ruku_01, sujud_01, tashahhud_13 have no single hadith;
   travel_12/13 cite Ibn as-Sunni). Reopen only if the owner can look them up on the Mac.
7. Widget QA left: 5.4c (lock-screen widget, needs a Pixel), 5.4's legacy half, a third-party
   launcher; TalkBack T1–T12 skipped by the owner. Needs the owner's Mac (emulators) or phone;
   cloud sessions can't. Details: history, "Closed open-work items", 7.
8. ~50 old merged `claude/*` branches on origin; the cloud git proxy refuses `git push --delete`.
   "Automatically delete head branches" is on (owner, 3 Nov), so new ones go away; the old ones
   stay until the owner deletes them on GitHub.

Not to do: the Wear OS app (owner, 31 Oct). Deleting count notes inside duas (after_salah_01/04,
sleep_08, fear_05, news_05; shown muted since 1.7.0).

## Sessions

- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or
  artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- Last sessions: session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1 on Opus 5-6 Nov, #163; promo plan on Sonnet 5 Nov; 3 Nov Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
