# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 15 Nov 2026 (#209 Qibla back fix and history restore merged; #210 Run audio dock).

## In flight

- **1.14.0 (versionCode 57) released 15 Nov WIB** with the Memorize revamp (PRs #196-#198, #200, #202; owner chose 1A-5A on
  https://claude.ai/artifact/3y4w5pvLiG7rvTxnkiDWLK; details: history, "Shipped" and "Memorize revamp PRs 1-4"). `mem_advanced`
  lives in the settings DataStore and rides in backups via the generic settings export. Owner has installed it.
- Done 14-15 Nov (Sonnet): #205, #207, #208 (history, "Finished work"). Fresh phone check of the Check row still to do.
- **Promo media is stale**: Video 1 and Carousel A show the old Memorize (shots 50-53); re-render from 150-153, 161, 165 and
  push to `promo-assets` only when the owner says (hold, 15 Nov). Copy and alt text still hold.
- **Run audio dock (#210, owner chose B on https://claude.ai/artifact/C2CocS4eNGcNrR5LS1ZX3C)**: Memorize's `Dock` below the Run
  card; collapsed = play + "Dengarkan" + status, Lainnya = phrase track, repeat, phrase skips, speed. Outside the card, so it
  never counts; swiping, restarting or closing stops audio; hidden "from memory" text blocks play; Arabic highlights the
  phrase heard. Shot 175 (open, playing); 123 shows the blocked state. To check on the phone: playback, lock-screen title
  (the run's name), Back closes the open dock, 200% font.
- **Qibla back overlap: fix merged in #209, unverified on a device**. Cause: every screen is transparent over the one
  `GlassBackdrop` (MainActivity; `HahoScreen` Scaffold is transparent in haho-design), and the shared-axis pop fades
  overlap (out 0-120 ms, in 60-320 ms); predictive back holds the seek part-way, so both screens drew on top of each other.
  Fix in `navigation/AppNavHost.kt` only: `predictivePopEnter/ExitTransition` = fade through (out 150 ms + scale to 0.9,
  then in 210 ms). No haho-design change. **Check on a phone/emulator (Android 14+, gesture nav)**: from Today open Qibla,
  swipe back slowly and hold at ~20%, ~50%, then release and cancel; expect only one screen visible at a time, no flash of
  the bare backdrop longer than a blink, cancel returns to Qibla. Repeat on one more pushed screen (dua detail) and with
  Remove animations on (instant switch). If it still overlaps, the next step is an opaque per-screen backdrop (haho-design).
- Main: 1.14.0 (versionCode 57, content_version 55, 329 rows), plus #205, #207-#210. Unreleased: #207-#210 (a minor, 1.15.0,
  when the owner wants it).

## Promo campaign (6 Nov)

34 posts, 9 Nov to 5 Jan, built; the owner posts. Source `marketing/calendar.csv`; media on orphan branch `promo-assets`
(never merge; ask before pushing). Details and pitfalls: history, "Promo campaign (moved from handover, 14 Nov)".

## Waiting on the owner

- IslamHouse licence: reply recorded (`docs/islamhouse-licence-request.md`), credits done; nothing owed.
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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-15 Nov, Sonnet: Memorize revamp PRs, 1.14.0 release, #205, #207, polish; child
  Opus session_01W2ZFMud8U4wDYpiJN2KDT6 built Guided #202; child Opus session_01W2ZFMuXRfZyDLfuS5TnzLY for the Run media bar
  and Qibla); session_01W2ZFMuXRfZyDLfuS5TnzLY (15 Nov, Opus child: #209, #210);
  session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
