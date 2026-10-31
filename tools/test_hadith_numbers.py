"""Tests for tools/hadith_numbers.py.  Run: python3 -m unittest discover -s tools"""
import csv
import tempfile
import unittest
from pathlib import Path

import hadith_numbers as hn
from test_build_content import DUA_COLUMNS, dua_row

# Neutral placeholder words ("ألف باء …") only; no religious text is generated here.


def hadith(number, text, grade="Sahih"):
    return {"hadithnumber": number, "text": text, "grades": [{"name": "Al-Albani", "grade": grade}]}


class MatchTest(unittest.TestCase):
    def test_words_ignore_harakat_and_letter_forms(self):
        self.assertEqual(hn.words("أَلِفٌ بَاءٌ | جِيمَةٌ"), hn.words("الف باء جيمه"))

    def test_longest_unbroken_run(self):
        self.assertEqual(3, hn.longest_run(["a", "b", "c", "x"], ["z", "a", "b", "c", "d"]))

    def test_one_hadith_holding_the_dua_is_a_match(self):
        row = {"arabic": "ألف باء جيم دال هاء", "source_1_book": "Abu Dawud"}
        found = hn.from_dataset(row, [hadith(7, "قال ألف باء جيم دال هاء"), hadith(8, "ألف واو")])
        self.assertEqual(("match", "7", "Sahih"), (found["status"], found["number"], found["grade"]))

    def test_ties_are_several_with_the_lowest_proposed(self):
        row = {"arabic": "ألف باء جيم دال هاء", "source_1_book": "Abu Dawud"}
        found = hn.from_dataset(row, [hadith(9, "ألف باء جيم دال هاء"), hadith(3, "ألف باء جيم دال هاء")])
        self.assertEqual(("several", "3", "9"), (found["status"], found["number"], found["others"]))

    def test_a_short_overlap_is_not_proposed(self):
        row = {"arabic": "ألف باء جيم دال هاء واو زاي حاء", "source_1_book": "Abu Dawud"}
        self.assertEqual("none", hn.from_dataset(row, [hadith(1, "ألف باء")])["status"])

    def test_number_inside_the_book_name_moves_with_its_qualifier(self):
        self.assertEqual(("Abu Dawud", "3/314 dengan sanad yang shahih"),
                         tuple(hn.in_book({"source_1_book": "Abu Dawud 3/314 dengan sanad yang shahih"})[k]
                               for k in ("book", "number")))
        self.assertEqual("1/539", hn.in_book({"source_1_book": "Muslim 1/539)"})["number"])
        self.assertIsNone(hn.in_book({"source_1_book": "Empat penyusun kitab Sunan"}))


class SuggestApplyTest(unittest.TestCase):
    def test_suggest_then_apply_fills_only_approved_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            duas = Path(tmp) / "duas.csv"
            with duas.open("w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=DUA_COLUMNS)
                w.writeheader()
                w.writerow(dua_row(id="a", arabic="ألف باء جيم دال هاء", source_1_book="Abu Dawud", source_1_no=""))
                w.writerow(dua_row(id="b", arabic="واو زاي", source_1_book="Muslim 1/539)", source_1_no=""))
            results = hn.suggest(duas, loader=lambda _: [hadith(42, "ألف باء جيم دال هاء")], out_dir=Path(tmp))
            self.assertEqual({"a", "b"}, {r["id"] for r in results})
            review = Path(tmp) / "review.csv"
            rows = list(csv.DictReader(review.open(encoding="utf-8")))
            for r in rows:
                r["approved"] = "ya" if r["id"] == "a" else ""
            with review.open("w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=hn.REVIEW_FIELDS)
                w.writeheader()
                w.writerows(rows)
            self.assertEqual((["a"], []), hn.apply(review, duas))
            out = {r["id"]: r for r in csv.DictReader(duas.open(encoding="utf-8"))}
            self.assertEqual("42", out["a"]["source_1_no"])
            self.assertIn(hn.NOTE, out["a"]["notes"])
            self.assertEqual("", out["b"]["source_1_no"])

    def test_sunnah_com_rows_keep_the_group_book_and_get_their_own_note(self):
        with tempfile.TemporaryDirectory() as tmp:
            duas = Path(tmp) / "duas.csv"
            with duas.open("w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=DUA_COLUMNS)
                w.writeheader()
                w.writerow(dua_row(id="a", arabic="ألف باء", source_1_book="Empat penyusun kitab Sunan", source_1_no=""))
            target = next(csv.DictReader(duas.open(encoding="utf-8")))
            review = Path(tmp) / "sunnah.csv"
            fields = hn.REVIEW_FIELDS + ["url", "confidence", "reason"]
            with review.open("w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=fields)
                w.writeheader()
                w.writerow({"id": "a", "kind": "sunnah", "book": "Empat penyusun kitab Sunan",
                            "number": "(Abu Dawud 1, At-Tirmidzi 2)", "approved": "ya",
                            "source_sha1": hn.row_sha1(target), "url": "https://sunnah.com/abudawud:1"})
            self.assertEqual((["a"], []), hn.apply(review, duas))
            out = next(csv.DictReader(duas.open(encoding="utf-8")))
            self.assertEqual(("Empat penyusun kitab Sunan", "(Abu Dawud 1, At-Tirmidzi 2)"),
                             (out["source_1_book"], out["source_1_no"]))
            self.assertIn(hn.NOTES["sunnah"], out["notes"])
            self.assertNotIn(hn.NOTE, out["notes"])


if __name__ == "__main__":
    unittest.main()
