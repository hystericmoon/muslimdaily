# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`, `motion`.

Last updated: 20 Nov 2026 (1.24.0 released, APK https://github.com/hystericmoon/muslimdaily/releases/download/v1.24.0/adwam-1.24.0.apk; silence code parked).

## In flight

- **1.24.0 (versionCode 69) released** (PR #303, 20 Nov, APK built): saved places and the dhikr counter; details in history, "Shipped" 1.24.0. Owner to check on a phone that the 3 to 4 database update keeps data. Not built: a one-tap place switch on Today.
- **1.25.0 silence during prayer: code PARKED, not on main.** Built and compiling in commit `943a40bc` (settings `silence`/`silenceMinutes`, planner types `SILENCE_START`/`SILENCE_END`, `PrayerSilencer`: alarms-only filter, only from normal mode, restores only its own filter, `reconcile()` on every re-plan; receiver and scheduler wiring; manifest `ACCESS_NOTIFICATION_POLICY`). Reverted in the next commit so main stays clean: resume with `git cherry-pick 943a40bc` on a fresh branch. Left: Reminders screen row (switch, minutes stepper, permission card to `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`, `PermissionState.dnd`), strings in both locales, tests (planner, silencer), version 1.25.0 (versionCode 70), What's New, screenshot. Unverified until a phone: the adhan (alarm category) still sounds under alarms-only. Ask the owner at merge (new permission, versionName).
- **Owner decided (20 Nov): go with the recommendations, build them AFTER the session is cleared.** (1B) Undo for Remove in Memorize: no confirmation dialog; Remove shows a snackbar "Dihapus dari Hafalan saya" with "Urungkan" that re-inserts the saved review rows (dua and twin) from memory; stays for the system accessibility timeout, announced, 48 dp action; two strings in both locales; touches `ReviewRepository` (snapshot and restore), no schema or backup change. (2B) Island haptics: `View.performHapticFeedback` on touch-down, at the 48 dp close threshold and on close/open, behind the `haptics` setting, no VIBRATE permission, no vibration when an alert appears; unverified on an overlay until a phone. Page: https://claude.ai/artifact/UhTG7SLNgZZz6xsTFvB7x6. Owner also said skip zakat and the missed-prayers log (drop them from the plan).
- **Owner to check on a phone (cloud can't)**: island swipe and tap from other apps, TalkBack Close, the arc at night and after Isya, the glow's heat and battery, 200% text; timetable share as PNG and PDF; the 1.22.0 dark accents (`docs/accent.md` has the way back); 1.16.0 to 1.20.0 items (history, "Shipped"); Qibla back overlap (#209).
- **Island follow-ups (not done)**: close on fling velocity (a flick under 48 dp springs back); a first-use nudge only if feedback says people miss the swipe (the hint was removed on purpose).
- **Prayer-time accuracy**: nothing changed. Recommendation (roadmap file): show "GPS, exact" on Today plus a dev-only eQuran comparison script.
- **LinkedIn post for 1.19**: waits for the owner's device screenshots (Build, Routine, Recall).
- Stray branches the owner deletes on GitHub: `claude/handover-1-19-0`, `claude/saved-places-dhikr-1-24-0`.
- **Other follow-ups (not done)**: delete the unreachable `GuidedSession.kt`, `GuidedStep`, tests and screenshots 163-168 (keep `advancedDuas`); Promo media is stale (history).

## Next work, in order (details and models: `docs/roadmap-2026-11.md`)

1. Order the owner chose: (a) Undo for Remove 1B and island haptics 2B (patch or minor, Sonnet), (b) silence during prayer 1.25.0 from the parked commit, (c) last third of the night, (d) Islamic dates with reminders; small ones when convenient: one-tap place switch on Today, island close on flick, delete `GuidedSession.kt`. Zakat and the missed-prayers log are dropped (owner).
2. Not built from the accent plan: the thin progress line under the Next card (drop it if the owner prefers colours only) and the accent on calendars, "New" chip and Qibla "Aligned". Next motion: Qibla needle, screen transitions, staggered empty states, Listen highlight glide (skill `motion`).

## Promo campaign (6 Nov)

34 posts, 9 Nov to 5 Jan, built; the owner posts. Source `marketing/calendar.csv`; media on orphan branch `promo-assets`
(never merge; ask before pushing). Details and pitfalls: history, "Promo campaign (moved from handover, 14 Nov)".

## Waiting on the owner

- IslamHouse licence: reply recorded (`docs/islamhouse-licence-request.md`), credits done; nothing owed.
- **Oppo user's widget feedback** via Diagnostics (1.5.0): no reply recorded.

## Open work, in order

Roadmap: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

0a. Content gaps (1 no English, 3 no Indonesian, 16 no audio, 39 no Latin): history, "Shipped" 1.12.6.
1. **On hold (owner, 1 Nov: no budget).** Play release: the owner's steps only ("Needs doing" in `docs/play-store.md`). Play ships the same content as GitHub (owner chose A, 3 Nov).
6. Empty `source_1_no`: 5 rows (ruku_01, sujud_01, tashahhud_13 have no single hadith; travel_12/13 cite Ibn as-Sunni). Reopen only if the owner can look them up on the Mac.
7. Widget QA left (5.4c lock-screen widget on a Pixel, 5.4 legacy half, a third-party launcher; TalkBack T1-T12 skipped by the owner). History, "Closed open-work items", 7.
8. ~50 old merged `claude/*` branches on origin: the cloud proxy refuses `git push --delete`; the owner deletes them on GitHub.

Not to do: the Wear OS app (owner, 31 Oct). Deleting count notes inside duas (after_salah_01/04, sleep_08, fear_05, news_05).

## Sessions

- **Owner wants the latest APK with all new fixes, always (16 Nov)**: after each merge offer the release (version first) or the CI `apks` artifact.
- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- **No Opus budget (owner, 16 Nov)**: work CLAUDE.md routes to Opus is done on Sonnet while this holds. Ask the owner before assuming it has changed.
- **Screenshots workflow**: start it right after pushing a UI change and push nothing while it runs (its commit fails). CI on the commit you pushed shows red until the bot's "Record screenshots on CI" commit; the workflow_dispatch Android run on that commit is the real one.
- Last session: session_01W2ZFMsFLyhveJYKUstp4CY (5-20 Nov, Sonnet): 1.14.0 to 1.24.0. Older ids: history, "Sessions (archive)".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
- Rendering a design page to check it: Playwright is installed (`require('playwright')`); write the script to a file and run it from `/tmp`.
