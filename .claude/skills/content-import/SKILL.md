---
name: content-import
description: Import, fix or check religious content in Adwam (duas, translations, grades, sources, transliteration). Use before touching content/source/*.csv, content/*.json, tools/build_content.py, tools/transliterate.py or any importer under tools/. Opus only.
---

# Content import and checks

Needs Opus (CLAUDE.md, "Sessions"). Never hand this to a Haiku sub-agent.

## Rules (from CLAUDE.md, repeated so they're in front of you)

- Never write religious text. Arabic, translations, virtues and sources come only from
  `content/source/*.csv` via `tools/build_content.py`. Latin comes only from the fixed rules in
  `tools/transliterate.py`.
- Sahih/hasan only. Counts only where the Sunnah gives one.
- Never fix a source's text by hand. Values are cut from the source between anchors, never typed.

## Every imported row: check, then record

From `docs/content-guide.md`, "Checking imported content":

1. Grade is sahih or hasan, as the source states.
2. Not a duplicate: compare with `arabic` and `arabic_full`.
3. A dua the reader says, not an instruction about one.
4. Indonesian and English translate the same Arabic, completely.

A row that fails is left out, with the reason in the importer. Never corrected or completed.

## Where things are

- Column reference and source kinds: `docs/content-guide.md` (read only the section you need).
- Hisn importer: `tools/hisnul_muslim/` (`import_hisn.py --pdf <file> --merge` replaces the
  mapped events' rows: hand edits after the final import). Other fetchers there
  (`fetch_arabic.py`, `fetch_english.py`, `fetch_islamhouse_id.py`, `fetch_audio.py`).
- HadeethEnc: `tools/hadeethenc/`. Hadith numbers: `tools/hadith_numbers*.py/csv`.
- Text decisions: `tools/clean_entries.py` (not idempotent on its own output; `WEAK` holds rows
  left out as weak).
- Source drift: `python3 -I tools/check_sources.py` (~5 min); review each change against the row
  before `--update-baseline`.
- Reachable from cloud sessions: hadeethenc.com, islamweb.net, cdn.jsdelivr.net. Not:
  sunnah.com, dorar.net (403). More in `docs/history.md`, "Facts that cost time to find".

## Finish

- `python3 tools/build_content.py` (bumps content_version), then
  `cd tools && python3 -m unittest discover -s .`.
- Shipped content changed: ask the owner before merging (CLAUDE.md, "Workflow").
