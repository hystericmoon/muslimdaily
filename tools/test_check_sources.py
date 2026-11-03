"""Tests for tools/check_sources.py (no network).  Run: cd tools && python3 -m unittest"""
import unittest

import check_sources as cs


class CompareTest(unittest.TestCase):
    def test_fingerprint_ignores_whitespace_only(self):
        self.assertEqual(cs.fingerprint("a  b\nc"), cs.fingerprint("a b c"))
        self.assertNotEqual(cs.fingerprint("a b c"), cs.fingerprint("a b d"))

    def test_reports_changed_new_and_gone_entries(self):
        old = {"hisnmuslim_ar": {"1": "x", "2": "y"}, "hisnmuslim_en": {"1": "x"}, "en_pdf_sha256": "p"}
        new = {"hisnmuslim_ar": {"1": "x", "3": "z"}, "hisnmuslim_en": {"1": "w"}, "en_pdf_sha256": "q"}
        self.assertEqual(cs.compare(old, new), [
            "hisnmuslim.com Arabic: Hisnul Muslim no. 2 gone",
            "hisnmuslim.com Arabic: Hisnul Muslim no. 3 new",
            "hisnmuslim.com English: Hisnul Muslim no. 1 changed",
            f"IslamHouse English PDF changed: {cs.EN_PDF}",
        ])

    def test_no_changes(self):
        state = {"hisnmuslim_ar": {"1": "x"}, "hisnmuslim_en": {}, "en_pdf_sha256": "p"}
        self.assertEqual(cs.compare(state, dict(state)), [])


if __name__ == "__main__":
    unittest.main()
