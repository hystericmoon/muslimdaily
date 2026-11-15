# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 15 Nov 2026, 22:10 WIB (UX/UI audit fixes: 12 merged, T2 open; decision page for M1/M3/M10; owner clearing).

## In flight

- **1.15.0 (versionCode 58) released 15 Nov WIB** (release PR #211; details: history, "Shipped"). No content, database or backup change.
- **Owner to check on a phone or emulator (cloud can't)**: (1) Check row: one-row dock, Indonesian labels on one line, 200% font
  stacks; (2) Run audio dock: playback, lock-screen title (the run's name), Back closes the open dock, 200% font; (3) **Qibla
  back overlap (#209, fix unverified on a device; Android 14+, gesture nav)**: from Today open Qibla, swipe back slowly and hold
  at ~20% and ~50%, then release and cancel; expect one screen at a time, cancel returns to Qibla; repeat on dua detail and
  with Remove animations on. If it still overlaps, next step is an opaque per-screen backdrop (haho-design).
- **Promo media is stale**: Video 1 and Carousel A show the old Memorize (shots 50-53); re-render from 150-153, 161, 165 and
  push to `promo-assets` only when the owner says (hold, 15 Nov). Copy and alt text still hold.
- **UX/UI audit fixes IN PROGRESS (Sonnet, from 15 Nov 18:30 WIB).** Page: https://claude.ai/artifact/GXE91xqQ9eAig4qHkDhcse
  (2 Blocker, 11 Major, 33 Minor). Owner's picks: the rec option on every finding, order B1, B2, M1-M11, then Minors; one small PR
  each, Screenshots workflow after each UI change; ask before any PR that needs a haho-design release.
  **Merged:** B1 #216, B2 #217, M2 #219, M4 #220, M3 #221 (partly), M5 #222, M6 #223, M7 #230, M8 #224, M9 #225, M10 #226, M11 #227 (docs #229).
  **Open:** T2 #231 (Today icons scale with the font; shared `smallIcon` is haho-design, untouched). Main was merged into it (M7 and
  T2 both re-recorded the Ramadan/Isbat 200% shots) and the Screenshots workflow re-run on the merge. Merge on a green dispatched
  Android run once shots 14, 15, 101 `-font200` show the darker crescent and scaled icons.
  **Resume (owner, 15 Nov night: "clear, then you resume and finish all of them"):** the remaining Minors in the page's order, two
  streams in parallel, one PR at a time inside each (merge before the next branch; same-screen PRs conflict on PNGs). Stream A
  Widgets: W2, W4, W5, W7, W9 (`PrayerWidgets.kt`). Stream B Settings: S7, S9, S10, S11. Then T3, T6, T7, T8, T10, S4, S6, S14, S15,
  R4, R6, R9-R15, R18, D6-D8. Skip and note any that need a haho-design release.
  **Held for the owner, one decision page:** https://claude.ai/artifact/VpwqEFFuCvQdiPEzi9Lqcj (rec marked): M1 glass floor (rec A:
  floor 85%; over white, muted text passes 4.5:1 only from 83%, a black tint only gets 73%), M3 remainder (rec A: pin Hafal /
  Perlu lihat teks in the dock at 200%), M10 scope (rec C: word clamp on card translation and routine-picker rows too, as a
  follow-up to #226). Build the owner's pick; if they say "all recommended", A, A, C.
  **Loose ends** (history, "Facts that cost time to find", audit session 2): routine-editor hint says "drag the handle" (R10 copy pass); `WidgetFitTest` flags 56/76 dp cells at large fonts; Qibla tip draws raw gold.
- Main: 1.15.0 (versionCode 58, content_version 55, 329 rows).

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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-15 Nov, Sonnet: Memorize revamp, 1.14.0 and 1.15.0, audit fixes #216-#227;
  child Opus sessions archived: Guided #202, #209, #210); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
