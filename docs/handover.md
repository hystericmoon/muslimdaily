# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`, `motion`.

Last updated: 20 Nov 2026 (1.24.0 is the latest release; 1.25.0 is built on PR #306, waiting for the screenshots run and the owner's yes to merge).

## In flight

- **1.24.0 (versionCode 69) released** (PR #303, 20 Nov, APK built): saved places and the dhikr counter; details in history, "Shipped" 1.24.0. Owner to check on a phone that the 3 to 4 database update keeps data. Not built: a one-tap place switch on Today.
- **1.25.0 (versionCode 70) built, PR #306, NOT merged** (owner, 20 Nov: finish everything, one release). Needs the owner's yes: `versionName` and a new permission (`ACCESS_NOTIFICATION_POLICY`, documented in `docs/play-store.md`). Contents: (a) Undo for Remove in Memorize (snackbar, `ReviewRepository.remove` returns `RemovedReviews`, `restore`; the Remove dialogs on dua detail and occasion screens are unchanged) and island haptics (overlay's own View, no permission); (b) silence during prayer (Reminders row, `PrayerSilencer`, planner `SILENCE_START/END`); (c) last third of the night as a plain row on Today (`LastThird`, no explanatory text); (d) Islamic dates (`domain/fasting/IslamicDates.kt`: new year, Asyura, start of Ramadan, both Eids, Arafah; list on the Fasting screen, reminder the evening before, `ReminderType.ISLAMIC_DATE`); owner's screenshot notes of 20 Nov: Today header (place and travel chips, then three equal tiles), shorter Island row text, Island screen (preview pinned on a wallpaper with a status bar, docked Pill/Card/Alert switch, per-mode settings, own X/Y for Card and Alert that follow the pill until set, open card and alert in a full-width window with a solid status-bar band). Decision page: https://claude.ai/artifact/T8jDbmf1y3VfXkP7z26sqB. After merge: the Release workflow publishes v1.25.0; add it to history "Shipped"; say where the APK is.
- **Left out on purpose, ask the owner**: Maulid, Isra Mi'raj and Nisfu Sya'ban in Islamic dates (not sahih-only occasions); Travel mode stays a toggle chip on Today (the page showed a tag); no silencer unit test (needs the Android DND manager).
- **Owner to check on a phone (cloud can't)**: 1.25.0: does the solid band cover the status bar on the phone (it may still draw above the overlay), the full-width card window blocks touches beside the card while open, haptics on the overlay, the adhan still sounds under alarms-only, the Today tiles at 200% text, Islamic date reminder once; island swipe and tap from other apps, TalkBack Close, the arc at night and after Isya, the glow's heat and battery, 200% text; timetable share as PNG and PDF; the 1.22.0 dark accents (`docs/accent.md` has the way back); 1.16.0 to 1.20.0 items (history, "Shipped"); Qibla back overlap (#209).
- **Island follow-ups (not done)**: close on fling velocity (a flick under 48 dp springs back); a first-use nudge only if feedback says people miss the swipe (the hint was removed on purpose).
- **Prayer-time accuracy**: nothing changed. Recommendation (roadmap file): show "GPS, exact" on Today plus a dev-only eQuran comparison script.
- **LinkedIn post for 1.19**: waits for the owner's device screenshots (Build, Routine, Recall).
- Stray branches the owner deletes on GitHub: `claude/handover-1-19-0`, `claude/saved-places-dhikr-1-24-0`.
- **Other follow-ups (not done)**: delete the unreachable `GuidedSession.kt`, `GuidedStep`, tests and screenshots 163-168 (keep `advancedDuas`); Promo media is stale (history).

## Next work, in order (details and models: `docs/roadmap-2026-11.md`)

1. (a) to (d) are all in 1.25.0 (PR #306). Small ones when convenient: one-tap place switch on Today, island close on flick, delete `GuidedSession.kt`. Zakat and the missed-prayers log are dropped (owner).
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
