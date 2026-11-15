# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 15 Nov 2026 (1.15.0 released; UX/UI audit plan confirmed, to run in a fresh Opus session).

## In flight

- **1.15.0 (versionCode 58) released 15 Nov WIB**, release PR #211: #205 ("Buka kata"), #207 Check ratings in one row (owner
  chose C), #208 plain rating card and honest "Belum", #209 Qibla back fix, #210 Run audio dock (owner chose B). Details: history,
  "Shipped" and "Run audio and predictive back". No content, database or backup change. Nothing in flight, no PR open.
- **Owner to check on a phone or emulator (cloud can't)**: (1) Check row: one-row dock, Indonesian labels on one line, 200% font
  stacks; (2) Run audio dock: playback, lock-screen title (the run's name), Back closes the open dock, 200% font; (3) **Qibla
  back overlap (#209, fix unverified on a device; Android 14+, gesture nav)**: from Today open Qibla, swipe back slowly and hold
  at ~20% and ~50%, then release and cancel; expect one screen at a time, cancel returns to Qibla; repeat on dua detail and
  with Remove animations on. If it still overlaps, next step is an opaque per-screen backdrop (haho-design).
- **Promo media is stale**: Video 1 and Carousel A show the old Memorize (shots 50-53); re-render from 150-153, 161, 165 and
  push to `promo-assets` only when the owner says (hold, 15 Nov). Copy and alt text still hold.
- **UX/UI audit: PLAN CONFIRMED by the owner 15 Nov; start it in a fresh Opus session (fixes after it are Sonnet).** Scope: the
  screens in `docs/screenshots` (375 PNGs, about 100 distinct screens; light, dark, `-font200`, `-en`; the earlier "160" count
  is unreconciled, so step 0 reconciles it and lists gaps) plus all widget sizes, measured against `docs/accessibility.md` and
  `docs/design-tokens.md`. Steps: (0) inventory: map the PNGs to the 5 areas. (1) Read-only: read Impeccable's `critique` and
  `audit` text (github.com/pbakaus/impeccable) and fold its hierarchy and clarity criteria into the reviewer brief; install
  nothing, no third-party skill (owner chose this after comparing UI UX Pro Max, Taste/anti-slop, Android UX, Compose Audit,
  Mobile UX Auditor: none fits, since they target generation or web). (2) Five reviewers in parallel, **sub-agents on Sonnet**
  (owner chose; the Opus main session does the judgment): Today and onboarding; Duas; Run, routines and Memorize; Settings,
  mosque, Qibla and fasting; widgets. Each views light, dark and font200 of every screen, reads the matching code, measures
  contrast from pixels, and returns claims as {screen, shot file, what, WCAG criterion}. (3) Opus re-opens the shot for every
  claim and drops what it cannot see; mark any unconfirmed one "unverified". (4) One visual findings page (Artifact; ranked
  Blocker / Major / Minor; screenshot crop, 2-3 fixes with a recommendation marked, accessibility flagged with the WCAG SC).
  (5) Stop; the owner picks fixes; small Sonnet PRs, run the Screenshots workflow after each UI change. A first pass on 15 Nov
  was discarded (about 60 s per reviewer, large coverage gaps): reuse none of its claims. Confirmed by eye: fasting calendar at
  200% font (legend one letter per line, Hijri digits clipped: `96-fasting-month-font200`) and onboarding at 200% (sticky
  "Mulai" covers the last line, small steppers: `100-onboarding-font200`). Code is clean on literal sp, unlabelled icon buttons
  and small touch targets; to verify: the fasting legend dot uses raw gold `#C9A227` (about 2.3:1 on parchment).
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
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-15 Nov, Sonnet: Memorize revamp PRs, 1.14.0 and 1.15.0 releases, #205, #207,
  #208; child Opus session_01W2ZFMud8U4wDYpiJN2KDT6 built Guided #202, archived 15 Nov; child Opus session_01W2ZFMuXRfZyDLfuS5TnzLY,
  15 Nov: #209, #210); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
