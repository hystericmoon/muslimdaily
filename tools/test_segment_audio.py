"""Tests for tools/segment_audio.py.  Run: python3 -m unittest discover -s tools"""
import csv
import math
import tempfile
import unittest
from pathlib import Path

import segment_audio as sa
from test_build_content import DUA_COLUMNS, dua_row

RATE = sa.SAMPLE_RATE


def tone(seconds):
    return [0.5 * math.sin(2 * math.pi * 220 * i / RATE) for i in range(int(seconds * RATE))]


def silence(seconds):
    return [0.0] * int(seconds * RATE)


def levels(*parts):
    """('s', 1.0) = one second of speech at 0 dB, ('q', 0.5) = half a second of quiet."""
    out = []
    for kind, seconds in parts:
        out += [0.0 if kind == "s" else -60.0] * round(seconds / sa.FRAME)
    return out


# Neutral test strings only; no religious text is generated here.
TEXT = "ألف باء، جيم دال. هاء واو"


class PauseTest(unittest.TestCase):
    def test_loudness_separates_tone_from_silence(self):
        db = sa.loudness(tone(0.1) + silence(0.1))
        self.assertGreater(db[0], -10)
        self.assertLess(db[-1], -100)

    def test_finds_pauses_inside_speech_only(self):
        start, end, pauses = sa.find_pauses(levels(("q", 0.5), ("s", 1), ("q", 0.4), ("s", 1), ("q", 1)))
        self.assertEqual((start, end), (0.5, 2.9))
        self.assertEqual(pauses, [sa.Pause(1.5, 1.9)])

    def test_ignores_short_gaps_and_leading_clicks(self):
        start, _, pauses = sa.find_pauses(
            levels(("s", 0.1), ("q", 0.5), ("s", 1), ("q", 0.1), ("s", 1), ("q", 0.3), ("s", 1)))
        self.assertEqual(start, 0.6)  # the 0.1 s click is not speech
        self.assertEqual([p.start for p in pauses], [2.7])  # the 0.1 s gap is not a pause


class TextTest(unittest.TestCase):
    def test_breaks_follow_existing_punctuation(self):
        spots = sa.break_candidates(TEXT)
        self.assertEqual([TEXT[s - 1] for s in spots], ["،", "."])

    def test_break_after_verse_number_and_before_quote(self):
        text = "ألف ۝١٢ باء ﴿جيم دال﴾ هاء"
        marked = sa.mark(text, sa.break_candidates(text))
        self.assertEqual(marked, "ألف ۝١٢ | باء | ﴿جيم دال﴾ | هاء")

    def test_marking_never_changes_the_text(self):
        marked = sa.mark(TEXT, sa.break_candidates(TEXT))
        self.assertEqual(sa.bc.split_phrases(marked)[0], TEXT)
        with self.assertRaises(ValueError):
            sa.mark("ألف  باء، جيم", [4])  # would collapse a double space


class AlignTest(unittest.TestCase):
    def test_pairs_in_order_and_skips_extra_pauses(self):
        spots = sa.break_candidates(TEXT)
        # Breaks sit at about 40% and 70% of the letters; an extra pause near the start is unpaired.
        pauses = [sa.Pause(0.9, 1.1), sa.Pause(3.9, 4.1), sa.Pause(6.9, 7.1)]
        pairs = sa.align(TEXT, spots, 0.0, 10.0, pauses)
        self.assertEqual([p for _, p in pairs], pauses[1:])

    def test_far_pause_is_not_paired(self):
        spots = sa.break_candidates(TEXT)
        pairs = sa.align(TEXT, spots, 0.0, 100.0, [sa.Pause(99.0, 99.5)])
        self.assertEqual(pairs, [])

    def test_segments_are_contiguous(self):
        segs = sa.segments(10.0, [sa.Pause(3.0, 4.0), sa.Pause(6.0, 7.0)])
        self.assertEqual(segs, [(0.0, 3.5), (3.5, 6.5), (6.5, 10.0)])

    def test_segments_start_at_the_dua_range(self):
        self.assertEqual([(2.0, 3.5), (3.5, 5.0)], sa.segments(3.0, [sa.Pause(1.0, 2.0)], offset=2.0))

    def test_status(self):
        self.assertEqual(sa.review_status(2, 2, 2), "match")
        self.assertEqual(sa.review_status(1, 5, 1), "close")
        self.assertEqual(sa.review_status(4, 4, 2), "partial")
        self.assertEqual(sa.review_status(0, 3, 0), "none")


class SuggestApplyTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.duas = self.tmp / "duas.csv"
        self.out = self.tmp / "out"
        rows = [
            dua_row(id="a_01", event="morning", arabic=TEXT, audio_file="a.opus", audio_segments=""),
            dua_row(id="b_01", event="evening", arabic=TEXT, audio_file="b.opus", audio_segments=""),
            dua_row(id="c_01", event="morning", arabic="ألف | باء"),  # already segmented
        ]
        with self.duas.open("w", encoding="utf-8", newline="") as f:
            w = csv.DictWriter(f, fieldnames=DUA_COLUMNS)
            w.writeheader()
            w.writerows(rows)
        # Speech with pauses at about 40% and 70% of the way, matching the punctuation.
        self.audio = tone(4) + silence(0.4) + tone(2.6) + silence(0.4) + tone(2.6)

    def suggest(self, **kw):
        return sa.suggest(kw.get("ids", set()), kw.get("events", {"morning"}), self.duas,
                          self.tmp, self.out, decoder=lambda _: self.audio)

    def rows(self, path):
        with path.open(encoding="utf-8") as f:
            return {r["id"]: r for r in csv.DictReader(f)}

    def approve(self, **changes):
        review = self.rows(self.out / "review.csv")
        for did, fields in changes.items():
            review[did].update(fields)
        sa.write_csv(self.out / "review.csv", sa.REVIEW_FIELDS, list(review.values()))

    def test_suggest_selects_unsegmented_rows_and_writes_review_files(self):
        results = self.suggest()
        self.assertEqual([r["id"] for r in results], ["a_01"])
        r = results[0]
        self.assertEqual((r["status"], r["phrases"]), ("match", 3))
        self.assertEqual(r["arabic_marked"], "ألف باء، | جيم دال. | هاء واو")
        self.assertEqual(len(sa.bc.parse_segments(r["audio_segments"])), 3)
        page = (self.out / "review.html").read_text(encoding="utf-8")
        self.assertIn('src="../a.opus"', page)
        self.assertIn('lang="ar"', page)

    def test_apply_writes_only_approved_rows(self):
        self.suggest(events={"morning", "evening"})
        self.approve(a_01={"approved": "ya"})
        applied, problems = sa.apply(self.out / "review.csv", self.duas)
        self.assertEqual((applied, problems), (["a_01"], []))
        rows = self.rows(self.duas)
        self.assertIn("|", rows["a_01"]["arabic"])
        self.assertEqual(rows["b_01"]["arabic"], TEXT)
        self.assertEqual(list(rows["a_01"].keys()), DUA_COLUMNS)

    def test_apply_refuses_changed_source_or_edited_words(self):
        self.suggest(events={"morning", "evening"})
        self.approve(a_01={"approved": "ya", "arabic_marked": "ألف باء، | جيم | هاء واو"},
                     b_01={"approved": "ya"})
        rows = self.rows(self.duas)
        rows["b_01"]["arabic"] = TEXT + " زاي"
        sa.write_csv(self.duas, DUA_COLUMNS, list(rows.values()))
        applied, problems = sa.apply(self.out / "review.csv", self.duas)
        self.assertEqual(applied, [])
        self.assertEqual(len(problems), 2)
        self.assertIn("beyond the '|' marks", problems[0])
        self.assertIn("changed since", problems[1])

    def test_apply_refuses_count_mismatch(self):
        self.suggest()
        self.approve(a_01={"approved": "ya", "audio_segments": "0.00-5.00; 5.00-10.00"})
        applied, problems = sa.apply(self.out / "review.csv", self.duas)
        self.assertEqual(applied, [])
        self.assertIn("3 phrases but 2 segments", problems[0])


if __name__ == "__main__":
    unittest.main()
