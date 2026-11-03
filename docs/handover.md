# Handover

What the next session needs, and nothing else. Read it first; update it before you stop, or
when the conversation is getting long. Keep it under ~6 KB: replace what is done, keep what is
open, facts only (PR numbers, ids, counts, decisions), each with where to check it. Finished
work, release details and hard-won facts go to `docs/history.md` (search it by row id, PR or
tool when a task touches them; don't read it end to end). Repeated procedures live in project
skills (`.claude/skills/`): `content-import`, `audio-recut`, `release`, `session-clear`.

Last updated: 3 Nov 2026 (1.12.5 released, #152; handover split into this file and
`docs/history.md`, project skills added; next: Opus, content gaps, item 0a).

## In flight

- Nothing open on main after 1.12.5 (versionCode 54, content_version 54, 329 rows).
- `claude/token-saving`: this handover split, `docs/history.md`, the four project skills,
  CLAUDE.md shortened (Release and the clearing routine point at skills). Docs only.

## Waiting on the owner

- **IslamHouse licence reply** (3 Nov; summary in `docs/islamhouse-licence-request.md`,
  "Reply"; details in history, "Finished work"). Credits done (1.12.5). Still asked: the date
  it arrived. Possible follow-up: a release-checklist step to re-check sources for corrections
  (`tools/check_sources.py` already is the gate in `docs/release.md`).
- **Oppo user's widget feedback** via Diagnostics (1.5.0): no reply recorded.
- Cloud environment: decided, stay on Default (see "Session setup").

## Open work, in order

Roadmap: https://claude.ai/artifact/12tBkTW16bxrFGE9tEuz73

0a. **Next (model: Opus): content gaps** (owner asked "can we fix those?", 3 Nov; counted on main,
   content_version 54, 329 rows, from `content/duas.json`). Arabic: none missing. Audio file on
   disk for every row that names one. Gaps:
   - English missing (3): eating_01, waswas_04, manners_05 (Indonesian present). Not yet checked
     whether their source has English: read the source rows first; import only what a source gives.
   - Indonesian missing (3): tasbih_03, tasbih_06, salam_04 (English present). Already searched
     (history, "English-only Hisn rows"): no importable source; only fixable if the owner
     supplies a licensed Indonesian edition.
   - No audio (17): funeral_01 (a Hisn row: check IslamHouse's Hisn audio for entry 153) and
     sunnah_01-16 (HadeethEnc, which has no audio; generated recitation is ruled out). Likely
     stays as is unless a licensed recording exists.
   - No Latin (55): by design, `tools/transliterate.py` skips rows whose Arabic has unvowelled
     words or inline narration (e.g. صلى الله عليه وسلم); only 2 of the 55 have no vowel marks at
     all, the rest were not inspected one by one. Never hand-edit the Arabic or write Latin. A fix
     is a fixed rule in the transliterator. APPROVED by the owner, 3 Nov: the transliterator may
     skip inline narration such as صلى الله عليه وسلم and transliterate the rest of the row by the
     same fixed rules (still never hand-written Latin, and never an edit to the Arabic). Rows
     with other unvowelled words stay without Latin. Check each newly transliterated row against
     its Arabic, and add tests in `tools/`.
   Rows with audio but no phrase timing (118) are not a gap: they play the whole clip.
   Owner's order (3 Nov): the 3 English, then funeral_01's audio, then the narration rule.
1. **On hold (owner, 1 Nov: no budget; don't start until the owner says).** Play release: the
   owner's steps only (upload keystore, four `PLAY_UPLOAD_*` secrets, Play App Signing, listing,
   content rating, target audience; "Needs doing" in `docs/play-store.md`). Play ships the same
   content as GitHub (owner chose A, 3 Nov).
6. Empty `source_1_no`: 5 rows (ruku_01, sujud_01, tashahhud_13 have no single hadith;
   travel_12/13 cite Ibn as-Sunni). Reopen only if the owner can look them up on the Mac.
7. Widget QA left: 5.4c (lock-screen widget, needs a Pixel), 5.4's legacy half, a third-party
   launcher; TalkBack T1–T12 skipped by the owner. Needs the owner's Mac (emulators) or phone;
   cloud sessions can't. Details: history, "Closed open-work items", 7.
8. ~50 merged `claude/*` branches on origin; the cloud git proxy refuses `git push --delete`.
   Suggested to the owner: turn on "Automatically delete head branches".

Not to do: the Wear OS app (owner, 31 Oct). Deleting count notes inside duas (after_salah_01/04,
sleep_08, fear_05, news_05; shown muted since 1.7.0).

## Sessions

- Give every time to the owner in WIB (UTC+7); tool results print UTC.
- Work on Adwam only: don't read, archive or change other projects' sessions, repos or
  artifacts (owner, 31 Oct).
- Clearing routine: the `session-clear` skill.
- Last sessions: session_01W2ZFMu64SGUJb7ZB37ryAj (3 Nov, Opus: 1.12.3, #145, #146, 1.12.4;
  then this split). Older ids in history, "Finished work".

## Session setup (cloud)

- Check `ls ~/.m2/repository/id/haho/haho-design/` for the version `gradle/libs.versions.toml`
  pins. Missing: `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh`
  (~2 min, safe to re-run). `export ANDROID_HOME=/opt/android-sdk`.
- Content tests run from `tools/`: `cd tools && python3 -m unittest discover -s .`.
- Whisper venv, Gradle and proxy quirks: history, "Cloud setup, details".
