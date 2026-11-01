"""Tests for tools/recut_audio.py (the proposal; Whisper's output is given).  Run: python3 -m unittest discover -s tools"""
import unittest

import recut_audio as ra

# Neutral placeholder words (consonants only) and a made-up title; no religious text here.


def entry(words, duration=10.0, quiet=()):
    rms = [-5.0] * int(duration / ra.FRAME)
    for a, b in quiet:
        for i in range(int(a / ra.FRAME), int(b / ra.FRAME)):
            rms[i] = -40.0
    return {"duration": duration, "words": words, "rms": rms}


class NearTest(unittest.TestCase):
    def test_a_dropped_suffix_or_one_letter_still_matches(self):
        self.assertTrue(ra.near("ثبت", "ثبته"))
        self.assertTrue(ra.near("كتب", "كتم"))
        self.assertFalse(ra.near("كتب", "نصر"))
        self.assertFalse(ra.near("كت", "كتبه"))  # too short to call a prefix


class ProposeTest(unittest.TestCase):
    def test_a_title_before_the_dua_is_cut_off(self):
        words = [[0.0, 0.8, "حمد"], [0.8, 1.6, "خطب"], [3.0, 3.5, "كتب"], [3.5, 4.0, "سلم"],
                 [4.0, 4.5, "درس"], [6.0, 6.5, "فهم"], [6.5, 7.0, "نصر"], [7.0, 7.5, "شرب"]]
        row = {"id": "t", "arabic": "كتب سلم درس | فهم نصر شرب", "audio_range": "", "audio_segments": "0.00-5.00; 5.00-10.00"}
        out = ra.propose(row, entry(words, quiet=[(1.7, 2.9), (4.6, 5.9)]))
        self.assertEqual("ok", out["status"], out["reason"])
        a, b = (float(x) for x in out["audio_range"].split("-"))
        self.assertTrue(2.6 <= a < 3.0, a)  # after the title, just before the first word
        self.assertEqual(10.0, b)
        cut = float(out["audio_segments"].split("; ")[1].split("-")[0])
        self.assertTrue(4.55 <= cut <= 5.9, cut)

    def test_a_phrase_not_heard_is_not_recut(self):
        words = [[0.0, 0.5, "كتب"], [0.5, 1.0, "سلم"], [1.0, 1.5, "درس"]]
        row = {"id": "t", "arabic": "كتب سلم درس | فهم نصر شرب", "audio_range": "", "audio_segments": "0.00-1.50; 1.50-3.00"}
        self.assertEqual("fail", ra.propose(row, entry(words, 3.0))["status"])

    def test_an_edge_in_loud_audio_is_refused(self):
        words = [[0.0, 0.8, "حمد"], [0.8, 1.6, "خطب"], [1.6, 2.0, "كتب"], [2.0, 2.5, "سلم"], [2.5, 3.0, "درس"]]
        row = {"id": "t", "arabic": "كتب سلم درس", "audio_range": "", "audio_segments": ""}
        out = ra.propose(row, entry(words, 4.0))  # no quiet stretch anywhere
        self.assertEqual("fail", out["status"])
        self.assertIn("quiet", out["reason"])


if __name__ == "__main__":
    unittest.main()
