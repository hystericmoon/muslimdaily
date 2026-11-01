#!/usr/bin/env python3
"""Proposes hadith numbers for duas whose source has none, for a person to approve.

    python3 -I tools/hadith_numbers.py suggest     # writes build/hadith_numbers/review.csv
    python3 -I tools/hadith_numbers.py apply       # copies approved rows into duas.csv
    python3 -I tools/hadith_numbers.py apply --review tools/hadith_numbers_sunnah.csv
    python3 -I tools/hadith_numbers.py apply --review tools/hadith_numbers_islamweb.csv

Two kinds of proposal, for rows with an empty `source_1_no`:

  * `in_book`: the import left the number inside the book name ("Abu Dawud 3/314 dengan sanad
    yang shahih"). The number and what follows it ("3/314 dengan sanad yang shahih", which can
    name the numbering) move to `source_1_no`; the book keeps its name. Nothing is dropped.
  * `dataset`: the row names one of the six books in fawazahmed0/hadith-api (public domain,
    https://github.com/fawazahmed0/hadith-api). The row's Arabic is matched against that book:
    the hadith holding the longest unbroken run of the dua's words wins. Status `match` when
    that run covers at least 80% of the dua (and 5+ words) in exactly one hadith; `several` when
    more than one hadith ties (all numbers listed, the lowest proposed); `check` when it covers
    at least half. The number uses the dataset's numbering. Al-Albani's grade is shown so a
    da'if hadith can be caught (the app is sahih/hasan only).

A third kind, `sunnah`, is looked up by hand on sunnah.com (which refuses cloud sessions) and
kept in `tools/hadith_numbers_sunnah.csv` with its URL, grade and confidence; rows naming a
group ("Seluruh penyusun kitab Sunan") get every book's number in brackets. A fourth, `islamweb`,
covers books sunnah.com lacks (al-Hakim's al-Mustadrak, al-Baihaqi's as-Sunan al-Kubra, an-Nasa'i's
as-Sunan al-Kubra with its 'Amal al-Yawm wa al-Laylah, the whole Musnad Ahmad): looked up in the
islamweb.net library (which answers cloud sessions) by matching the dua's words against the
book's pages, read in full, and kept in `tools/hadith_numbers_islamweb.csv` with the page URL. The
number is islamweb's numbering of that edition. A review row with `slot` 2 fills the row's second
source instead (its `source_2_book` must be the book named), for rows whose first source is not
online (travel_12/13: Ibnu As-Sunni).

Nothing is written into the Arabic or translations. `approved` is set by a person ("ya");
`apply` refuses rows that changed since the suggestion.
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import segment_audio as sa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "build/hadith_numbers"
CACHE = ROOT / "build/hadith_api"
URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-{}.min.json"
NOTE = "Nomor hadits: fawazahmed0/hadith-api"
NOTES = {"dataset": NOTE, "sunnah": "Nomor hadits: sunnah.com", "islamweb": "Nomor hadits: islamweb.net"}

# Book names as the import writes them -> dataset edition.
BOOKS = {
    "abu dawud": "abudawud", "at-tirmidzi": "tirmidhi", "tirmidzi": "tirmidhi",
    "an-nasai": "nasai", "an-nasa’i": "nasai", "ibnu majah": "ibnmajah",
    "al-bukhari": "bukhari", "muslim": "muslim",
}
REVIEW_FIELDS = ["id", "kind", "status", "book", "number", "others", "words", "run", "grade",
                 "approved", "source_sha1"]

HARAKAT = re.compile(r"[ؐ-ًؚ-ٰٟۖ-ۭـ]")
IN_BOOK = re.compile(r"^(?P<book>.+?)\s+(?P<no>(?:no\.\s*)?\d+(?:/\d+)?.*)$")


def words(text: str) -> list[str]:
    """Arabic words without harakat, with alif/ya/ta marbuta forms unified, for matching."""
    t = HARAKAT.sub("", text.replace(bc.PHRASE_MARK, " "))
    t = re.sub("[إأآٱ]", "ا", t).replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    return re.findall(r"[ء-ي]+", t)


def longest_run(a: list[str], b: list[str]) -> int:
    """Length of the longest unbroken run of words of [a] that appears in [b]."""
    where: dict[str, list[int]] = {}
    for j, w in enumerate(b):
        where.setdefault(w, []).append(j)
    best, prev = 0, {}
    for w in a:
        cur = {j: prev.get(j - 1, 0) + 1 for j in where.get(w, [])}
        best = max([best, *cur.values()])
        prev = cur
    return best


def row_sha1(row: dict) -> str:
    return sa.sha1(row["arabic"] + "\n" + row["source_1_book"] + "\n" + row["source_1_no"])


def in_book(row: dict) -> dict | None:
    m = IN_BOOK.match(row["source_1_book"].strip())
    if not m:
        return None
    number = m["no"].strip()
    if number.count(")") > number.count("("):  # "Muslim 1/539)": a bracket left over from the import
        number = number.rstrip(")").strip()
    return {"kind": "in_book", "status": "match", "book": m["book"].strip(), "number": number}


def from_dataset(row: dict, hadiths: list[dict]) -> dict:
    dua = words(row["arabic"])
    scored = sorted(((longest_run(dua, words(h["text"])), h) for h in hadiths), key=lambda x: -x[0])
    run = scored[0][0] if scored else 0
    top = [h for s, h in scored if s == run]
    numbers = sorted(int(h["hadithnumber"]) for h in top)
    grade = next((g["grade"] for g in top[0].get("grades", []) if g.get("name") == "Al-Albani"), "") if top else ""
    if run >= max(5, 0.8 * len(dua)):
        status = "match" if len(top) == 1 else "several"
    elif run >= max(5, 0.5 * len(dua)) and len(top) == 1:
        status = "check"
    else:
        status = "none"
    return {"kind": "dataset", "status": status, "book": row["source_1_book"].strip(),
            "number": str(numbers[0]) if numbers and status != "none" else "",
            "others": " ".join(map(str, numbers[1:6])), "words": len(dua), "run": run, "grade": grade}


def load(edition: str) -> list[dict]:
    path = CACHE / f"ara-{edition}.min.json"
    if not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(URL.format(edition), timeout=120) as r:
            path.write_bytes(r.read())
    return json.loads(path.read_text(encoding="utf-8"))["hadiths"]


def suggest(duas: Path = sa.DUAS, loader=load, out_dir: Path = OUT_DIR) -> list[dict]:
    _, rows, _ = sa.read_csv(duas)
    results, cache = [], {}
    for row in rows:
        if row["source_1_no"].strip() or not row["source_1_book"].strip():
            continue
        found = in_book(row)
        edition = BOOKS.get(row["source_1_book"].strip().lower())
        if found is None and edition:
            if edition not in cache:
                cache[edition] = loader(edition)
            found = from_dataset(row, cache[edition])
        if found is None or found["status"] == "none":
            continue
        found.update(id=row["id"], approved="", source_sha1=row_sha1(row))
        results.append(found)
    out_dir.mkdir(parents=True, exist_ok=True)
    sa.write_csv(out_dir / "review.csv", REVIEW_FIELDS, results)
    return results


def apply(review: Path = OUT_DIR / "review.csv", duas: Path = sa.DUAS) -> tuple[list[str], list[str]]:
    _, reviewed, _ = sa.read_csv(review)
    fields, rows, newline = sa.read_csv(duas)
    by_id = {r["id"]: r for r in rows}
    applied, problems = [], []
    for r in reviewed:
        if (r.get("approved") or "").strip().lower() not in bc.YES:
            continue
        target = by_id.get(r["id"])
        slot = "2" if (r.get("slot") or "1").strip() == "2" else "1"
        if target is not None and target[f"source_{slot}_no"].strip() == r["number"].strip():
            continue  # applied before
        if target is None or row_sha1(target) != r["source_sha1"]:
            problems.append(f"{r['id']}: changed since the suggestion; run suggest again")
            continue
        if not r["number"].strip():
            problems.append(f"{r['id']}: no number")
            continue
        if slot == "2":  # the row's second source (its book must match)
            if target["source_2_book"].strip() != r["book"].strip():
                problems.append(f"{r['id']}: source_2_book is not {r['book']}")
                continue
            target["source_2_no"] = r["number"]
        else:
            target["source_1_book"] = r["book"]
            target["source_1_no"] = r["number"]
        note = NOTES.get(r["kind"])
        if note and note not in target.get("notes", ""):
            target["notes"] = " | ".join(x for x in (target.get("notes", ""), note) if x)
        applied.append(r["id"])
    if applied:
        sa.write_csv(duas, fields, rows, newline)
    return applied, problems


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("suggest")
    sub.add_parser("apply").add_argument("--review", type=Path, default=OUT_DIR / "review.csv")
    args = ap.parse_args()
    if args.cmd == "suggest":
        results = suggest()
        print(f"{len(results)} proposals in {OUT_DIR / 'review.csv'}")
        return 0
    applied, problems = apply(args.review)
    print(f"applied: {' '.join(applied) or '-'}")
    for p in problems:
        print("problem:", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
