"""Tests for tools/build_content.py.  Run: python3 -m unittest discover -s tools"""
import csv
import json
import tempfile
import unittest
from pathlib import Path

import build_content as bc

DUA_COLUMNS = [
    "id", "category", "event", "order", "variant_group", "arabic", "is_quran", "translation_id",
    "translation_en", "transliteration", "count", "source_1_book", "source_1_no", "source_2_book",
    "source_2_no", "source_3_book", "source_3_no", "grade", "grader", "alt_gradings", "virtue_text",
    "virtue_source", "audio_file", "audio_segments", "reference", "notes",
]
RAWATIB_COLUMNS = [
    "id", "prayer", "position", "rakah", "tier", "variant_group", "variant_label", "default_variant",
    "evidence_1_book", "evidence_1_no", "evidence_2_book", "evidence_2_no", "notes",
    "hidden_when_travelling",
]


def dua_row(**overrides):
    # Neutral test strings only; no religious text is generated here.
    row = {c: "" for c in DUA_COLUMNS}
    row.update({
        "id": "t_01", "category": "daily", "event": "waking_up", "order": "1",
        "arabic": "ألف | باء", "is_quran": "tidak", "translation_id": "teks uji",
        "source_1_book": "Buku Uji", "source_1_no": "1", "grade": "sahih", "grader": "Penguji",
        "audio_file": "t_01.opus", "audio_segments": "0.0-1.0; 1.0-2.0",
        "reference": "HR. Uji 1.",
    })
    row.update(overrides)
    return row


class BuildContentTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.audio = self.tmp / "audio"
        self.audio.mkdir()
        (self.audio / "t_01.opus").write_bytes(b"x")
        self.rawatib = self.tmp / "rawatib.csv"
        self._write(self.rawatib, RAWATIB_COLUMNS, [])

    def _write(self, path, columns, rows):
        with path.open("w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=columns)
            w.writeheader()
            w.writerows(rows)

    def run_build(self, rows):
        duas = self.tmp / "duas.csv"
        self._write(duas, DUA_COLUMNS, rows)
        report = bc.Report()
        bc.build(duas, self.rawatib, self.tmp, self.audio, report)
        return report

    def test_valid_entry_builds(self):
        report = self.run_build([dua_row()])
        self.assertTrue(report.ok, report.errors)
        data = json.loads((self.tmp / "duas.json").read_text(encoding="utf-8"))
        entry = data["duas"][0]
        self.assertEqual(entry["arabic"], "ألف باء")
        self.assertEqual(entry["phrase_breaks"], [4])
        self.assertEqual(entry["audio"]["segments"], [[0.0, 1.0], [1.0, 2.0]])
        self.assertIsNone(entry["count"])
        self.assertEqual(entry["reference"], "HR. Uji 1.")
        self.assertNotIn("status", entry)

    def test_without_source_fails(self):
        report = self.run_build([dua_row(source_1_book="", source_1_no="")])
        self.assertFalse(report.ok)
        self.assertTrue(any("source" in e for e in report.errors))

    def test_grade_without_grader_fails(self):
        report = self.run_build([dua_row(grader="")])
        self.assertTrue(any("grader" in e for e in report.errors))

    def test_grading_and_audio_are_optional(self):
        report = self.run_build([dua_row(grade="", grader="", audio_file="", audio_segments="")])
        self.assertTrue(report.ok, report.errors)

    def test_named_audio_file_must_exist(self):
        report = self.run_build([dua_row(audio_file="missing.opus")])
        self.assertTrue(any("not found" in e for e in report.errors))

    def test_placeholder_text_fails(self):
        report = self.run_build([dua_row(arabic="[PLACEHOLDER]", audio_segments="0-1")])
        self.assertTrue(any("Arabic" in e for e in report.errors))

    def test_grade_outside_scope_fails(self):
        report = self.run_build([dua_row(grade="dhaif")])
        self.assertTrue(any("outside scope" in e for e in report.errors))

    def test_segments_must_match_phrases(self):
        report = self.run_build([dua_row(audio_segments="0.0-2.0")])
        self.assertTrue(any("phrase" in e for e in report.errors))

    def test_overlapping_segments_fail(self):
        report = self.run_build([dua_row(audio_segments="0.0-1.5; 1.0-2.0")])
        self.assertTrue(any("overlaps" in e for e in report.errors))

    def test_count_must_be_positive_whole_number(self):
        report = self.run_build([dua_row(count="0")])
        self.assertTrue(any("count" in e for e in report.errors))
        report = self.run_build([dua_row(count="tiga")])
        self.assertTrue(any("count" in e for e in report.errors))

    def test_duplicate_ids_fail(self):
        report = self.run_build([dua_row(), dua_row()])
        self.assertTrue(any("duplicate" in e for e in report.errors))

    def test_unknown_event_fails(self):
        report = self.run_build([dua_row(event="nope")])
        self.assertTrue(any("unknown event" in e for e in report.errors))

    def test_version_bumps_only_on_change(self):
        self.run_build([dua_row()])
        v1 = json.loads((self.tmp / "duas.json").read_text(encoding="utf-8"))["content_version"]
        self.run_build([dua_row()])
        v2 = json.loads((self.tmp / "duas.json").read_text(encoding="utf-8"))["content_version"]
        self.run_build([dua_row(translation_id="teks lain")])
        v3 = json.loads((self.tmp / "duas.json").read_text(encoding="utf-8"))["content_version"]
        self.assertEqual(v1, v2)
        self.assertEqual(v3, v2 + 1)

    def test_nothing_written_when_errors(self):
        self.run_build([dua_row(grader="")])
        self.assertFalse((self.tmp / "duas.json").exists())

    def test_rawatib_notes_cannot_be_placeholders(self):
        self._write(self.rawatib, RAWATIB_COLUMNS, [{
            **{c: "" for c in RAWATIB_COLUMNS}, "id": "r1", "prayer": "FAJR", "position": "BEFORE",
            "rakah": "2", "tier": "MUAKKADAH", "hidden_when_travelling": "tidak", "notes": "[PLACEHOLDER] x",
        }])
        report = self.run_build([dua_row()])
        self.assertTrue(any("placeholder" in e for e in report.errors))

    def test_split_phrases(self):
        text, breaks = bc.split_phrases(" a |bb| ccc ")
        self.assertEqual(text, "a bb ccc")
        self.assertEqual(breaks, [2, 5])


if __name__ == "__main__":
    unittest.main()
