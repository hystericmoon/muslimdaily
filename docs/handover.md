# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`, `motion`.

Last updated: 20 Nov 2026 (1.23.0 released; 1.24.0 built, PR open for the owner's yes).

## In flight

- **1.23.0 is released** (run 86 green, APK https://github.com/hystericmoon/muslimdaily/releases/download/v1.23.0/adwam-1.23.0.apk, 40 MB, published 20 Nov 09:59 WIB). Details: history, "Shipped" 1.23.0.
- **1.24.0 (versionCode 69), PR open, waits for the owner's yes** (changes `UserDatabase`, `BackupRepository`, `versionName`): saved places (Room v3 to v4 auto-migration, `app/schemas/.../4.json`, table `saved_places`; backup VERSION 4, a v3 file restores with no places, test `BackupRepositoryTest`) and the dhikr counter (DataStore `dhikr_count`/`dhikr_target`, so the existing settings backup carries them; Today chip "Penghitung"). Home and Work hold one place each, Other many with the user's name; the active place is the one whose coordinates match the current location (nothing stored); "Gunakan lokasi saya" is the follow-GPS path. Not built: a one-tap switch on Today itself (switch is on Settings, Location: open, tap). Screenshots 192-194 are new; run the Screenshots workflow after each push.
- **Owner to check on a phone (cloud can't)**: island swipe and tap from other apps, TalkBack Close, the arc at night and after Isya, the glow's heat and battery, 200% text; timetable share as PNG and PDF; the 1.22.0 dark accents (`docs/accent.md` has the way back); 1.16.0 to 1.20.0 items (history, "Shipped"); Qibla back overlap (#209).
- **Island follow-ups (not done)**: close on fling velocity (a flick under 48 dp springs back); a first-use nudge only if feedback says people miss the swipe (the hint was removed on purpose); vibration needs the VIBRATE permission, not approved.
- **Prayer-time accuracy**: nothing changed. Recommendation (roadmap file): show "GPS, exact" on Today plus a dev-only eQuran comparison script.
- **LinkedIn post for 1.19**: waits for the owner's device screenshots (Build, Routine, Recall).
- Stray branch `claude/handover-1-19-0`: the owner deletes it on GitHub.
- **Other follow-ups (not done)**: delete the unreachable `GuidedSession.kt`, `GuidedStep`, tests and screenshots 163-168 (keep `advancedDuas`); Undo for Remove needs an owner decision. Promo media is stale (history).

## Next work, in order (details and models: `docs/roadmap-2026-11.md`)

1. **1.24.0 merge** (owner's yes), then silence during prayer (1.25.0, needs the DND permission, approved), last third of the night, Islamic dates. Zakat (needs a source) and the missed-prayers log (database) wait for sourcing or a design; ask at merge.
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
- Last session: session_01W2ZFMsFLyhveJYKUstp4CY (5-20 Nov, Sonnet): 1.14.0 to 1.23.0. Older ids: history, "Sessions (archive)".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
- Rendering a design page to check it: Playwright is installed (`require('playwright')`); write the script to a file and run it from `/tmp`.
