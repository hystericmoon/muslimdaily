"""Tests for tools/alt_gradings.py.  Run: python3 -m unittest discover -s tools"""
import csv
import tempfile
import unittest
from pathlib import Path

import alt_gradings as ag
import build_content as bc
from test_build_content import DUA_COLUMNS, dua_row

SUNNAH_FIELDS = ["id", "number", "grade", "reason", "url"]


def write(path, fields, rows):
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


class ApplyTest(unittest.TestCase):
    def setUp(self):
        self.dir = Path(tempfile.mkdtemp())
        self.duas = self.dir / "duas.csv"
        self.sunnah = self.dir / "sunnah.csv"
        write(self.duas, DUA_COLUMNS, [dua_row()])
        write(self.sunnah, SUNNAH_FIELDS, [{
            "id": "t_01", "number": "(Uji 12; Coba 34)",
            "grade": "U Sahih (A); C Da'if (Penguji Dua)", "reason": "", "url": "",
        }])

    def rows(self):
        with self.duas.open(encoding="utf-8") as f:
            return list(csv.DictReader(f))

    def test_writes_the_cell_the_build_reads(self):
        gradings = {"t_01": [("Penguji Dua", "Coba 34", "da'if", "C Da'if (Penguji Dua)")]}
        applied, problems = ag.apply(self.duas, self.sunnah, gradings)
        self.assertEqual((["t_01"], []), (applied, problems))
        row = self.rows()[0]
        self.assertEqual("Penguji Dua (Coba 34): da'if", row["alt_gradings"])
        dua = bc.dua_from_row(row, 2, bc.Report())
        self.assertEqual([{"grade": "da'if", "grader": "Penguji Dua (Coba 34)"}], dua["alt_gradings"])

    def test_grade_missing_from_the_reviewed_table_is_refused(self):
        gradings = {"t_01": [("Penguji Dua", "Coba 34", "hasan", "C Hasan (Penguji Dua)")]}
        applied, problems = ag.apply(self.duas, self.sunnah, gradings)
        self.assertEqual([], applied)
        self.assertTrue(problems)
        self.assertEqual("", self.rows()[0]["alt_gradings"])

    def test_number_missing_from_the_reviewed_table_is_refused(self):
        gradings = {"t_01": [("Penguji Dua", "Coba 99", "da'if", "C Da'if (Penguji Dua)")]}
        self.assertTrue(ag.apply(self.duas, self.sunnah, gradings)[1])

    def test_second_run_changes_nothing(self):
        gradings = {"t_01": [("Penguji Dua", "Coba 34", "da'if", "C Da'if (Penguji Dua)")]}
        ag.apply(self.duas, self.sunnah, gradings)
        self.assertEqual(([], []), ag.apply(self.duas, self.sunnah, gradings))

    def test_real_entries_match_the_reviewed_table(self):
        with ag.SUNNAH.open(encoding="utf-8", newline="") as f:
            reviewed = {r["id"]: r for r in csv.DictReader(f)}
        for rid, entries in ag.GRADINGS.items():
            self.assertEqual([], ag.check(rid, entries, reviewed), rid)


if __name__ == "__main__":
    unittest.main()
