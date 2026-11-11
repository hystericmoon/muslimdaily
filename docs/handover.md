# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 11 Nov 2026 (screenshot-review UI pass in flight; promo done; 1.12.6 shipped; next: owner posts from 9 Nov).

## In flight

- Screenshot review (owner, 11 Nov, Sonnet): review page https://claude.ai/artifact/HhVZ1irubHyv8ZpEX6ob2v. Done in the PR
  `claude/notion-promo-batch-1-o2p0c4`: Memorize is now a toggle (adds to My hifz, never starts a session) in the occasion
  card header and on the dua detail source row, with an Undo snackbar; compact player bar with previous/next phrase (focus
  layout keeps only the "n / m" count when audio exists); Isha ends show a countdown on the nearest end and "Ended" after;
  Today timeline: the rail dot becomes the check and the whole row is the target; Run card: previous/next sit in line with
  the tap hint. NOT done, needs the owner: (1) "Start reading" only on chosen packs (needs a flag in `content/*.json`, so
  an owner OK and which packs); (2) back-transition overlap on Qibla (predictive back; screens are transparent over the
  window backdrop, needs an emulator to verify); (3) a media bar inside Run (no audio there today, a feature for Opus);
  (4) wrapping date heading on Today. Screenshots workflow must be run on the branch after the push.

- Main: 1.12.6 (versionCode 55, content_version 55, 329 rows). Promo PRs #163, #165-#181 and #183 (Notion tools allowed in `.claude/settings.json`) merged; no PR open.

## Promo campaign: built, scheduled, waiting to be posted (6 Nov)

Batches 1 and 2 are done: 34 posts, Mon 9 Nov to Tue 5 Jan, every slot has media, copy and alt text.
The owner posts; nothing has been posted. Details of batch 2: history, "Promo batch 2".
- Plan, results and calendar live in Notion (the Drive Doc and Sheets were trashed 6 Nov, restorable
  from Drive's trash): plan https://app.notion.com/p/40feb9fc21558177b761e849159cd6f3, calendar database
  https://app.notion.com/p/06ba9cf0b4664b45b94264815533ad04. `marketing/calendar.csv` is the source of truth.
  Calendar columns added 6 Nov (Thumbnail 7 Nov): Media link (GitHub raw link on `promo-assets`, folder link for
  Instagram carousels), Copy, Alt text, Extra files (.srt, slide names); filled for all 34 rows from
  `copy.md`/`alt-text.md` verbatim. Update them in Notion if copy changes (property key is `Media link`,
  no `userDefined:` prefix). Links open only while signed in to GitHub (private repo).
- Media: orphan branch `promo-assets` (never merge it; head 15d6316) and the review page
  https://claude.ai/artifact/MTfXg3ptJcNMLLqRhVSMsj (private; the owner shares it).
- Source: `marketing/` (`README.md` how to render, `brief.md`, `batch2.md`, `copy.md`, `alt-text.md`,
  scenes, engine). Last four slots: imgP1, imgP2, imgT1, imgD1 (26, 28, 31 Dec, 2 Jan; ID only).
- LinkedIn video thumbnails (7 Nov, #185): v1 `images/tV1-en.png`, v2 `images/tV2-en.png` (scene ids
  `tV1`/`tV2` in `stills.html`: covers without swipe line and counter), v3 existing `imgW1-en.png`.
  Linked in the Notion `Thumbnail` column (URL) for weeks 1, 3, 5.
- Decisions (owner, 5 Nov): direction A (parchment, product first); memorization first; light and
  dark both shown; Arabic only as empty hidden-word boxes; silent; any voiceover is a male voice.
- Rules: no IslamHouse/HadeethEnc material, no religious text, no faces, no streaks. Ask the owner
  before pushing to `promo-assets` (the owner said "always allow" on 6 Nov for pushes, review page,
  Notion and Drive updates of this campaign).
- Pitfall: the Screenshots workflow does a plain `git push`: push your own commits after it ends.
- Possible next: a LinkedIn slot after 5 Jan, English versions of the four open-slot stills, a batch 3.

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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-7 Nov Sonnet: promo batch 2, #165-#181; Notion copy/links, #183; LinkedIn thumbnails, #185-#187); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1 on Opus 5-6 Nov, #163; promo plan on Sonnet 5 Nov; 3 Nov Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
