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


class UnheardTest(unittest.TestCase):
    ROW = {"id": "t", "arabic": "كتب سلم | درس فهم] [نصر شرب علم] [حمد خطب ذكر] [صبر شكر حلم]",
           "audio_range": "", "audio_segments": "0.00-2.00; 2.00-9.00"}
    # the second part ("نصر شرب علم") and the last are not said; "حمد" shares nothing with them
    WORDS = [[0.0, 0.5, "كتب"], [0.5, 1.0, "سلم"], [2.2, 2.7, "درس"], [2.7, 3.2, "فهم"],
             [5.0, 5.5, "حمد"], [5.5, 6.0, "خطب"], [6.0, 6.5, "ذكر"]]

    def test_parts_the_recording_leaves_out_get_no_segment(self):
        out = ra.propose_unheard(self.ROW, entry(self.WORDS, 9.0, quiet=[(3.3, 4.9), (6.6, 9.0)]), 2)
        self.assertEqual("ok", out["status"], out["reason"])
        segs = out["audio_segments"].split("; ")
        self.assertEqual(["0.00-2.00", "-", "-"], [segs[0], segs[2], segs[4]])
        a, b = (float(x) for x in segs[1].split("-"))
        c, d = (float(x) for x in segs[3].split("-"))
        self.assertEqual((2.0, 9.0), (a, d))
        self.assertEqual(b, c)
        self.assertTrue(3.3 <= b <= 5.1, b)
        self.assertEqual("كتب سلم | درس فهم] | [نصر شرب علم] | [حمد خطب ذكر] | [صبر شكر حلم]", out["arabic"])

    def test_a_part_heard_only_in_half_is_refused(self):
        # one word of three: too many to call the part missing, too few to call it heard
        words = self.WORDS[:4] + [[4.0, 4.4, "نصر"], [4.4, 4.6, "سجد"]] + self.WORDS[4:]
        out = ra.propose_unheard(self.ROW, entry(words, 9.0, quiet=[(3.3, 3.9), (6.6, 9.0)]), 2)
        self.assertEqual("fail", out["status"])
        self.assertIn("part 2", out["reason"])

    def test_a_phrase_without_bracketed_parts_is_refused(self):
        row = dict(self.ROW, arabic="كتب سلم | درس فهم")
        self.assertEqual("fail", ra.propose_unheard(row, entry(self.WORDS, 9.0), 2)["status"])


class JoinTest(unittest.TestCase):
    ROW = {"id": "t", "arabic": "كتب سلم، | درس فهم | نصر شرب", "audio_range": "1.00-9.00",
           "audio_segments": "1.00-3.00; 3.00-6.00; 6.00-9.00"}

    def test_a_title_only_segment_is_dropped_and_the_phrases_joined(self):
        out = ra.propose_join(self.ROW, entry([[0.5, 2.5, "قول"], [3.2, 4.0, "كتب"]], 9.0), 1)
        self.assertEqual("ok", out["status"], out["reason"])
        self.assertEqual("كتب سلم، درس فهم | نصر شرب", out["arabic"])
        self.assertEqual("3.00-6.00; 6.00-9.00", out["audio_segments"])
        self.assertEqual("3.00-9.00", out["audio_range"])

    def test_refused_when_the_earlier_segment_has_the_phrase(self):
        out = ra.propose_join(self.ROW, entry([[1.2, 2.0, "كتب"]], 9.0), 1)
        self.assertEqual("fail", out["status"])


class EndTest(unittest.TestCase):
    ROW = {"id": "t", "arabic": "كتب سلم | درس فهم", "audio_range": "1.00-5.00",
           "audio_segments": "1.00-3.00; 3.00-5.00"}

    def test_a_held_last_sound_is_kept_until_it_dies_away(self):
        out = ra.propose_end(self.ROW, entry([], 9.0, quiet=[(5.3, 9.0)]))
        self.assertEqual("ok", out["status"], out["reason"])
        self.assertEqual("1.00-5.30", out["audio_range"])
        self.assertEqual("1.00-3.00; 3.00-5.30", out["audio_segments"])

    def test_the_end_stops_before_the_next_word(self):
        e = entry([], 9.0)
        for i in range(int(5.0 / ra.FRAME), int(5.6 / ra.FRAME)):
            e["rms"][i] = -12.0
        for i in range(int(5.2 / ra.FRAME), int(5.24 / ra.FRAME)):
            e["rms"][i] = -24.0  # a short dip, then the next word at -5 dB
        for i in range(int(5.3 / ra.FRAME), int(9.0 / ra.FRAME)):
            e["rms"][i] = -5.0
        out = ra.propose_end(self.ROW, e)
        self.assertEqual("ok", out["status"], out["reason"])
        self.assertEqual("1.00-5.20", out["audio_range"])

    def test_an_end_already_in_silence_is_left_alone(self):
        out = ra.propose_end(self.ROW, entry([], 9.0, quiet=[(4.98, 9.0)]))
        self.assertEqual("fail", out["status"])

    def test_no_quiet_point_is_refused(self):
        out = ra.propose_end(self.ROW, entry([], 9.0))
        self.assertEqual("fail", out["status"])


if __name__ == "__main__":
    unittest.main()
