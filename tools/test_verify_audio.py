"""Tests for tools/verify_audio.py (the comparison; the model is not run).  Run: python3 -m unittest discover -s tools"""
import unittest

import verify_audio as va

# Neutral placeholder words (consonants only, as the skeleton keeps them); no religious text is generated here.


class JudgeTest(unittest.TestCase):
    def test_one_misheard_word_passes(self):
        self.assertEqual((2, "ok"), va.judge(["كتب", "سلم", "درس"], ["كتب", "قلب", "درس"]))

    def test_a_title_read_before_the_phrase_is_flagged(self):
        self.assertEqual("check", va.judge(["كتب", "سلم"], ["حمد", "خطب", "رسم", "كتب", "سلم"])[1])

    def test_a_phrase_missing_from_its_segment_is_flagged(self):
        self.assertEqual("check", va.judge(["كتب", "سلم", "درس", "فهم"], ["كتب", "سلم"])[1])


class SkeletonTest(unittest.TestCase):
    def test_spellings_with_and_without_long_vowels_agree(self):
        self.assertEqual("ok", va.judge(["كتب", "سلم"], ["كاتب", "سالم"])[1])


class EdgesTest(unittest.TestCase):
    def test_words_fused_inside_a_phrase_pass(self):
        w = ["كتب", "سلم", "درس", "فهم", "نصر", "شرب", "قلب"]
        self.assertEqual("ok", va.judge(w, ["كتب", "سلمدرس", "فهم", "نصر", "شرب", "قلب"])[1])

    def test_a_phrase_cut_short_is_flagged(self):
        self.assertEqual("check", va.judge(["كتب", "سلم", "درس", "فهم", "نصر"], ["كتب", "سلم", "درس"])[1])


class SegmentsTest(unittest.TestCase):
    def test_segments_then_range_then_whole_file(self):
        self.assertEqual([(0.0, 1.5), (1.5, 3.0)], va.segments_of({"audio_segments": "0.0-1.5; 1.5-3.0"}))
        self.assertEqual([(2.0, 4.0)], va.segments_of({"audio_segments": "", "audio_range": "2.0-4.0"}))
        self.assertEqual([(0.0, 0.0)], va.segments_of({"audio_segments": "", "audio_range": ""}))


if __name__ == "__main__":
    unittest.main()
