"""Tests for the Hisnul Muslim importer helpers (no PDF or network needed)."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "hisnul_muslim"))
import import_hisn  # noqa: E402
import weak_appendix  # noqa: E402


class TakhrijTest(unittest.TestCase):
    def test_books_and_numbers(self):
        self.assertEqual(
            import_hisn.split_takhrij("HR. Al-Bukhari 7/162, Muslim 4/2083."),
            [("Al-Bukhari", "7/162"), ("Muslim", "4/2083")],
        )

    def test_number_after_comma_attaches_to_book(self):
        self.assertEqual(import_hisn.split_takhrij("HR. Al-Hakim, 1/562. Al-Albani ..."), [("Al-Hakim", "1/562")])

    def test_grading_remark_is_not_a_source(self):
        self.assertEqual(
            import_hisn.split_takhrij("HR. Abu Dawud 1/303, dinyatakan shahih oleh Al-Albani dalam Shahih Abi Dawud 1/216."),
            [("Abu Dawud", "1/303")],
        )

    def test_no_takhrij(self):
        self.assertEqual(import_hisn.split_takhrij("Lihat Zadul Ma'ad."), [])


class GradeTest(unittest.TestCase):
    def test_stated_verdicts(self):
        self.assertEqual(import_hisn.albani_grade("dinyatakan hasan oleh Al-Albani"), "hasan")
        self.assertEqual(import_hisn.albani_grade("Al-Albani menshahihkannya dalam Shahih An-Nasai"), "sahih")

    def test_no_verdict_without_albani_or_when_weak(self):
        self.assertEqual(import_hisn.albani_grade("HR. Al-Bukhari 7/162."), "")
        self.assertEqual(import_hisn.albani_grade("didla'ifkan oleh Al-Albani"), "")


class WeakAppendixTest(unittest.TestCase):
    APPENDIX = (
        "HADITS-HADITS LEMAH HISNUL MUSLIM 1. Do'a Ketika Masuk Rumah. "
        "“Dengan menyebut nama Allah kami masuk (ke rumah), dan dengan nama Allah kami keluar” "
        "(HR. Abu Dawud 4/325) Hadits ini didla’ifkan oleh Syaikh Al-Albani. "
        "4. Dzikir-dzikir Ketika Tidur. “Ya Allah! Jauhkanlah aku dari siksaan-Mu pada hari Engkau "
        "membangkitkan hamba-hamba-Mu” (dibaca 3 kali) (HR. Abu Dawud 4/311). Hadits ini dishahihkan oleh Al-Albani."
    )

    def test_matches_within_section_and_keeps_verdict(self):
        items = weak_appendix.items(self.APPENDIX)
        found = weak_appendix.match(
            items,
            {18: "“Dengan nama Allah, kami masuk (ke rumah), dengan nama Allah, kami keluar”",
             104: "“Ya Allah! Jauhkanlah aku dari siksaanMu pada hari Engkau membangkitkan hamba-hambaMu.”",
             5: "“Dengan nama Allah kami masuk”"},
            {18: "BACAAN APABILA MASUK RUMAH", 104: "BACAAN SEBELUM TIDUR", 5: "DOA PAKAIAN"},
        )
        self.assertTrue(found[18]["weak"])
        self.assertFalse(found[104]["weak"])
        self.assertEqual(found[104]["count"], "(dibaca 3 kali)")
        self.assertNotIn(5, found)


if __name__ == "__main__":
    unittest.main()
