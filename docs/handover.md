# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`, `motion`.

Last updated: 19 Nov 2026 (1.23.0 PR open, waiting for the owner's yes).

## In flight

- **1.23.0 (versionCode 68), PR open, merge needs the owner's yes (versionName change)**: monthly timetable and Ramadan Imsakiyah. `domain/prayer/Timetable.kt` (pure; month or the Ramadan the date falls in or the next), `feature/timetable/` (screen from a "Jadwal" chip on Today, route `TimetableRoute`; `TimetableShare` draws PNG or one-page A4 PDF through the FileProvider `export/` cache). Owner decision 19 Nov: **no Imsak column** (Subuh ends suhur; the Methodology text stays true). Island (same PR): card and big card lose their buttons (tap opens Adwam, swipe closes; TalkBack gets activate and a Close custom action), and each gets width, text size and auto-close seconds in Island settings (`IslandSettings`, six new keys). To check on a phone: the swipe, and that a tap still opens Adwam from every app. Shots 199 (month), 200 (Ramadan), 201 (shared image) are recorded by the Screenshots workflow. Today's shots change too (new chip). At 200% text the table scrolls sideways (date column not sticky).
- **1.22.0 (versionCode 67) merged 18 Nov ~16:02 WIB (PR #300), release run 85**: third accent on Today (colours only) and the first motion pieces (shared highlight slide, mark-as-read check). The owner said "merge" with the dark accents paler than the 16 approved values (the approved ones were 3.1 to 4.2:1 on dark glass cards, AA needs 4.5; `docs/accent.md`, "Values"). APK: https://github.com/hystericmoon/muslimdaily/releases/download/v1.22.0/adwam-1.22.0.apk once the release run is done. Owner to eyeball the dark accents on a phone; if they dislike the pale look, the way back is in `docs/accent.md`.
- **Owner approved everything on 18 Nov** (free features, motion, third accent, the DND permission, the version numbers): `docs/roadmap-2026-11.md`. **Main** is 1.22.0 (versionCode 67), content_version 55, 329 rows.
- **Owner to check on a phone (cloud can't)**: island (hugs the camera, fast expand, 10-minute card, each Island settings slider, survives screen off, reboot, battery killer); 1.18.0 to 1.19.0 (glass bar, auto-scroll, Build, Routine mark row, Recall, Listen only, full-height Reading sheet); 1.17.0 and 1.16.0 (start sheet, repeat modes, widget fits and taps); Qibla back overlap (#209, Android 14+ gesture nav, if still overlapping: opaque per-screen backdrop in haho-design). New in 1.22.0: accent colours in all 8 palettes (Navy and Lavender greens 35 degrees apart), the highlight slide at a prayer change, the check draw.
- **Prayer-time accuracy**: nothing changed. The owner never said where they saw "province based". Recommendation (page in the roadmap file): show "GPS, exact" on Today plus a dev-only eQuran comparison script.
- **LinkedIn post for 1.19**: waits for the owner's device screenshots of Build, Routine and Recall; Arabic unblurred. Site: https://hystericmoon.github.io/muslimdaily/.
- Stray branch `claude/handover-1-19-0`: the owner deletes it on GitHub.
- **Follow-ups (not done)**: delete the unreachable `GuidedSession.kt`, `GuidedStep`, their tests and screenshots 163-168 (keep the stored `advancedDuas` flag); Undo for Remove needs an owner decision (restores a review schedule). Promo media is stale (Video 1 and Carousel A show the old Memorize; re-render from shots 150-153, 161, 165, push to `promo-assets` only when the owner says). Not done: W7 "next two prayers at 200%" (unreachable, see history).

## Next work, in order (details and models: `docs/roadmap-2026-11.md`)

1. **Next: saved places and the dhikr counter (1.24.0)**, after 1.23.0 merges (free features below). Not built from the accent plan: the thin progress line under the Next card (drop it if the owner prefers colours only) and the accent on the island, calendars, "New" chip and Qibla "Aligned" (one screen per PR). Next motion: Qibla needle, screen transitions, staggered empty states, Listen highlight glide (skill `motion`).
2. Free features order: timetable and Imsakiyah (1.23.0, done, PR open), saved places and dhikr counter (1.24.0), silence during prayer (1.25.0, needs the DND permission, approved), then last third of the night and Islamic dates. Zakat (needs a source) and the missed-prayers log (database) wait for sourcing or a design; ask at merge.

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
7. Widget QA left (5.4c lock-screen widget on a Pixel, 5.4 legacy half, a third-party launcher; TalkBack T1-T12 skipped by the owner): needs the owner's Mac or phone. History, "Closed open-work items", 7.
8. ~50 old merged `claude/*` branches on origin: the cloud proxy refuses `git push --delete`; the owner deletes them on GitHub (new ones auto-delete).

Not to do: the Wear OS app (owner, 31 Oct). Deleting count notes inside duas (after_salah_01/04,
sleep_08, fear_05, news_05; shown muted since 1.7.0).

## Sessions

- **Owner wants the latest APK with all new fixes, always (16 Nov)**: after each merge offer the release (version first) or the CI `apks` artifact. Main is 1.22.0.
- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or
  artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- **No Opus budget (owner, 16 Nov)**: work CLAUDE.md routes to Opus is done on Sonnet while this holds (1.16.0 and 1.17.0 did it that way). Ask the owner before assuming it has changed.
- Last session: session_01W2ZFMsFLyhveJYKUstp4CY (5-18 Nov, Sonnet; the owner cleared it for weekly usage). Older ids and what each did: history, "Sessions (archive)".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
