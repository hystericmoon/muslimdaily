# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`, `motion`.

Last updated: 24 Nov 2026 (latest release 1.25.0, PR #306, APK https://github.com/hystericmoon/muslimdaily/releases/download/v1.25.0/adwam-1.25.0.apk; main also has the `:design` detach, PR #309, no release).

## In flight

- **Design revamp (owner, 24 Nov): "revamp the whole app, fresh and more intuitive". Option B chosen** (decision page https://claude.ai/artifact/NqQUUPugBmdymGfFmDEZJd). Done and merged (PR #309): step 1, haho-design v0.2.0 copied into the `:design` module (same package, no visual change, all screenshots matched on CI; details in history, "Shipped" top entry), and step 0, the audit `docs/ux-audit-2026-11.md` (8 findings, 5-task test plan). **Waiting for the owner's design ideas** (references, the feeling, what to keep or drop: glass, 8 palettes, third accent; which finding first, Today suggested). Then: foundations (tokens, type, shell), Today, then one screen per release, each with screenshots re-recorded and 200% text and TalkBack checked. Not audited yet: Run, Qibla, Mosques, Timetable, island, widgets, 200% renders. Name the version before any release merge. Smol still uses the haho-design repo; Adwam no longer follows it.
- **1.25.0 phone check: the owner's feedback has not arrived** (their message held the placeholder `[paste what you see]`). Ask once more. Items: solid band over the status bar, the full-width card window blocking touches beside the card, haptics on the overlay, adhan under alarms-only, Today tiles at 200% text, Islamic date reminder once, island swipe and tap from other apps, TalkBack Close, the arc at night and after Isya, glow heat and battery, timetable share as PNG and PDF, the 3 to 4 database update keeping data (1.24.0), the 1.22.0 dark accents (`docs/accent.md`), Qibla back overlap (#209), older items in history, "Shipped" 1.16.0 to 1.20.0. Fixes go in one patch (1.25.1) unless a feature is added.
- **Left out on purpose, ask the owner**: Maulid, Isra Mi'raj and Nisfu Sya'ban in Islamic dates (not sahih-only occasions); Travel mode stays a toggle chip on Today; no silencer unit test (needs the Android DND manager).
- **Small follow-ups (not done)**: island close on fling velocity; delete the unreachable `GuidedSession.kt`, `GuidedStep`, tests and screenshots 163-168 (keep `advancedDuas`); one-tap place switch on Today (may fold into the revamp); Promo media is stale (history). Prayer-time accuracy: show "GPS, exact" on Today plus a dev-only eQuran comparison script (roadmap file). LinkedIn post for 1.19 waits for the owner's device screenshots.
- **Owner housekeeping**: delete the secret `HAHO_DESIGN_DEPLOY_KEY` (now unused) and the stray branches `claude/handover-1-19-0`, `claude/saved-places-dhikr-1-24-0`, `claude/undo-remove-island-haptics-1-24-1`, `claude/notion-promo-batch-1-o2p0c4` (merged), plus ~50 old merged `claude/*` branches (the cloud proxy refuses `git push --delete`).

## Next work (model: Sonnet; roadmap and models in `docs/roadmap-2026-11.md`)

1. Get the owner's design ideas, turn them into a short written plan (steps above), have the owner choose, then build. Sonnet suits it (UI, a plan the owner chose). Switch to Opus only if a step touches `UserDatabase`, `BackupRepository` or religious content (none planned).
2. Then the 1.25.0 phone-check fixes. Not built from the accent plan: the thin progress line under the Next card, accent on calendars, "New" chip and Qibla "Aligned". Next motion: Qibla needle, screen transitions, staggered empty states, Listen highlight glide (skill `motion`). Zakat and the missed-prayers log are dropped (owner).

## Promo campaign (6 Nov)

34 posts, 9 Nov to 5 Jan, built; the owner posts. Source `marketing/calendar.csv`; media on orphan branch `promo-assets`
(never merge; ask before pushing). Details: history, "Promo campaign (moved from handover, 14 Nov)".

## Waiting on the owner

- IslamHouse licence: reply recorded (`docs/islamhouse-licence-request.md`), credits done.
- Oppo user's widget feedback via Diagnostics (1.5.0): no reply recorded.
- Play release on hold (no budget, 1 Nov): "Needs doing" in `docs/play-store.md`. Empty `source_1_no` (5 rows) and widget QA left (5.4c, 5.4 legacy, a third-party launcher): history, "Closed open-work items".

Not to do: the Wear OS app (owner, 31 Oct); deleting count notes inside duas (after_salah_01/04, sleep_08, fear_05, news_05).

## Sessions

- **Owner wants the latest APK with all new fixes, always (16 Nov)**: after each merge offer the release (version first) or the CI `apks` artifact.
- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only (owner, 31 Oct). Clearing routine: the `session-clear` skill.
- **No Opus budget (owner, 16 Nov)**: work CLAUDE.md routes to Opus is done on Sonnet while this holds. Ask before assuming it changed.
- **Screenshots workflow**: start it right after pushing a UI change and push nothing while it runs. CI shows red until the bot's "Record screenshots on CI" commit; the workflow_dispatch Android run on that commit is the real one.
- Last session: session_01W2ZFMsFLyhveJYKUstp4CY (5-24 Nov, Sonnet): 1.14.0 to 1.25.0, the `:design` detach, the UX audit. Older ids: history, "Sessions (archive)".

## Session setup (cloud)

- No design repo to attach any more. `export ANDROID_HOME=/opt/android-sdk`; if the SDK is missing, `bash tools/cloud_setup.sh` (~2 min, safe to re-run).
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`. Design tests: `./gradlew :design:testDebugUnitTest`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details". Rendering a design page: Playwright is installed (`require('playwright')`); write the script to a file and run it from `/tmp`.
