"""Tests for tools/clean_entries.py.  Run: python3 -m unittest discover -s tools"""
import csv
import tempfile
import unittest
from pathlib import Path

import clean_entries as ce
from test_build_content import DUA_COLUMNS, dua_row

# Neutral placeholder words ("ألف باء …") only; no religious text is generated here.
SHADDA_FATHA = "بَّ"  # source order: shadda (U+0651) then fatha (U+064E)


class VerbatimRunTest(unittest.TestCase):
    def test_returns_the_source_characters_whatever_the_mark_order(self):
        full = "قال: ((ر" + SHADDA_FATHA + " ألف باء))."
        typed = "ر" + "بَّ" + " ألف"  # fatha then shadda, as editors normalise it
        run = ce.verbatim_run(typed, full)
        self.assertEqual("ر" + SHADDA_FATHA + " ألف", run)
        self.assertIn(run, full)

    def test_brackets_around_the_run_stay_outside(self):
        self.assertEqual("ألف باء", ce.verbatim_run("ألف باء", "((ألف باء))."))

    def test_words_not_in_the_entry_are_refused(self):
        self.assertIsNone(ce.verbatim_run("ألف جيم", "ألف باء جيم"))


class CutNotesTest(unittest.TestCase):
    def test_count_and_time_notes_are_cut(self):
        self.assertEqual("Teks uji.", ce.cut_notes("Teks uji. (Dibaca tiga kali)"))
        self.assertEqual("(Test words.)", ce.cut_notes("(Test words.)(three times)"))
        self.assertEqual("(Test words.)", ce.cut_notes("(Test words.)( [ten times or once if lazy])"))
        self.assertEqual("“Teks uji.”", ce.cut_notes("“Teks uji.” Dibaca sepuluh kali setiap pagi."))
        self.assertEqual("Teks uji.", ce.cut_notes("Teks uji. (Dibaca 3 kali pada sore hari)."))

    def test_bracketed_words_of_the_dua_stay(self):
        text = "(O test, by your leave we reached the morning and the evening.)"
        self.assertEqual(text, ce.cut_notes(text))
        self.assertEqual("Teks (bagi semua) uji.", ce.cut_notes("Teks (bagi semua) uji."))


class ApplyTest(unittest.TestCase):
    def setUp(self):
        self.path = Path(tempfile.mkdtemp()) / "duas.csv"
        rows = [
            dua_row(id="t_01", order="1", arabic="((ألف باء)) قال: ((جيم دال)).",
                    translation_id="“Satu dua.” Lalu: “Tiga empat.”", translation_en="(One two.) then (three four.)"),
            dua_row(id="t_02", order="2", arabic="هاء واو", translation_id="Lima enam.", translation_en=""),
        ]
        with self.path.open("w", encoding="utf-8", newline="") as f:
            w = csv.DictWriter(f, fieldnames=DUA_COLUMNS, lineterminator="\n")
            w.writeheader()
            w.writerows(rows)
        self.splits = {"t_01": ("", [
            ce.Part("t_01", "ألف باء", "Satu dua.", "One two.", count="3"),
            ce.Part("t_09", "جيم دال", "Tiga empat.", "three four."),
        ])}

    def run_apply(self, **patch):
        saved = {k: getattr(ce, k) for k in ("SPLITS", "KINDS", "FOOTNOTES", "REMOVE", "TAKE", "DROP_FULL")}
        try:
            ce.SPLITS, ce.KINDS, ce.FOOTNOTES, ce.REMOVE, ce.TAKE, ce.DROP_FULL = (
                patch.get("SPLITS", self.splits), patch.get("KINDS", {}), {}, patch.get("REMOVE", {}), {}, {})
            return ce.apply(self.path)
        finally:
            for k, v in saved.items():
                setattr(ce, k, v)

    def rows(self):
        with self.path.open(encoding="utf-8") as f:
            return list(csv.DictReader(f))

    def test_one_row_per_saying_first_keeps_the_id(self):
        changed, problems = self.run_apply()
        self.assertEqual([], problems)
        rows = {r["id"]: r for r in self.rows()}
        self.assertEqual(["t_01", "t_09", "t_02"], [r["id"] for r in self.rows()])
        self.assertEqual(("ألف باء", "3", "Satu dua."), (rows["t_01"]["arabic"], rows["t_01"]["count"], rows["t_01"]["translation_id"]))
        self.assertEqual("((ألف باء)) قال: ((جيم دال)).", rows["t_09"]["arabic_full"])
        self.assertEqual(["1", "2", "3"], [r["order"] for r in self.rows()])

    def test_kind_and_removal(self):
        self.run_apply(SPLITS={}, KINDS={"t_02": "hadith"}, REMOVE={"t_01": "test"})
        self.assertEqual([("t_02", "hadith")], [(r["id"], r["kind"]) for r in self.rows()])

    def test_a_run_not_in_the_entry_stops_everything(self):
        bad = {"t_01": ("", [ce.Part("t_01", "ألف زاي", "Satu dua.", "One two.")])}
        changed, problems = self.run_apply(SPLITS=bad)
        self.assertTrue(problems)
        self.assertEqual("((ألف باء)) قال: ((جيم دال)).", self.rows()[0]["arabic"])


if __name__ == "__main__":
    unittest.main()
