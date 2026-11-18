# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 16 Nov 2026, 19:50 WIB (1.19.0 released; prayer-time accuracy question open).

## In flight

- **1.19.0 (versionCode 64) released 16 Nov 18:23 WIB** (release PR #292, feature PR #291; details: history, "Shipped"). **Owner to check on a phone (cloud can't)**: glass bar blur and icon contrast over scrolling text, auto-scroll on next/previous/audio, Build part and repeat, the Routine mark row and one-time hint, Recall "Next".
- 1.18.0 (versionCode 63, 16 Nov) shipped the standard player bar in Memorize and Listen only; its phone checks (Listen only skip, Build repeat, stacked Recall row at large font, full-height Reading sheet) are still the owner's, with 1.19.0's.
- **Prayer-time accuracy (owner asked 16 Nov, nothing changed)**: times are computed from coordinates (adhan + Kemenag preset), no API, no internet permission. GPS is exact; the manual picker has 484 Indonesian places (GeoNames pop >= 5,000). eQuran API (Kemenag data per kab/kota, needs internet): not adopted. Ask where the owner saw "province based". Options: show "exact GPS" on Today; kecamatan in `tools/build_cities.py` (Opus: data); coordinates entry; dev-only comparison script against eQuran.
- **LinkedIn post for 1.19 (owner posts)**: text, dark image and carousel lived in the scratchpad (lost on clear). The owner wants the Arabic unblurred; Build, Routine and Recall slides need real device screenshots. Site: https://hystericmoon.github.io/muslimdaily/.
- **Island (owner asked twice, 18 Nov: "like the Dynamic Island")**: branch `claude/notion-promo-batch-1-o2p0c4`, PR open. Overlay pill at the camera cutout (`island/IslandService.kt`, `IslandOverlay.kt`, `IslandModel.kt`), Settings, Reminders, "Island (beta)", off by default. Adds `SYSTEM_ALERT_WINDOW` and `FOREGROUND_SERVICE_SPECIAL_USE`: **needs the owner's yes before merge**, and the owner must test placement, expand/collapse and kill-by-OEM on a phone (cloud can't). Opens a big card 10 min before each prayer (once per prayer, 60 s or Close). Proposed version 1.20.0. The Live Update chip already exists ("Next prayer on lock screen"), so it is not new work. Next ideas: Listen controls, after-salah shortcut (decision page in this session's artifacts, lost on clear).
- Stray branch `claude/handover-1-19-0`: the owner deletes it on GitHub.
- **Earlier checks still open (owner, on a phone; cloud can't)**: 1.17.0 start sheet and list rows, Listen repeat modes, docks collapsed by default; widget Dark text and large-font fits (1.16.0), widget taps,
  **Qibla back overlap (#209)**: Android 14+, gesture nav, swipe back slowly at ~20% and ~50%; if it still overlaps, next is an opaque per-screen backdrop (haho-design). 1.17.0 details: history, "Shipped".
- **Follow-ups from the s3 review (not done)**: delete the unreachable
  `GuidedSession.kt`, `GuidedStep`, their tests and screenshots 163-168 (the stored `advancedDuas` flag stays for backups); Undo for Remove needs Opus or an owner decision (restores a review schedule).
- **Promo media is stale**: Video 1 and Carousel A show the old Memorize (shots 50-53); re-render from 150-153, 161, 165 and
  push to `promo-assets` only when the owner says (hold, 15 Nov). Copy and alt text still hold.
- UX/UI audit done (history, "Shipped" 1.15.1, 1.15.2). Not done: W7 "next two prayers at 200%" (unreachable, see history). Before merging a batch of PRs read history, "Facts that cost time to find".
- Main: 1.19.0 (versionCode 64, content_version 55, 329 rows).

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

- **Owner wants the latest APK with all new fixes, always (16 Nov)**: after each merge offer the release (version first) or the CI `apks` artifact. Main is 1.19.0.
- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or
  artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- **No Opus budget (owner, 16 Nov)**: work CLAUDE.md routes to Opus is done on Sonnet while this holds (1.16.0 and 1.17.0 did it that way). Ask the owner before assuming it has changed.
- Last sessions: session_01W2ZFMsFLyhveJYKUstp4CY (5-16 Nov, Sonnet: Memorize revamp, 1.14.0 to 1.17.0, audit fixes #216-#272, widget text mode #276, Memorize review fixes #278, s3 review #282-#284;
  child Opus sessions archived: Guided #202, #209, #210); session_01W2ZFMu64SGUJb7ZB37ryAj (3-6 Nov; promo batch 1). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
