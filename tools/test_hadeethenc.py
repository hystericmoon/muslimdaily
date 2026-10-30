"""The HadeethEnc import: only the dua is cut from each hadith, nothing is written."""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "hadeethenc"))
from build_content import is_part_of  # noqa: E402
from import_hadeethenc import EXCLUDED, SELECTED, SNAPSHOT, mark_phrases, quote, rows, span  # noqa: E402

FIELDS = ["id", "category", "event", "order", "arabic", "arabic_full", "translation_id", "translation_id_full",
          "translation_en", "translation_en_full", "grade", "grader"] + [
          f"source_{n}_{k}" for n in (1, 2, 3) for k in ("book", "no")] + ["reference", "notes", "is_quran"]


class HadeethEncTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rows = rows(json.loads(SNAPSHOT.read_text("utf-8")), FIELDS)

    def test_every_dua_is_cut_from_its_hadith(self):
        self.assertEqual(len(self.rows), len(SELECTED))
        for r in self.rows:
            with self.subTest(r["id"]):
                self.assertTrue(is_part_of(r["arabic"], r["arabic_full"]))
                self.assertIn(r["translation_id"], r["translation_id_full"])
                self.assertIn(r["translation_en"], r["translation_en_full"])
                self.assertIn(r["grade"], {"sahih", "hasan"})

    def test_excluded_hadiths_are_not_imported(self):
        self.assertFalse({s[0] for s in SELECTED} & set(EXCLUDED))

    def test_helpers_keep_the_text(self):
        self.assertEqual(quote("قَالَ: «أ ب»، ثُمَّ «ج»", 1), "ج")
        self.assertEqual(span('He said: "O Allah, guide me." Then', "O Allah", "me."), "O Allah, guide me.")
        self.assertEqual(mark_phrases("أ، ب، ج"), "أ، | ب، | ج")
        with self.assertRaises(ValueError):
            span("text", "missing", "end")


if __name__ == "__main__":
    unittest.main()
