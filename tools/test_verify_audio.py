"""Tests for tools/verify_audio.py (the comparison; the model is not run).  Run: python3 -m unittest discover -s tools"""
import unittest

import verify_audio as va

# Neutral placeholder words only; no religious text is generated here.


class JudgeTest(unittest.TestCase):
    def test_one_misheard_word_passes(self):
        self.assertEqual((2, "ok"), va.judge(["ألف", "باء", "جيم"], ["ألف", "زاي", "جيم"]))

    def test_a_title_read_before_the_phrase_is_flagged(self):
        self.assertEqual("check", va.judge(["ألف", "باء"], ["كاف", "لام", "ميم", "ألف", "باء"])[1])

    def test_a_phrase_missing_from_its_segment_is_flagged(self):
        self.assertEqual("check", va.judge(["ألف", "باء", "جيم", "دال"], ["ألف", "باء"])[1])


class SegmentsTest(unittest.TestCase):
    def test_segments_then_range_then_whole_file(self):
        self.assertEqual([(0.0, 1.5), (1.5, 3.0)], va.segments_of({"audio_segments": "0.0-1.5; 1.5-3.0"}))
        self.assertEqual([(2.0, 4.0)], va.segments_of({"audio_segments": "", "audio_range": "2.0-4.0"}))
        self.assertEqual([(0.0, 0.0)], va.segments_of({"audio_segments": "", "audio_range": ""}))


if __name__ == "__main__":
    unittest.main()
