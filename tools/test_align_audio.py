"""Tests for tools/align_audio.py (the proposal step; timings are given).  Run: python3 -m unittest discover -s tools"""
import unittest

import align_audio as aa

# Neutral placeholder words ("ألف باء …") only; no religious text is generated here.


def entry(text, words, duration=10.0):
    return {"file": "t.opus", "duration": duration, "text_sha1": aa.sha1(text), "words": words}


class ProposeTest(unittest.TestCase):
    def test_breaks_go_at_punctuation_where_the_reciter_pauses(self):
        text = "ألف باء، جيم دال، هاء واو"
        words = [[0.5, 1.0, .9], [1.1, 1.8, .9], [2.6, 3.0, .9], [3.1, 3.9, .9], [4.0, 4.5, .9], [4.6, 5.4, .9]]
        out = aa.propose({"id": "t", "arabic": text}, entry(text, words))
        self.assertEqual("ok", out["status"])
        # a pause after the first comma (1.8 -> 2.6), none after the second (3.9 -> 4.0)
        self.assertEqual("ألف باء، | جيم دال، هاء واو", out["arabic_marked"])
        self.assertEqual("0.25-2.20; 2.20-5.65", out["audio_segments"])
        self.assertEqual("0.25-5.65", out["audio_range"])  # more than a second of silence after

    def test_a_dua_inside_its_entry_gets_its_own_range(self):
        full = "قال: ((ألف باء جيم)) ثلاثا"
        words = [[0.2, 0.8, .9], [2.0, 2.4, .9], [2.5, 2.9, .9], [3.0, 3.6, .9], [5.0, 5.8, .9]]
        out = aa.propose({"id": "t", "arabic": "ألف باء جيم", "arabic_full": full}, entry(full, words, 6.0))
        self.assertEqual(("ok", "1.75-3.85"), (out["status"], out["audio_range"]))

    def test_a_recording_that_does_not_match_is_not_cut(self):
        text = "ألف باء جيم دال هاء"
        poor = [[0, 1, .9], [1, 2, .1], [2, 3, .2], [3, 4, .3], [4, 5, .9]]
        out = aa.propose({"id": "t", "arabic": text}, entry(text, poor))
        self.assertEqual("fail", out["status"])
        self.assertIn("low confidence", out["reason"])

    def test_an_unsure_edge_next_to_read_words_is_not_cut(self):
        full = "ألف باء جيم دال"
        words = [[0, 1, .9], [1, 2, .2], [2, 3, .9], [3, 4, .9]]
        out = aa.propose({"id": "t", "arabic": "باء جيم", "arabic_full": full}, entry(full, words))
        self.assertEqual("fail", out["status"])

    def test_a_short_dhikr_at_the_start_needs_only_sure_edges(self):
        full = "ألف باء ثلاثا جيم دال"
        words = [[0.5, 2.5, .2], [2.6, 3.0, .9], [3.1, 6.0, .7], [7.0, 7.5, .9], [7.6, 8.0, .9]]
        out = aa.propose({"id": "t", "arabic": "ألف باء", "arabic_full": full}, entry(full, words))
        self.assertEqual(("ok", "0.25-3.05"), (out["status"], out["audio_range"]))

    def test_changed_text_is_refused(self):
        out = aa.propose({"id": "t", "arabic": "ألف باء"}, entry("ألف جيم", [[0, 1, .9], [1, 2, .9]]))
        self.assertIn("text changed", out["reason"])

    def test_locate_ignores_marks_and_brackets(self):
        self.assertEqual(2, aa.locate("بَاء جيم", "قال: ((ألف باء جيم))"))
        self.assertIsNone(aa.locate("باء ألف", "ألف باء"))


if __name__ == "__main__":
    unittest.main()
