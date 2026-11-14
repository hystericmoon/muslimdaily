# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 14 Nov 2026 (1.13.0 published; Memorize decision page up, waiting on the owner's picks).

## In flight

- 1.13.0 (versionCode 56) is live: GitHub Release v1.13.0 published 11 Nov 23:38 WIB with adwam-1.13.0.apk
  (checked 14 Nov). Owner still to install and check. Details: history, "Shipped".
  Review page https://claude.ai/artifact/HhVZ1irubHyv8ZpEX6ob2v, media bar concepts https://claude.ai/artifact/Q2C54rfAhT6May8h3kuEsA.
- **Next, Opus: Memorize tab revamp** (owner, 14 Nov; nothing built, no PR). Owner's complaints: (1) Memorize list cards badly
  laid out; (2) in the dua's Memorize view, nav should dock at the bottom with each tab's actions in a collapsed docked sheet
  above it; (3) Build tab, Repeat A to B: picking phrases is very hard; (4) Recall tab is messy. Owner asked whether to build
  two levels (easy + the current 4-step advanced). Sonnet's position, not yet decided by the owner: yes, as one flow:
  Guided (default: listen, read with hidden words, test yourself, one task per screen) over the same engine, plus Advanced
  (today's 4 tabs) switchable per dua and remembered; compact list with Continue first and a plain-text state per dua;
  A-B picker as 48dp phrase chips, tap start then tap end, no dragging (WCAG 2.5.7); Recall one prompt, Reveal, Got it/Again.
  No streaks, points or badges. **Decision page (Opus, 14 Nov): https://claude.ai/artifact/3y4w5pvLiG7rvTxnkiDWLK**,
  five decisions, recommended 1A 2A 3A 4A 5A (compact list with Continue; docked nav + collapsible sheet; A-B by tapping phrase
  rows; one Recall card with levels under More; Guided default + per-dua Advanced switch in DataStore). Waiting for the owner's
  picks. Then PRs in order: list, docked layout, A-B, Recall (Sonnet can do these), Guided (Opus: session flow, check backup).
- Open, needs the owner (Opus or the owner's Mac): (1) Qibla back-transition overlap (predictive back over transparent
  screens; needs an emulator, may touch haho-design); (2) media bar inside Run (no audio there; Opus feature; concepts page above).
- Main: 1.13.0 (versionCode 56, content_version 55, 329 rows). No other PR open.

## Promo campaign: built, scheduled, waiting to be posted (6 Nov)

34 posts, Mon 9 Nov to Tue 5 Jan, all with media, copy and alt text. The owner posts; nothing posted yet.
Source of truth `marketing/calendar.csv` (Notion calendar mirrors it). Media: orphan branch `promo-assets`
(never merge; ask before pushing, except the campaign's pushes the owner always allowed). Rules: no
IslamHouse/HadeethEnc material, no religious text, no faces, no streaks. Pitfall: the Screenshots workflow
does a plain `git push`; push your own commits after it ends. Links, decisions, thumbnails: history,
"Promo campaign (moved from handover, 14 Nov)".

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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-14 Nov Sonnet: Memorize revamp proposal 14 Nov, no code; 1.13.0 release #193; promo batch 2, #165-#181; Notion copy/links, #183; LinkedIn thumbnails, #185-#187; screenshot review #188; Today heading #191; sequential flag #192); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1 on Opus 5-6 Nov, #163; promo plan on Sonnet 5 Nov; 3 Nov Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
