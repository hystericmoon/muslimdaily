#!/usr/bin/env python3
"""Imports Hisnul Muslim into the content spreadsheet as draft rows for the reviewer.

    python3 tools/hisnul_muslim/import_hisn.py --pdf PATH/TO/hisnul-muslim.pdf [--merge]

Sources, joined by the book's standard entry number (1-267):
  * Indonesian translation, takhrij footnotes and the closing appendix on weak narrations:
    the Indonesian Hisnul Muslim PDF supplied by the owner (Arabic in it is not extractable).
  * Arabic text and repetition counts: hisnmuslim.com, the book's official site.

Always writes content/source/import/hisnul_muslim.csv: every entry, same columns as duas.csv
plus hisn_no / hisn_chapter, all with status "draft". This folder is not committed: the
translation's redistribution licence still has to be confirmed.

With --merge, also replaces the placeholder rows in content/source/duas.csv for each app
event that has a matching Hisnul Muslim chapter, skipping entries the appendix marks weak.

Nothing here writes religious text: it copies the two sources verbatim, keeps the footnote
in reviewer_notes, and only pre-fills grade/grader where the footnote states al-Albani's
verdict. Every row stays a draft until the ustadz reviews it.
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import weak_appendix  # noqa: E402

ROOT = HERE.parent.parent
DUAS = ROOT / "content/source/duas.csv"
STAGING = ROOT / "content/source/import/hisnul_muslim.csv"
CACHE = ROOT / "build/hisnmuslim"

# hisnmuslim.com chapter id -> app events (category, event, id prefix).
# Chapters not listed stay in the staging file only.
CHAPTER_EVENTS: dict[int, list[tuple[str, str, str]]] = {
    1: [("daily", "waking_up", "wake")],
    2: [("daily", "dressing", "dress")],
    3: [("daily", "dressing", "dress")],
    4: [("daily", "dressing", "dress")],
    5: [("daily", "dressing", "dress")],
    6: [("daily", "toilet", "toilet")],
    7: [("daily", "toilet", "toilet")],
    8: [("daily", "wudu", "wudu")],
    9: [("daily", "wudu", "wudu")],
    10: [("daily", "leaving_home", "leave_home")],
    11: [("daily", "entering_home", "enter_home")],
    12: [("daily", "going_to_masjid", "to_masjid")],
    13: [("daily", "entering_masjid", "enter_masjid")],
    14: [("daily", "leaving_masjid", "leave_masjid")],
    15: [("daily", "adhan", "adhan")],
    16: [("prayer", "istiftah", "istiftah")],
    17: [("prayer", "ruku", "ruku")],
    19: [("prayer", "sujud", "sujud")],
    22: [("prayer", "tashahhud", "tashahhud")],
    23: [("prayer", "tashahhud", "tashahhud")],
    24: [("prayer", "tashahhud", "tashahhud")],
    25: [("prayer", "after_salah", "after_salah")],
    26: [("situational", "istikharah", "istikharah")],
    27: [("adhkar", "morning", "morning"), ("adhkar", "evening", "evening")],
    28: [("daily", "sleeping", "sleep")],
    34: [("situational", "distress", "distress")],
    35: [("situational", "distress", "distress")],
    43: [("situational", "distress", "distress")],
    49: [("situational", "illness", "illness")],
    51: [("situational", "illness", "illness")],
    62: [("situational", "thunder", "thunder")],
    63: [("situational", "rain", "rain")],
    64: [("situational", "rain", "rain")],
    65: [("situational", "rain", "rain")],
    66: [("situational", "rain", "rain")],
    69: [("daily", "eating", "eating")],
    70: [("daily", "eating", "eating")],
    95: [("situational", "travel", "travel")],
    96: [("situational", "travel", "travel")],
    97: [("situational", "travel", "travel")],
    105: [("situational", "travel", "travel")],
}

TAKHRIJ = re.compile(r"\bHR\.\s*([^.]*(?:\.\s*\d[^.]*)*)")
REF_NO = re.compile(r"\s*((?:no\.\s*)?\d+(?:\s*/\s*\d+)?(?:-\d+)?)\s*$", re.I)


def split_takhrij(footnote: str) -> list[tuple[str, str]]:
    """'HR. Al-Bukhari 7/162, Muslim 4/2083.' -> [('Al-Bukhari', '7/162'), ('Muslim', '4/2083')].

    A suggestion only; the full footnote is kept in reviewer_notes.
    """
    m = TAKHRIJ.search(footnote)
    if not m:
        return []
    # Stop at grading or cross-reference remarks ("dinyatakan shahih oleh Al-Albani ...").
    body = re.split(r"\b(?:dinyatakan|dishahihkan|menshahihkan|dihasankan|lihat|Al-?\s?Albani)\b", m.group(1), flags=re.I)[0]
    out: list[tuple[str, str]] = []
    for part in re.split(r",|\bdan\b", body):
        part = part.strip(" .;")
        if not part:
            continue
        ref = REF_NO.search(part)
        book = part[: ref.start()].strip(" ,") if ref else part
        no = ref.group(1).strip() if ref else ""
        if book:
            out.append((book, no))
        elif no and out and not out[-1][1]:
            out[-1] = (out[-1][0], no)  # "HR. Al-Hakim, 1/562"
    return out[:3]


def albani_grade(footnote: str) -> str:
    """'sahih' / 'hasan' when the footnote states al-Albani's verdict plainly, else ''."""
    t = footnote.lower()
    if "albani" not in t or re.search(r"dla.?if|dha.?if|lemah|maudlu|mungkar|syadz", t):
        return ""
    if re.search(r"\bhasan\b", t):
        return "hasan"
    if re.search(r"shahih|menshahihkan|dishahihkan", t):
        return "sahih"
    return ""


def row_template(fields: list[str]) -> dict[str, str]:
    return {f: "" for f in fields}


def build_rows(entries, footnotes, arabic, weak, fields, pdf_name: str) -> list[dict]:
    rows = []
    titles: dict[int, str] = {}
    for e in entries:
        if e.number in arabic:
            titles.setdefault(arabic[e.number]["chapter_id"], e.chapter_title)
    by_no = {e.number: e for e in entries}
    for n in sorted(set(arabic) | set(by_no)):
        e, a = by_no.get(n), arabic.get(n)
        notes_txt = [footnotes.get(k, "") for k in (e.notes if e else [])]
        notes_txt = [t for t in notes_txt if t]
        joined = " ".join(notes_txt)
        flags = []
        if e is None:
            flags.append("Terjemah tidak ditemukan di PDF (entri tidak bernomor; kemungkinan tergabung di entri sebelumnya).")
        elif e.printed_number:
            flags.append(f"Di PDF tertulis nomor {e.printed_number}.")
        if a is None:
            flags.append("Teks Arab tidak ada di hisnmuslim.com.")
        if n in weak:
            w = weak[n]
            if w["weak"]:
                flags.append(f"LEMAH menurut al-Albani (lampiran PDF): {w['takhrij']} {w['verdict']}")
            else:
                flags.append(f"Lampiran PDF: {w['takhrij']} {w['verdict']}")
        repeat = a["repeat"] if a else None
        count = str(repeat) if repeat and repeat > 1 else ""
        if n in weak and not weak[n]["weak"] and weak[n].get("count"):
            flags.append(f"Jumlah '{weak[n]['count']}' dinilai tidak shahih oleh al-Albani; kolom count dikosongkan.")
            count = ""
        arabic_text = a["arabic"] if a else ""
        if re.match(r"\s*(?:قَالَ|قال|كان|كَانَ)", arabic_text):
            flags.append("Teks Arab diawali konteks riwayat (bukan lafazh doa); rapikan.")
        sources = split_takhrij(joined)
        grade = albani_grade(joined)
        chapter_id = a["chapter_id"] if a else None
        r = row_template(fields)
        r.update({
            "hisn_no": str(n),
            "hisn_chapter": f"{chapter_id or ''} {titles.get(chapter_id, '') or (e.chapter_title if e else '')} / {a['chapter_ar'] if a else ''}".strip(),
            "arabic": arabic_text,
            "is_quran": "ya" if "﴿" in arabic_text else "tidak",
            "translation_id": e.translation if e else "",
            "count": count,
            "grade": grade,
            "grader": "al-Albani" if grade else "",
            "status": "draft",
            "reviewer_notes": " | ".join(filter(None, [
                f"Hisnul Muslim no. {n}",
                f"Catatan kaki PDF: {joined}" if joined else "Catatan kaki PDF: (tidak ada)",
                *flags,
                f"Terjemah & takhrij: {pdf_name}; teks Arab: hisnmuslim.com",
            ])),
        })
        for i, (book, no) in enumerate(sources, start=1):
            r[f"source_{i}_book"], r[f"source_{i}_no"] = book, no
        rows.append(r)
    return rows


def merge(rows: list[dict], weak: dict[int, dict], arabic: dict[int, dict]) -> tuple[list[dict], dict]:
    with DUAS.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        fields, existing = reader.fieldnames, list(reader)
    by_event: dict[tuple[str, str], list[dict]] = {}
    for r in rows:
        n = int(r["hisn_no"])
        if n in weak and weak[n]["weak"]:
            continue
        a = arabic.get(n)
        for category, event, prefix in CHAPTER_EVENTS.get(a["chapter_id"] if a else -1, []):
            out = {k: r.get(k, "") for k in fields}
            out.update({"category": category, "event": event})
            by_event.setdefault((category, event, prefix), []).append(out)
    replaced = {(c, e) for c, e, _ in by_event}
    kept = [
        r for r in existing
        if (r["category"], r["event"]) not in replaced or "[PLACEHOLDER" not in r.get("arabic", "")
    ]
    taken = {r["id"] for r in kept}
    added = []
    for (category, event, prefix), items in by_event.items():
        start = 1 + sum(1 for r in kept if r["event"] == event)
        for i, r in enumerate(items, start=start):
            rid = f"{prefix}_{i:02d}"
            while rid in taken:
                i += 1
                rid = f"{prefix}_{i:02d}"
            taken.add(rid)
            r.update({"id": rid, "order": str(i)})
            added.append(r)
    with DUAS.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(kept + added)
    stats = {"kept": len(kept), "added": len(added), "events": len(replaced)}
    return added, stats


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--pdf", type=Path, required=True)
    ap.add_argument("--merge", action="store_true", help="also replace placeholder rows in duas.csv")
    args = ap.parse_args()

    import fetch_arabic
    import parse_pdf

    entries, footnotes, appendix = parse_pdf.parse(str(args.pdf))
    arabic = fetch_arabic.fetch(CACHE)
    weak = weak_appendix.match(
        weak_appendix.items(appendix),
        {e.number: e.translation for e in entries},
        {e.number: e.chapter_title for e in entries},
    )
    with DUAS.open(encoding="utf-8-sig", newline="") as f:
        fields = csv.DictReader(f).fieldnames
    staging_fields = ["hisn_no", "hisn_chapter"] + [f for f in fields if f not in ("id", "category", "event", "order")]
    rows = build_rows(entries, footnotes, arabic, weak, staging_fields, args.pdf.name)

    STAGING.parent.mkdir(parents=True, exist_ok=True)
    with STAGING.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=staging_fields)
        w.writeheader()
        w.writerows(rows)
    weak_n = sorted(n for n, v in weak.items() if v["weak"])
    graded = sum(1 for r in rows if r["grade"])
    sourced = sum(1 for r in rows if r["source_1_book"])
    print(f"{len(rows)} entries -> {STAGING.relative_to(ROOT)}")
    print(f"  with Indonesian: {sum(1 for r in rows if r['translation_id'])}, Arabic: {sum(1 for r in rows if r['arabic'])}")
    print(f"  source suggested: {sourced}, al-Albani grade stated: {graded}, weak per appendix: {weak_n}")
    if args.merge:
        added, stats = merge(rows, weak, arabic)
        print(f"duas.csv: {stats['added']} draft rows added across {stats['events']} events, {stats['kept']} rows kept")
    return 0


if __name__ == "__main__":
    sys.exit(main())
