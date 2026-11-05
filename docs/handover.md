# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 5 Nov 2026 22:30 WIB (promo campaign planned, nothing built; 1.12.6 is the shipped version).

## In flight

- Main: 1.12.6 (versionCode 55, content_version 55, 329 rows). #157 and #159 merged 3 Nov; PR #157 answers: honorific
  left out, release 1.12.6. Nothing else in flight.

## Promo campaign (owner, 5 Nov; next task, Opus)

Free LinkedIn + Instagram videos, images and copy for Adwam. The owner gives no creative
direction: the session owns concept, copy, visuals, QA. Rules from the owner:
- No music; natural sounds or narration are fine. No faces, no awrah. Everything free.
- **No IslamHouse material**: its licence (Reply in `docs/islamhouse-licence-request.md`) covers
  the app only, not promo, and HadeethEnc is the same family. So no dua, hadith, verse or
  recitation anywhere, and no screenshot showing a dua card. Never write religious text.
- Use: Adwam mark/palette (`docs/brand/`), screens without dua text (onboarding, prayer times,
  widgets, library categories, settings, dark, large text, tablet), claims the app proves (free,
  offline, no ads/tracking, open source, WCAG AA). No streaks/points language.
- Plan: brief and four-week calendar, copy (Indonesian first, English for LinkedIn), render HTML
  scenes with Chromium + ffmpeg (both installed), captions burned in plus subtitle file, alt text,
  QA by a fresh reviewer agent, no sourced religious text in any frame.
- First batch: 8 images, 2 carousels, 2 videos; then the rest of the calendar.
- Deliver: files in `marketing/` on a branch (kept out of the APK) with a PR; videos and images
  to a private Google Drive folder "Adwam promo"; plan and results as a Google Doc there. Notion:
  connected, but this session lacked its page tools (create/update/fetch/search). Tool lists load
  at session start: check with ToolSearch; if present, write the plan and results to Notion.
- Run as director: the Opus session decides concept, copy and what ships; sub-agents do the rest
  (`model: "sonnet"` for rendering, ffmpeg, uploads and the fresh-eyes QA review; `"haiku"` for bulk
  resizing/renaming). Never hand an agent sourced religious text or user data.
- Don't post anything; the owner posts.

## Waiting on the owner

- **IslamHouse licence reply**: arrived ~01:00 WIB 3 Nov (recorded in `docs/islamhouse-licence-request.md`,
  "Reply"). Credits done (1.12.5). Nothing owed. Possible follow-up: a release-checklist step to
  re-check sources for corrections (`tools/check_sources.py` already is the gate in `docs/release.md`).
- **Oppo user's widget feedback** via Diagnostics (1.5.0): no reply recorded.
- Cloud environment: decided, stay on Default (see "Session setup").

## Open work, in order

Roadmap: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

0a. **Done (3 Nov, shipped in 1.12.6, #157/#159).** Gaps left: 1 row without English (manners_05: no
   row has its Arabic), 3 without Indonesian (tasbih_03, tasbih_06, salam_04: no licensed source),
   16 without audio (sunnah_01-16: HadeethEnc has none), 39 without Latin (unvowelled words; never
   hand-written). Details: history, "Shipped" 1.12.6.
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
- Last sessions: session_01W2ZFMu64SGUJb7ZB37ryAj (3-5 Nov; promo plan on Sonnet 5 Nov; 3 Nov Opus: 1.12.3, #145, #146, 1.12.4;
  #156 token saving; #157 content gaps). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
