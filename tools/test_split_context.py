"""Tests for tools/split_context.py.  Run: python3 -m unittest discover -s tools"""
import csv
import tempfile
import unittest
from pathlib import Path

import build_content as bc
import split_context as sc
from test_build_content import DUA_COLUMNS, dua_row
from test_segment_audio import silence, tone

# Neutral test strings only: placeholder words ("ألف باء …") around the book's own markup and
# cue words ("let him say", "three times"). No religious text is generated here.


class ProposeTest(unittest.TestCase):
    def test_clean_entry_is_left_alone(self):
        self.assertEqual(("((ألف باء جيم))", []), sc.propose_arabic("((ألف باء جيم))"))

    def test_count_after_the_quote_is_context(self):
        part, rules = sc.propose_arabic("((ألف باء جيم)) ثلاث مرَّاتٍ.")
        self.assertEqual("((ألف باء جيم))", part)
        self.assertEqual(["count"], rules)

    def test_count_in_parentheses_is_context(self):
        part, _ = sc.propose_arabic("((ألف باء (ثلاثاً) جيم دال))")
        self.assertEqual("((ألف باء جيم دال))", part)

    def test_or_that_introduced_a_dropped_note_goes_with_it(self):
        part, _ = sc.propose_arabic("((ألف باء جيم)) ، أَوْ (مرَّةً واحدةً عندَ الكَسَلِ).")
        self.assertEqual("((ألف باء جيم))", part)

    def test_or_inside_the_dua_stays(self):
        part, _ = sc.propose_arabic("((ألف أَوْ باء (ثلاثاً) جيم))")
        self.assertEqual("((ألف أَوْ باء جيم))", part)

    def test_instruction_in_brackets_is_context_but_added_words_stay(self):
        part, rules = sc.propose_arabic("((ألف باء جيم)). [وإذا أمسى قال: دال هاء]")
        self.assertEqual("((ألف باء جيم))", part)
        self.assertEqual(["note"], rules)
        self.assertEqual(("([دال] ألف باء)", []), sc.propose_arabic("([دال] ألف باء)"))

    def test_narration_before_the_quote_is_context(self):
        part, rules = sc.propose_arabic("يَبْدَأُ بِرِجْلِهِ ، وَيَقُولُ: ((ألف باء جيم))")
        self.assertEqual("((ألف باء جيم))", part)
        self.assertEqual(["narration"], rules)

    def test_a_cue_word_inside_the_dua_is_not_an_intro(self):
        self.assertEqual([], sc.propose_arabic("((ألف باء، أحق ما قال العبد، جيم دال))")[1])

    def test_several_sayings_keep_each_piece(self):
        part, rules = sc.propose_arabic("((إذا فعل أحدكم فليقل ألف باء، فإن نسي فليقل جيم دال))")
        self.assertEqual("ألف باء، جيم دال", part)
        self.assertIn("several", rules)
        self.assertEqual("check", sc.confidence(part, rules))

    def test_a_proposal_only_leaves_words_out(self):
        for text in ["((ألف باء)) ثلاث مرات", "قال: ((ألف باء))", "((ألف (سبعا) باء))", "[وإذا قال: جيم] ((ألف))"]:
            part, _ = sc.propose_arabic(text)
            self.assertTrue(bc.is_part_of(part, text), text)

    def test_translation_keeps_the_quoted_dua_when_it_carries_narration(self):
        self.assertEqual("satu dua", sc.propose_translation("Beliau bersabda: “satu dua”"))
        self.assertEqual("“satu dua”", sc.propose_translation("“satu dua”"))


class RangeTest(unittest.TestCase):
    def test_range_snaps_to_the_pause_after_the_intro(self):
        # 1 s of intro, a pause, 2 s of dua.
        samples = silence(0.2) + tone(1.0) + silence(0.5) + tone(2.0) + silence(0.2)
        rng = sc.propose_range("جيم: ((ألف باء دال هاء واو زاي))", "((ألف باء دال هاء واو زاي))", samples)
        a, b = bc.parse_segments(rng)[0]
        self.assertAlmostEqual(1.45, a, delta=0.15)
        self.assertGreater(b, 3.5)


class ApplyTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.duas = self.tmp / "duas.csv"
        with self.duas.open("w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=DUA_COLUMNS)
            w.writeheader()
            w.writerow(dua_row(id="a", arabic="قال: ((ألف باء))", audio_segments="", translation_id="Ia berkata: “satu dua”",
                               translation_en="He said:‘one two’"))

    def review(self, **over):
        _, rows, _ = sc.sa.read_csv(self.duas)
        r = sc.suggest_row(rows[0], None)
        r.update(over)
        sc.sa.write_csv(self.tmp / "review.csv", sc.REVIEW_FIELDS, [r])
        return sc.apply(self.tmp / "review.csv", self.duas)

    def test_apply_keeps_the_full_entry_and_writes_the_dua(self):
        applied, problems = self.review(approved="ya", audio_range="0.50-1.50")
        self.assertEqual((["a"], []), (applied, problems))
        _, rows, _ = sc.sa.read_csv(self.duas)
        self.assertEqual("((ألف باء))", rows[0]["arabic"])
        self.assertEqual("قال: ((ألف باء))", rows[0]["arabic_full"])
        self.assertEqual("satu dua", rows[0]["translation_id"])
        self.assertEqual("0.50-1.50", rows[0]["audio_range"])

    def test_apply_refuses_added_words_and_skips_unapproved(self):
        self.assertEqual(([], []), self.review())
        applied, problems = self.review(approved="ya", arabic="((ألف زاي))")
        self.assertEqual([], applied)
        self.assertTrue(problems)

    def test_apply_cuts_the_english_too(self):
        applied, problems = self.review(approved="ya", translation_en="one two")
        self.assertEqual((["a"], []), (applied, problems))
        _, rows, _ = sc.sa.read_csv(self.duas)
        self.assertEqual("one two", rows[0]["translation_en"])
        self.assertEqual("He said:‘one two’", rows[0]["translation_en_full"])

    def test_apply_refuses_english_that_is_not_in_the_source(self):
        applied, problems = self.review(approved="ya", translation_en="one three")
        self.assertEqual([], applied)
        self.assertTrue(problems)


if __name__ == "__main__":
    unittest.main()
