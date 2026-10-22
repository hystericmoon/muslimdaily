#!/usr/bin/env python3
"""Builds the app's content files from the owner's reviewed spreadsheets.

    python3 tools/build_content.py            # CSV -> content/duas.json + content/rawatib.json
    python3 tools/build_content.py --check    # validate the existing JSON only

Input (CSV export of the spreadsheet, UTF-8):
    content/source/duas.csv      one row per dua / dhikr
    content/source/rawatib.csv   one row per fard or sunnah rawatib entry

Every row ships. Rules enforced (see docs/content-guide.md):
  * Unique id, known category/event, integer order, count (if given) a positive whole number.
  * Real Arabic text, Indonesian translation and at least one source. No placeholders.
  * Grade is optional; if given it must be sahih or hasan and name its grader.
  * Audio is optional; if a file is named it must exist in content/audio, and segments (if
    given) must match the phrases.
  * Phrase boundaries are marked in the Arabic column with "|". They become `phrase_breaks`
    (character offsets into the stored Arabic).

Exits with status 1 on any error, so CI and release builds stop.

The content version increments automatically whenever the generated JSON changes.
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"

GRADES = {"sahih", "hasan"}  # Scope: authentic Sunnah only.
PRAYERS = {"FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA", "JUMUAH", "ANY"}
POSITIONS = {"BEFORE", "AFTER", "FARD"}
TIERS = {"MUAKKADAH", "GHAIRU_MUAKKADAH"}
YES = {"ya", "y", "yes", "true", "1"}
NO = {"tidak", "t", "no", "n", "false", "0", ""}
PLACEHOLDER = "[PLACEHOLDER"
PHRASE_MARK = "|"


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    def problem(self, message: str) -> None:
        self.errors.append(message)

    @property
    def ok(self) -> bool:
        return not self.errors


def parse_bool(value: str, where: str, report: Report, strict: bool) -> bool:
    v = (value or "").strip().lower()
    if v in YES:
        return True
    if v in NO:
        return False
    (report.errors if strict else report.warnings).append(f"{where}: '{value}' is not ya/tidak")
    return False


def split_phrases(marked: str) -> tuple[str, list[int]]:
    """Removes '|' markers and returns (arabic, phrase_breaks).

    phrase_breaks are offsets in the returned text where phrases 2..n start.
    """
    parts = [p.strip() for p in marked.split(PHRASE_MARK)]
    parts = [p for p in parts if p]
    text, breaks = "", []
    for i, part in enumerate(parts):
        if i:
            text += " "
            breaks.append(len(text))
        text += part
    return text, breaks


def parse_segments(value: str) -> list[list[float]]:
    """'0.0-1.8; 1.8-3.9' -> [[0.0, 1.8], [1.8, 3.9]]."""
    segments = []
    for chunk in (value or "").split(";"):
        chunk = chunk.strip()
        if not chunk:
            continue
        start, end = chunk.split("-")
        segments.append([round(float(start), 3), round(float(end), 3)])
    return segments


def load_taxonomy() -> dict[str, list[str]]:
    """Category id -> event ids, both in display order."""
    data = json.loads((CONTENT / "taxonomy.json").read_text(encoding="utf-8"))
    cats = sorted(data["categories"], key=lambda c: c["order"])
    return {c["id"]: [e["id"] for e in sorted(c["events"], key=lambda e: e["order"])] for c in cats}


def is_blank_or_placeholder(value: str | None) -> bool:
    return not value or not value.strip() or PLACEHOLDER in value


def validate_dua(d: dict, taxonomy: dict[str, list[str]], audio_dir: Path, report: Report) -> None:
    """Validates one dua entry in its JSON shape. Shared by CSV build and --check."""
    did = d.get("id") or "?"
    p = lambda msg: report.problem(f"{did}: {msg}")  # noqa: E731

    if d.get("category") not in taxonomy:
        report.errors.append(f"{did}: unknown category '{d.get('category')}'")
    elif d.get("event") not in taxonomy[d["category"]]:
        report.errors.append(f"{did}: unknown event '{d.get('event')}' for category '{d['category']}'")
    count = d.get("count")
    if count is not None and (not isinstance(count, int) or count < 1):
        report.errors.append(f"{did}: count must be a positive whole number or blank")
    grade = (d.get("grading") or {}).get("grade", "")
    if grade and grade not in GRADES:
        report.errors.append(f"{did}: grade '{grade}' is outside scope (only sahih or hasan)")
    for alt in d.get("alt_gradings", []):
        if not alt.get("grader"):
            p("alternate grading without grader")

    if is_blank_or_placeholder(d.get("arabic")):
        p("Arabic text is missing or a placeholder")
    if is_blank_or_placeholder(d.get("translations", {}).get("id")):
        p("Indonesian translation is missing or a placeholder")
    if not any((s.get("book") or "").strip() for s in d.get("sources", [])):
        p("at least one source (book) is required")
    if grade and not (d.get("grading") or {}).get("grader"):
        p("a grade needs its grader")
    if not grade and (d.get("grading") or {}).get("grader"):
        p("grader given without a grade")
    virtue = d.get("virtue") or {}
    if virtue.get("text") and not virtue.get("source"):
        p("virtue text needs its source")

    audio = d.get("audio") or {}
    segments = audio.get("segments") or []
    if audio.get("file") and not (audio_dir / audio["file"]).exists():
        p(f"audio file content/audio/{audio['file']} not found")
    if segments and not audio.get("file"):
        p("audio segments given without an audio file")
    last_end = 0.0
    for i, (start, end) in enumerate(segments):
        if start < last_end - 1e-6 or end <= start:
            report.errors.append(f"{did}: segment {i + 1} ({start}-{end}) overlaps or is out of order")
        last_end = end
    phrases = len(d.get("phrase_breaks", [])) + 1
    if segments and phrases != len(segments):
        report.errors.append(f"{did}: {phrases} phrase(s) marked with '|' but {len(segments)} audio segment(s)")
    breaks = d.get("phrase_breaks", [])
    if breaks != sorted(set(breaks)) or any(b <= 0 or b >= len(d.get("arabic", "")) for b in breaks):
        report.errors.append(f"{did}: phrase_breaks must be increasing offsets inside the Arabic text")


def dua_from_row(row: dict, line: int, report: Report) -> dict | None:
    rid = (row.get("id") or "").strip()
    if not rid:
        report.errors.append(f"duas.csv line {line}: id is empty")
        return None
    try:
        order = int(row.get("order") or 0)
    except ValueError:
        report.errors.append(f"{rid}: order '{row.get('order')}' is not a whole number")
        order = 0
    count_raw = (row.get("count") or "").strip()
    count = None
    if count_raw:
        try:
            count = int(count_raw)
        except ValueError:
            report.errors.append(f"{rid}: count '{count_raw}' is not a whole number")
    try:
        segments = parse_segments(row.get("audio_segments", ""))
    except ValueError:
        report.errors.append(f"{rid}: audio_segments must look like '0.0-1.8; 1.8-3.9'")
        segments = []
    arabic, breaks = split_phrases(row.get("arabic", ""))
    sources = []
    for n in (1, 2, 3):
        book = (row.get(f"source_{n}_book") or "").strip()
        no = (row.get(f"source_{n}_no") or "").strip()
        if book or no:
            sources.append({"book": book, "no": no})
    alt = []
    for chunk in (row.get("alt_gradings") or "").split(";"):
        if chunk.strip():
            grader, _, grade = chunk.partition(":")
            alt.append({"grade": grade.strip().lower(), "grader": grader.strip()})
    virtue_text = (row.get("virtue_text") or "").strip()
    return {
        "id": rid,
        "category": (row.get("category") or "").strip(),
        "event": (row.get("event") or "").strip(),
        "order": order,
        "variant_group": (row.get("variant_group") or "").strip() or None,
        "arabic": arabic,
        "is_quran": parse_bool(row.get("is_quran", ""), f"{rid}: is_quran", report, True),
        "translations": {
            "id": (row.get("translation_id") or "").strip(),
            "en": (row.get("translation_en") or "").strip(),
        },
        "transliteration": (row.get("transliteration") or "").strip(),
        "count": count,
        "sources": sources,
        "reference": (row.get("reference") or "").strip(),
        "grading": {"grade": (row.get("grade") or "").strip().lower(), "grader": (row.get("grader") or "").strip()},
        "alt_gradings": alt,
        "virtue": {"text": virtue_text, "source": (row.get("virtue_source") or "").strip()} if virtue_text else None,
        "audio": {"file": (row.get("audio_file") or "").strip(), "segments": segments},
        "phrase_breaks": breaks,
    }


def rawatib_from_row(row: dict, line: int, report: Report) -> tuple[str, dict] | None:
    rid = (row.get("id") or "").strip()
    if not rid:
        report.errors.append(f"rawatib.csv line {line}: id is empty")
        return None
    prayer = (row.get("prayer") or "").strip().upper()
    position = (row.get("position") or "").strip().upper()
    if prayer not in PRAYERS:
        report.errors.append(f"{rid}: prayer must be one of {sorted(PRAYERS)}")
    if position not in POSITIONS:
        report.errors.append(f"{rid}: position must be one of {sorted(POSITIONS)}")
    rakah_raw = (row.get("rakah") or "").strip()
    rakah = int(rakah_raw) if rakah_raw.isdigit() else None
    if rakah_raw and rakah is None:
        report.errors.append(f"{rid}: rakah '{rakah_raw}' is not a whole number")
    if position == "FARD":
        if rakah is None:
            report.errors.append(f"{rid}: fard entries need a rak'ah count")
        return "fard", {"id": rid, "prayer": prayer, "rakah": rakah}
    tier = (row.get("tier") or "").strip().upper()
    if tier not in TIERS:
        report.errors.append(f"{rid}: tier must be MUAKKADAH or GHAIRU_MUAKKADAH")
    evidence = []
    for n in (1, 2):
        book = (row.get(f"evidence_{n}_book") or "").strip()
        no = (row.get(f"evidence_{n}_no") or "").strip()
        if book or no:
            evidence.append({"book": book, "no": no})
    notes = (row.get("notes") or "").strip() or None
    if notes and PLACEHOLDER in notes:
        report.errors.append(f"{rid}: notes still contain a placeholder")
    return "rawatib", {
        "id": rid,
        "prayer": None if prayer == "ANY" else prayer,
        "position": position,
        "rakah": rakah,
        "tier": tier,
        "evidence": evidence,
        "notes": notes,
        "hidden_when_travelling": parse_bool(row.get("hidden_when_travelling", ""), f"{rid}: hidden_when_travelling", report, True),
        "variant_group": (row.get("variant_group") or "").strip() or None,
        "variant_label": (row.get("variant_label") or "").strip() or None,
        "default_variant": parse_bool(row.get("default_variant", ""), f"{rid}: default_variant", report, False),
    }


def read_csv(path: Path) -> list[dict]:
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def next_version(path: Path, payload_key: str, payload: list) -> int:
    """Keeps the version when content is unchanged; otherwise previous + 1."""
    if not path.exists():
        return 1
    old = json.loads(path.read_text(encoding="utf-8"))
    if old.get(payload_key) == payload:
        return old.get("content_version", 1)
    return old.get("content_version", 0) + 1


def build(duas_csv: Path, rawatib_csv: Path, out_dir: Path, audio_dir: Path, report: Report) -> None:
    taxonomy = load_taxonomy()
    duas, seen = [], set()
    for line, row in enumerate(read_csv(duas_csv), start=2):
        d = dua_from_row(row, line, report)
        if d is None:
            continue
        if d["id"] in seen:
            report.errors.append(f"{d['id']}: duplicate id")
        seen.add(d["id"])
        validate_dua(d, taxonomy, audio_dir, report)
        duas.append(d)
    cat_order = {c: i for i, c in enumerate(taxonomy)}
    event_order = {e: i for events in taxonomy.values() for i, e in enumerate(events)}
    duas.sort(key=lambda d: (cat_order.get(d["category"], 99), event_order.get(d["event"], 99), d["order"], d["id"]))

    fard, rawatib, seen = [], [], set()
    for line, row in enumerate(read_csv(rawatib_csv), start=2):
        parsed = rawatib_from_row(row, line, report)
        if parsed is None:
            continue
        kind, entry = parsed
        if entry["id"] in seen:
            report.errors.append(f"{entry['id']}: duplicate id")
        seen.add(entry["id"])
        (fard if kind == "fard" else rawatib).append(entry)

    if not report.ok:
        return
    out_dir.mkdir(parents=True, exist_ok=True)
    duas_path = out_dir / "duas.json"
    rawatib_path = out_dir / "rawatib.json"
    duas_doc = {"content_version": next_version(duas_path, "duas", duas), "duas": duas}
    rawatib_payload = {"fard": fard, "rawatib": rawatib}
    old_r = json.loads(rawatib_path.read_text(encoding="utf-8")) if rawatib_path.exists() else {}
    same = old_r.get("fard") == fard and old_r.get("rawatib") == rawatib
    rawatib_doc = {"content_version": old_r.get("content_version", 1) if same else old_r.get("content_version", 0) + 1,
                   **rawatib_payload}
    duas_path.write_text(json.dumps(duas_doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    rawatib_path.write_text(json.dumps(rawatib_doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def check(out_dir: Path, audio_dir: Path, report: Report) -> None:
    taxonomy = load_taxonomy()
    data = json.loads((out_dir / "duas.json").read_text(encoding="utf-8"))
    for d in data.get("duas", []):
        validate_dua(d, taxonomy, audio_dir, report)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--duas", type=Path, default=CONTENT / "source/duas.csv")
    ap.add_argument("--rawatib", type=Path, default=CONTENT / "source/rawatib.csv")
    ap.add_argument("--out", type=Path, default=CONTENT)
    ap.add_argument("--audio", type=Path, default=CONTENT / "audio")
    ap.add_argument("--check", action="store_true", help="validate existing JSON without reading CSV")
    args = ap.parse_args(argv)

    report = Report()
    if args.check:
        check(args.out, args.audio, report)
    else:
        build(args.duas, args.rawatib, args.out, args.audio, report)
    for w in report.warnings:
        print(f"warning: {w}")
    for e in report.errors:
        print(f"ERROR: {e}", file=sys.stderr)
    if not report.ok:
        print(f"\n{len(report.errors)} error(s). Nothing was written." if not args.check else "", file=sys.stderr)
        return 1
    print(f"OK ({len(report.warnings)} warning(s))")
    return 0


if __name__ == "__main__":
    sys.exit(main())
