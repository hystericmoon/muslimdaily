"""The transliteration rules, checked on common phrases whose Indonesian reading is well known."""
import unittest

from transliterate import transliterate


class TransliterateTest(unittest.TestCase):
    def check(self, arabic: str, latin: str):
        self.assertEqual(transliterate(arabic), latin)

    def test_basmala_sun_letters_and_joining(self):
        self.check("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ", "Bismillāhir-rahmānir-rahīm")
        self.check("الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Al-hamdu lillāhi rabbil-'ālamīn")

    def test_letters_and_long_vowels(self):
        self.check("أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ", "A'ūdzu billāhi minasy-syaithānir-rajīm")
        self.check("لاَ حَوْلَ وَلاَ قُوَّةَ إِلاَّ بِاللَّهِ", "Lā haula walā quwwata illā billāh")
        self.check("لَا إِلَهَ إِلَّا اللَّهُ", "Lā ilāha illallāh")

    def test_pauses_and_hamza(self):
        self.check(
            "الْحَمْدُ للَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا، وَإِلَيْهِ النُّشُورُ",
            "Al-hamdu lillāhilladzī ahyānā ba'da mā amātanā, wa ilaihin-nusyūr",
        )
        self.check("اللَّهُمَّ أَغِثْنَا، اللَّهُمَّ أَغِثْنَا", "Allāhumma aghitsnā, Allāhumma aghitsnā")

    def test_uthmani_spelling(self):
        self.check(
            "رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً وَفِى ٱلْـَٔاخِرَةِ حَسَنَةً وَقِنَا عَذَابَ ٱلنَّارِ ﴿٢٠١﴾",
            "Rabbanā ātinā fid-dunyā hasanatan wafil-ākhirati hasanatan waqinā 'adzāban-nār",
        )
        self.check("قَالَ رَبِّ ٱشْرَحْ لِى صَدْرِى ﴿٢٥﴾", "Qāla rabbisyrah lī shadrī")

    def test_unwritten_sukun_and_long_vowel(self):
        self.check("أَعوذُ بِكَ", "A'ūdzu bik")
        self.check("أَنتَ", "Ant")

    def test_doubled_letter_without_vowel_gets_none(self):
        # The book writes السّماء and الصّلاة with the shadda but not the fatha.
        # Its vowel can't be read off the letters, so no guess.
        self.assertIsNone(transliterate("فِي السّمَاءِ"))
        self.assertIsNone(transliterate("وَالصّلَاةُ"))
        # Before an alif the vowel can only be a fatha.
        self.check("مِنَ الظّالِمِينَ", "Minazh-zhālimīn")

    def test_name_of_allah_without_its_marks(self):
        # Printed without the shadda and fatha on the doubled lam, it reads as the full form.
        self.check("اللهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى", "Allāhumma innī as'alukal-hudā")
        self.check("بِسْمِ اللهِ", "Bismillāh")
        self.check("الْحَمْدُ لِلهِ", "Al-hamdu lillāh")
        # Only that word: a bare lam elsewhere still can't be read.
        self.assertIsNone(transliterate("ثلاث مرات"))

    def test_helping_vowel_of_min_is_a(self):
        # The book may print the sukun of min; read on into the article it takes a fatha.
        self.check("مِنْ الْخَيْرِ", "Minal-khair")
        self.check("وَمِنْ الشَّرِّ", "Waminasy-syarr")
        self.check("عَنْ الْخَيْرِ", "'anil-khair")

    def test_pausal_form_of_hamza_and_tanwin(self):
        self.check("مِنَ السَّمَاءِ، وَالْأَرْضِ", "Minas-samā', wal-ardh")
        self.check("لَكَ الْحَمْدُ حَمْدًا كَثِيرًا", "Lakal-hamdu hamdan katsīrā")

    def test_joined_jalalah_is_not_capitalised(self):
        self.check("سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ", "Sami'allāhu liman hamidah")

    def test_unreadable_text_gets_none(self):
        # A note with no vowel marks can't be read by the rules: no guess.
        self.assertIsNone(transliterate("ثلاث مرات"))
        # ...but an unreadable bracketed note is only left out.
        self.assertEqual(transliterate("سُبْحَانَ اللَّهِ [ثلاثاً]"), "Subhānallāh")


if __name__ == "__main__":
    unittest.main()
