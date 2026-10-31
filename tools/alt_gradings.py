#!/usr/bin/env python3
"""Writes other graders' verdicts into `alt_gradings` in duas.csv.

    python3 -I tools/alt_gradings.py          # writes duas.csv, then run build_content.py

The app's grading standard is al-Albani's (the book's footnotes). Where another grader is
known to differ, the Sources sheet lists that verdict under "Penilaian lain" / "Other gradings".

Owner's decision (31 Oct 2026, option B on the da'if decision page): the five rows whose
sunnah.com lookup (#102) showed Darussalam grading the cited chains da'if stay in the app,
and Darussalam's grade is shown for each chain. Every grade below is copied from the reviewed
sunnah.com table `tools/hadith_numbers_sunnah.csv` (its `grade` column); `apply` refuses an
entry whose grade or hadith numbers are not in that row, so nothing here is typed from memory.

Format of the CSV cell: `grader (chains): grade; ...`, read by build_content.dua_from_row.
"""
from __future__ import annotations

import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import segment_audio as sa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SUNNAH = ROOT / "tools/hadith_numbers_sunnah.csv"

# id -> [(grader, chains, grade, evidence in the reviewed table's grade column)]
GRADINGS: dict[str, list[tuple[str, str, str, str]]] = {
    "leave_home_02": [
        ("Darussalam", "At-Tirmidzi 3427, An-Nasai 5486, Ibnu Majah 3884", "da'if", "T, N, IM Da'if (Darussalam)"),
    ],
    "between_sujud_02": [
        ("Darussalam", "At-Tirmidzi 284, Ibnu Majah 898", "da'if", "T, IM Da'if (Darussalam)"),
    ],
    "sleep_12": [
        ("Darussalam", "At-Tirmidzi 2892", "da'if", "Da'if (Darussalam)"),
    ],
    "illness_05": [
        ("Darussalam", "At-Tirmidzi 3430, Ibnu Majah 3794", "da'if", "Da'if (Darussalam); Ibnu Majah 3794 Da'if (Darussalam)"),
    ],
    "illness_06": [
        ("Darussalam", "At-Tirmidzi 969", "da'if", "Da'if (Darussalam)"),
        ("Darussalam", "Ibnu Majah 1442", "hasan", "Ibnu Majah 1442 Hasan (Darussalam)"),
    ],
}


def cell(entries: list[tuple[str, str, str, str]]) -> str:
    return "; ".join(f"{grader} ({chains}): {grade}" for grader, chains, grade, _ in entries)


def check(rid: str, entries, reviewed: dict[str, dict]) -> list[str]:
    row = reviewed.get(rid)
    if row is None:
        return [f"{rid}: not in {SUNNAH.name}"]
    problems = []
    text = " ".join(row.get(k, "") for k in ("number", "grade", "reason", "url"))
    for grader, chains, grade, evidence in entries:
        if evidence not in row.get("grade", ""):
            problems.append(f"{rid}: grade '{evidence}' not in the reviewed table")
        if grader not in evidence or grade.lower() not in evidence.lower():
            problems.append(f"{rid}: '{grader}: {grade}' does not match its evidence '{evidence}'")
        for chain in chains.split(","):
            number = chain.strip().rsplit(" ", 1)[-1]
            if number not in text:
                problems.append(f"{rid}: hadith number {number} not in the reviewed table")
    return problems


def apply(duas: Path = sa.DUAS, sunnah: Path = SUNNAH,
          gradings: dict = GRADINGS) -> tuple[list[str], list[str]]:
    with sunnah.open(encoding="utf-8", newline="") as f:
        reviewed = {r["id"]: r for r in csv.DictReader(f)}
    fields, rows, newline = sa.read_csv(duas)
    by_id = {r["id"]: r for r in rows}
    applied, problems = [], []
    for rid, entries in gradings.items():
        found = check(rid, entries, reviewed)
        if rid not in by_id:
            found.append(f"{rid}: not in duas.csv")
        if found:
            problems += found
            continue
        value = cell(entries)
        if by_id[rid].get("alt_gradings", "") != value:
            by_id[rid]["alt_gradings"] = value
            applied.append(rid)
    if applied and not problems:
        sa.write_csv(duas, fields, rows, newline)
    return (applied if not problems else []), problems


def main() -> int:
    applied, problems = apply()
    print(f"applied: {' '.join(applied) or '-'}")
    for p in problems:
        print("problem:", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
