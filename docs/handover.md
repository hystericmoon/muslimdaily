# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 14 Nov 2026 (Memorize revamp PRs 1-3 merged; PR 4 Sonnet, PR 5 Opus).

## In flight

- 1.13.0 is live (Release v1.13.0, 11 Nov); owner still to install and check. History, "Shipped".
  Review page https://claude.ai/artifact/HhVZ1irubHyv8ZpEX6ob2v, media bar concepts https://claude.ai/artifact/Q2C54rfAhT6May8h3kuEsA.
- **Memorize revamp: owner chose 1A 2A 3A 4A 5A (14 Nov)** on https://claude.ai/artifact/3y4w5pvLiG7rvTxnkiDWLK (the
  mockups are the spec; read the page before each PR). Done and merged: PR 1 list #196, PR 2 dock #197, PR 3 A-B #198
  (details: history, "Memorize revamp PRs 1-3"). Next, one PR each, Screenshots workflow right after each push, in
  `feature/memorize/session/MemorizeSessionScreen.kt` (fill `recallDock` / `recallMore`; `Dock` helpers exist):
  - **PR 4 (Sonnet), Recall (4A):** title line as cue with Meaning on tap (shrink `CueCard`); gapped text; collapsed dock =
    "Hints · 60% hidden" (text, not only boxes), Next word, Done reciting; expanded = hide level, order, first letters, Play
    with gaps (already split that way by PR 2; add the "Hints" line and the cue). Check tab stays as is.
  - **PR 5 (Opus), Guided (5A):** default per dua, Advanced switch in the overflow, remembered per dua in DataStore (decide
    whether it belongs in backup); steps Listen > Repeat after it > Read with gaps (ladder sets level) > Test yourself
    (Reveal, tap missed words, Knew it / With help / Not yet = Easy / Hard / Again); reviews open on Test yourself.
  Ship together as 1.14.0 once all five are merged (ask the owner first). Open question for the owner: dock the Check
  ratings too? (PR 2 left Check without a dock.)
- Open, needs the owner (Opus or the owner's Mac): (1) Qibla back-transition overlap (predictive back over transparent
  screens; needs an emulator, may touch haho-design); (2) media bar inside Run (no audio there; Opus feature; concepts page above).
- Main: 1.13.0 (versionCode 56, content_version 55, 329 rows). No PR open after #198.

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

0a. Content gaps (1 no English, 3 no Indonesian, 16 no audio, 39 no Latin): history, "Shipped" 1.12.6.
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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-14 Nov; 14 Nov Opus: Memorize decision page #195, list #196; Sonnet: dock #197, A-B #198; Sonnet: Memorize revamp proposal 14 Nov; 1.13.0 release #193; promo batch 2, #165-#181; Notion copy/links, #183; LinkedIn thumbnails, #185-#187; screenshot review #188; Today heading #191; sequential flag #192); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1 on Opus 5-6 Nov, #163; promo plan on Sonnet 5 Nov; 3 Nov Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
