#!/usr/bin/env python3
"""Builds the app's content files from the owner's reviewed spreadsheets.

    python3 tools/build_content.py            # CSV -> content/duas.json + content/rawatib.json
    python3 tools/build_content.py --check    # validate the existing JSON only

It also writes content/play/duas.json, the Play build's copy (docs/play-store.md, "Plan B"):
the same rows without the English taken from hisnmuslim.com, whose translator and licence are
not named. Rows whose only translation is that English are left out of it.

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
  * Where a source entry also carries its context (hadith wording, a note, a repetition count),
    `arabic_full` keeps the entry verbatim and `arabic` holds only the words to recite. Every
    word of `arabic` must come from `arabic_full`, in order: nothing is written, only left out.
    `audio_range` is where those words are recited in the audio file. `translation_id_full` and
    `translation_en_full` do the same for the translations.
  * `transliteration`, when empty, is derived from the Arabic by tools/transliterate.py
    (Indonesian style); a row the rules can't read gets none.

Exits with status 1 on any error, so CI and release builds stop.

The content version increments automatically whenever the generated JSON changes.
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from transliterate import transliterate  # noqa: E402

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
# Brackets and punctuation the book prints around a recitation. A recited part may leave them
# out (or keep them); every other character must match the full entry.
FRAME_CHARS = "()[]«»{}﴿﴾.،,:؛“”\"‘’'"


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


def strip_quote_marks(text: str) -> str:
    """Drops the book's quotation marks around a whole dua for display: one outer "((...))" or
    "(...)" pair, with a full stop after it. Only a pair that encloses the whole text is
    removed; "[...]" (words in some narrations), notes in brackets inside the text and
    Qur'anic ﴿...﴾ stay. A "((...))" followed only by the book's bracketed notes ("(three
    times)", "[in the evening he says: ...]") loses its marks too: the app shows those notes
    muted, so the end of the dua stays visible. Notes in plain text after the marks keep them,
    since the marks are then the only sign of where the dua ends. The CSV keeps the entry as the
    book prints it."""
    before_notes = _quote_before_notes(text)
    if before_notes is not None:
        return before_notes
    t = text.strip()
    if t.endswith("."):
        t = t[:-1].rstrip()
    for opening, closing in (("((", "))"), ("(", ")")):
        if not (t.startswith(opening) and t.endswith(closing)) or len(t) <= 2 * len(opening):
            continue
        depth = 0
        for i, ch in enumerate(t):
            depth += {"(": 1, ")": -1}.get(ch, 0)
            if i >= len(opening) - 1 and depth < len(opening) and i < len(t) - len(closing):
                break  # the opening bracket closes before the end: not a wrapper
        else:
            inner = t[len(opening):-len(closing)].strip()
            if inner.startswith(PHRASE_MARK) or inner.endswith(PHRASE_MARK):
                break
            return inner
    return text.strip()


def _bracket_groups(text: str) -> bool:
    """True when text is nothing but balanced "(...)" or "[...]" groups (and spaces), none doubled."""
    i, n = 0, len(text)
    if not text.strip():
        return False
    while i < n:
        if text[i].isspace():
            i += 1
            continue
        if text[i] not in "([" or text[i:i + 2] == "((":
            return False
        opening, closing = text[i], ")" if text[i] == "(" else "]"
        depth, j = 0, i
        while j < n:
            depth += 1 if text[j] == opening else -1 if text[j] == closing else 0
            if depth == 0:
                break
            j += 1
        if j == n:
            return False
        i = j + 1
    return True


def _quote_before_notes(text: str) -> str | None:
    """"((dua)). (note) [note]" -> "dua (note) [note]"; None when the text is not of that shape."""
    t = text.strip()
    if t.endswith("."):
        t = t[:-1].rstrip()
    if not t.startswith("((") or t.count("((") != 1 or t.count("))") != 1:
        return None
    end = t.index("))")
    inner, rest = t[2:end].strip(), t[end + 2:].strip()
    if rest.startswith("."):
        rest = rest[1:].strip()
    if not inner or not _bracket_groups(rest):
        return None
    if inner.startswith(PHRASE_MARK) or inner.endswith(PHRASE_MARK):
        return None
    return inner + " " + rest


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


def words(text: str) -> list[str]:
    """The text's words, without the brackets and punctuation around them."""
    out = []
    # "،" ":" "؛" run straight into the next word in some entries ("فيهما:بسم"); they end a word.
    for token in re.sub(r"([،:؛])(?=\w)", r"\1 ", text.replace(PHRASE_MARK, " ")).split():
        token = token.strip(FRAME_CHARS)
        if token:
            out.append(token)
    return out


KINDS = {"", "hadith"}
HADEETHENC = Path(__file__).resolve().parent.parent / "content/source/import/hadeethenc.json"


def check_taken(duas: list[dict], report: Report) -> None:
    """A translation taken from elsewhere must be words of that source, in order."""
    by_id = {d["id"]: d for d in duas}
    snapshot = None
    for d in duas:
        for lang, src in (d.get("translations_from") or {}).items():
            if src == "none":
                continue
            kind, _, ref = src.partition(":")
            text = d["translations"].get(lang, "")
            if kind == "row":
                other = by_id.get(ref)
                base = other["translations"].get(lang, "") if other else None
            else:
                if snapshot is None:
                    snapshot = json.loads(HADEETHENC.read_text(encoding="utf-8")).get("hadeeths", {})
                h = snapshot.get(ref, {}).get(lang)
                base = (h.get("hadeeth") if isinstance(h, dict) else h) if h else None
            if base is None:
                report.errors.append(f"{d['id']}: translation_{lang}_from {src} not found")
            elif not text_part_of(text, base):
                report.errors.append(f"{d['id']}: translation_{lang} must be words of {src} in order")


def is_part_of(part: str, full: str) -> bool:
    """True when every word of [part] appears in [full], in the same order."""
    it = iter(words(full))
    return all(any(w == f for f in it) for w in words(part))


def text_part_of(part: str, full: str) -> bool:
    """is_part_of for a translation, whose words run into brackets and quotes ("says:‘And"):
    every word of [part] appears in [full], in order."""
    it = iter(re.findall(r"\w+", full))
    return all(any(w == f for f in it) for w in re.findall(r"\w+", part))


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


CONTROL_CHARS = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")


def strip_markers(text: str) -> str:
    """The Hisnul Muslim PDF's footnote markers come out as NUL bytes, each with a stray dot
    after the last sentence ("…sore). \\x00 . \\x00"). Drops the control characters and the
    dots left alone at the end; words are never touched. Used by hisnul_muslim/parse_pdf.py."""
    t = re.sub(r"\s+", " ", CONTROL_CHARS.sub(" ", text)).strip()
    return re.sub(r"(?:\s+\.)+$", "", t)


def _strings(value, key: str = ""):
    """(key path, text) for every string inside a dua entry."""
    if isinstance(value, str):
        yield key, value
    elif isinstance(value, dict):
        for k, v in value.items():
            yield from _strings(v, f"{key}.{k}" if key else k)
    elif isinstance(value, list):
        for i, v in enumerate(value):
            yield from _strings(v, f"{key}[{i}]")


def validate_dua(d: dict, taxonomy: dict[str, list[str]], audio_dir: Path, report: Report) -> None:
    """Validates one dua entry in its JSON shape. Shared by CSV build and --check."""
    did = d.get("id") or "?"
    p = lambda msg: report.problem(f"{did}: {msg}")  # noqa: E731

    for key, value in _strings(d):
        if CONTROL_CHARS.search(value):
            report.errors.append(f"{did}: {key} has control characters (PDF footnote markers?); "
                                 "clean them with strip_markers")
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
    no_id = (d.get("translations_from") or {}).get("id") == "none"  # no source has it; English shown
    if not no_id and is_blank_or_placeholder(d.get("translations", {}).get("id")):
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
    rng = audio.get("range")
    if rng:
        if not audio.get("file"):
            p("audio_range given without an audio file")
        if len(rng) != 2 or rng[1] <= rng[0] or rng[0] < 0:
            report.errors.append(f"{did}: audio_range must look like '3.2-9.8'")
        elif segments and (segments[0][0] < rng[0] - 1e-6 or segments[-1][1] > rng[1] + 1e-6):
            report.errors.append(f"{did}: audio segments must lie inside audio_range")
    full = d.get("arabic_full") or ""
    if full:
        if full.replace(PHRASE_MARK, "").strip() == d.get("arabic", "").strip():
            p("arabic_full is the same as arabic; leave it blank")
        elif not is_part_of(d.get("arabic", ""), full):
            report.errors.append(f"{did}: arabic must be words of arabic_full in order (nothing added or changed)")
    if d.get("kind", "") not in KINDS:
        report.errors.append(f"{did}: kind must be one of {sorted(KINDS)}")
    if d.get("footnote") and not is_part_of(d["footnote"], full):
        report.errors.append(f"{did}: footnote must be words of arabic_full")
    taken = d.get("translations_from") or {}
    for lang, src in taken.items():
        if src == "none":
            if lang != "id" or d["translations"].get("id") or not d["translations"].get("en"):
                report.errors.append(f"{did}: translation_id_from 'none' needs an empty Indonesian and an English")
        elif not re.fullmatch(r"(row|hadeethenc):[\w.-]+", src):
            report.errors.append(f"{did}: translation_{lang}_from must be 'row:<id>', 'hadeethenc:<id>' or 'none'")
    for lang, tr_full in (d.get("translations_full") or {}).items():
        if lang in taken:
            continue  # checked against its own source in check_taken()
        if not text_part_of(d.get("translations", {}).get(lang, ""), tr_full):
            report.errors.append(f"{did}: translation_{lang} must be words of translation_{lang}_full in order")
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
    audio_range = None
    if (row.get("audio_range") or "").strip():
        try:
            parsed = parse_segments(row["audio_range"])
            audio_range = parsed[0] if len(parsed) == 1 else []
        except ValueError:
            audio_range = []
        if not audio_range:
            report.errors.append(f"{rid}: audio_range must look like '3.2-9.8'")
            audio_range = None
    arabic, breaks = split_phrases(strip_quote_marks(row.get("arabic", "")))
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
        **({"arabic_full": full} if (full := (row.get("arabic_full") or "").strip()) else {}),
        "is_quran": parse_bool(row.get("is_quran", ""), f"{rid}: is_quran", report, True),
        "translations": {
            "id": (row.get("translation_id") or "").strip(),
            "en": (row.get("translation_en") or "").strip(),
        },
        **({"translations_full": tf} if (tf := {
            lang: text for lang in ("id", "en") if (text := (row.get(f"translation_{lang}_full") or "").strip())
        }) else {}),
        # A sourced transliteration wins; otherwise the Latin is derived from the Arabic by
        # tools/transliterate.py (the owner's one exception to the sourced-content rule).
        "transliteration": (row.get("transliteration") or "").strip() or transliterate(arabic) or "",
        "count": count,
        "sources": sources,
        "reference": (row.get("reference") or "").strip(),
        "grading": {"grade": (row.get("grade") or "").strip().lower(), "grader": (row.get("grader") or "").strip()},
        "alt_gradings": alt,
        "virtue": {"text": virtue_text, "source": (row.get("virtue_source") or "").strip()} if virtue_text else None,
        "audio": {"file": (row.get("audio_file") or "").strip(), "segments": segments, **({"range": audio_range} if audio_range else {})},
        "phrase_breaks": breaks,
        # "hadith": nothing to recite (a virtue hadith, an instruction); shown as a reading card.
        **({"kind": kind} if (kind := (row.get("kind") or "").strip()) else {}),
        # Another wording the book gives as a note (the evening form), cut from arabic_full.
        **({"footnote": fn} if (fn := (row.get("footnote") or "").strip()) else {}),
        # Where a translation was taken from: "row:<id>" (same book) or "hadeethenc:<id>".
        **({"translations_from": tfrom} if (tfrom := {
            lang: src for lang in ("id", "en") if (src := (row.get(f"translation_{lang}_from") or "").strip())
        }) else {}),
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


HISN_NOTE = "Hisnul Muslim"


def play_duas(duas: list[dict], hisn_ids: set[str]) -> tuple[list[dict], list[str]]:
    """The Play build's rows: Hisn al-Muslim English (hisnmuslim.com, or IslamHouse's English
    edition for one row) removed; Qur'an and HadeethEnc English stay. Returns the rows and the
    ids left out because no translation would be left."""
    rows, left_out = [], []
    for d in duas:
        if d["id"] not in hisn_ids:
            rows.append(d)
            continue
        if not d["translations"].get("id"):
            left_out.append(d["id"])
            continue
        d = json.loads(json.dumps(d))
        d["translations"]["en"] = ""
        full = d.get("translations_full")
        if full and "en" in full:
            del full["en"]
            if not full:
                del d["translations_full"]
        taken = d.get("translations_from")
        if taken and "en" in taken:
            del taken["en"]
            if not taken:
                del d["translations_from"]
        rows.append(d)
    return rows, left_out


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
    duas, seen, hisn_ids = [], set(), set()
    for line, row in enumerate(read_csv(duas_csv), start=2):
        d = dua_from_row(row, line, report)
        if d is None:
            continue
        if (row.get("notes") or "").strip().startswith(HISN_NOTE):
            hisn_ids.add(d["id"])
        if d["id"] in seen:
            report.errors.append(f"{d['id']}: duplicate id")
        seen.add(d["id"])
        validate_dua(d, taxonomy, audio_dir, report)
        duas.append(d)
    check_taken(duas, report)
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

    play, left_out = play_duas(duas, hisn_ids)
    play_path = out_dir / "play" / "duas.json"
    play_path.parent.mkdir(exist_ok=True)
    play_doc = {"content_version": duas_doc["content_version"], "duas": play}
    play_path.write_text(json.dumps(play_doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report.warnings.append(f"play/duas.json: {len(left_out)} row(s) left out, English only: {', '.join(left_out)}")


def check(out_dir: Path, audio_dir: Path, report: Report) -> None:
    taxonomy = load_taxonomy()
    data = json.loads((out_dir / "duas.json").read_text(encoding="utf-8"))
    for d in data.get("duas", []):
        validate_dua(d, taxonomy, audio_dir, report)
    check_taken(data.get("duas", []), report)


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
