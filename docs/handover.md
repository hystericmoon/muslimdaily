# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 16 Nov 2026, 10:40 WIB (1.16.0 released, session clearing).

## In flight

- **1.16.0 (versionCode 61) released 16 Nov 10:22 WIB** (release PR #279; #276 widget text mode, #278 Memorize review fixes and widget fit;
  details: history, "Shipped"). No content, database or backup change. Nothing in flight; no open PRs.
- **Owner to check on a phone or emulator (cloud can't)**:
  (1) **Widget Dark text**: Settings → Display and text → Home screen widgets, Text colour Dark, opacity ~20% over a light wallpaper (Light text over a dark one).
  Below 85% AA holds only for the matching wallpaper (mid-grey fails below ~55%); the note says so.
  (2) **Memorize** (all Sonnet, no Opus review): tap a row → start sheet (Easy = guided, Advanced = tabs, Listen mode = hands-free, Remove); the dua name
  heading and Meaning line; the Listen split button (right part opens the speeds); Advanced shows its controls with no Options toggle (Run still has one);
  menu "Switch to Listen mode" and the Easy/Advanced row; Listen mode on a long dua keeps its play button. "Easy" is my label for the guided mode, so ask the owner if it is the word they meant.
  (3) Widgets at large fonts: 56/76 dp cells and the 76 dp-wide tall cell at 1.3x and 2x (WidgetFitTest covers all texts now).
  (4) Check row, Run audio dock (Back closes the dock, 200% font), widget taps and 1x1 / 4x2 scale, and **Qibla back overlap (#209, unverified on a device;
  Android 14+, gesture nav)**: from Today open Qibla, swipe back slowly and hold at ~20% and ~50%, release and cancel; one screen at a time.
  If it still overlaps, next step is an opaque per-screen backdrop (haho-design).
- **Promo media is stale**: Video 1 and Carousel A show the old Memorize (shots 50-53); re-render from 150-153, 161, 165 and
  push to `promo-assets` only when the owner says (hold, 15 Nov). Copy and alt text still hold.
- **UX/UI audit: done** (page https://claude.ai/artifact/GXE91xqQ9eAig4qHkDhcse; fix list: history, "Shipped" 1.15.1, D10 in 1.15.2). Not done: W7 "next two
  prayers at 200%" (unreachable, see history). Before merging a batch of PRs read history, "Facts that cost time to find" (16 Nov).
- Main: 1.16.0 (versionCode 61, content_version 55, 329 rows).

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
- **No Opus budget (owner, 16 Nov)**: work CLAUDE.md routes to Opus is done on Sonnet while this holds (1.16.0 did B1, the start sheet and the mode switch that way). Ask the owner before assuming it has changed.
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-16 Nov, Sonnet: Memorize revamp, 1.14.0 to 1.16.0, audit fixes #216-#272, widget text mode #276, Memorize review fixes #278;
  child Opus sessions archived: Guided #202, #209, #210); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
