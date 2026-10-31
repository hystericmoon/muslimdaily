#!/usr/bin/env python3
"""Turns Hisnul Muslim entries into rows of only what the reader says.

    python3 -I tools/clean_entries.py          # writes duas.csv, then run align_audio.py and build_content.py

Owner's instruction (31 Oct 2026): every row holds just the dua or dhikr, no narration, no
instruction, no note; with its Indonesian and English translation, Latin and audio. The full
book entry stays in `arabic_full` / `translation_*_full` and the app shows it under the dua.

Rules, applied per entry in SPLITS / KINDS / FOOTNOTES below (each decision is listed, with the
reason where it is not obvious):

  1. A row is one contiguous run of words of the entry, cut out exactly (`arabic` must occur
     verbatim in the entry). Nothing is written or respelled.
  2. One row per saying. An entry with several sayings (food and milk, the sneezer and the one
     who answers, "three times ... then") becomes one row per saying, each with its own count
     and its own stretch of the recording. The first part keeps the entry's id, so routines,
     notes and memorisation stay attached; later parts get new ids.
  3. Translations are cut the same way from the entry's own translation. Where the book prints
     only a transliteration for the dua words (common in its virtue chapters), the translation
     of the same Arabic is taken from another row of the same book (`translation_*_from` names
     that row) or from HadeethEnc (`hadeethenc:<id>`); build_content.py checks both. Where no
     source has it, `translation_id_from` is "none": the row has no Indonesian and the app shows
     its English (eating_08 and eating_09: HadeethEnc has the hadith in Arabic only; tasbih_03:
     its words in another order; tasbih_06: its Indonesian leaves out al-'Azim).
  4. An entry with no words to recite (a virtue hadith, an instruction) becomes kind "hadith":
     the app shows it as a reading card, without counter, routines or memorisation.
  5. A note giving another wording for the evening goes to `footnote`, shown under the dua.
  6. A row the book files under the other time of day ("three times in the evening" in the
     morning list) is removed from that list (REMOVE); its twin in the right list stays.

`apply` refuses to run when an entry changed since these decisions (its sha1 is pinned).
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import segment_audio as sa  # noqa: E402

HADEETHENC = Path(__file__).resolve().parent.parent / "content/source/import/hadeethenc.json"
NEW_COLUMNS = ["kind", "footnote", "translation_id_from", "translation_en_from"]


@dataclass(frozen=True)
class Part:
    id: str
    arabic: str                 # verbatim run of the entry's Arabic
    tid: str | None             # verbatim run of the Indonesian (of `tid_from`, else the entry's)
    ten: str | None             # verbatim run of the English (of `ten_from`, else the entry's)
    count: str = ""
    tid_from: str = ""          # "row:<id>" or "hadeethenc:<id>" when not the entry's own
    ten_from: str = ""


# entry id -> (sha1 of its arabic_full or arabic, parts). Parts after the first are inserted
# right after the entry, in order.
SPLITS: dict[str, tuple[str, list[Part]]] = {
    "after_salah_01": ("", [
        Part("after_salah_01", "أَسْتَغْفِرُ اللَّهَ", "Aku minta ampun kepada Allah", "I ask Allah for forgiveness.", count="3"),
        Part("after_salah_09", "اللَّهُمَّ أَنْتَ السَّلاَمُ، وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالْإِكْرَامِ",
             "Ya Allah, Engkau pemberi keselamatan, dan dariMu keselamatan, Maha Suci Engkau, wahai Tuhan Yang Pemilik Keagungan dan Kemuliaan.",
             "O Allah, You are As-Salam and from You is all peace, blessed are You, O Possessor of majesty and honour."),
    ]),
    "after_salah_04": ("", [
        Part("after_salah_04", "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَاللَّهُ أَكْبَرُ",
             "Maha Suci Allah, segala puji bagi Allah. Dan Allah Maha Besar.",
             "How perfect Allah is, all praise is for Allah, and Allah is the greatest.", count="33"),
        Part("after_salah_10", "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
             "Tidak ada Tuhan (yang hak disembah) kecuali Allah Yang Maha Esa, tidak ada sekutu bagiNya. BagiNya kerajaan. BagiNya pujaan. Dia-lah Yang Mahakuasa atas segala sesuatu.",
             "None has the right to be worshipped except Allah, alone, without partner, to Him belongs all sovereignty and praise and He is over all things omnipotent."),
    ]),
    # The book counts 33, 33, 34 (the Indonesian says 33 for the last; the Arabic and English say 34).
    "sleep_08": ("", [
        Part("sleep_08", "سُبْحَانَ اللَّهِ", "Maha Suci Allah", "How Perfect Allah is.", count="33"),
        Part("sleep_14", "وَالْحَمْدُ لِلَّهِ", "Segala puji bagi Allah", "All praise is for Allah.", count="33"),
        Part("sleep_15", "وَاللَّهُ أَكْبَرُ", "Allah Maha Besar", "Allah is the greatest.", count="34"),
    ]),
    "fear_05": ("", [
        Part("fear_05", "اللَّهُ أَكْبَرُ، اللَّهُ أَعَزُّ مِنْ خَلْقِهِ جَمِيعاً، اللَّهُ أَعَزُّ مِمَّا أَخَافُ وَأَحْذَرُ، أَعُوذُ بِاللَّهِ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ، الْمُمْسِكِ السَّمَوَاتِ السَّبْعِ أَنْ يَقَعْنَ عَلَى الْأَرْضِ إِلاَّ بِإِذْنِهِ، مِنْ شَرِّ عَبْدِكَ فُلاَنٍ، وَجُنُودِهِ وَأَتْبَاعِهِ وَأَشْيَاعِهِ، مِنْ الْجِنِّ وَالإِنْسِ، اللَّهُمَّ كُنْ لِي جَاراً مِنْ شَرِّهِمْ، جَلَّ ثَنَاؤُكَ وَعَزَّ جَارُكَ، وَتَبَارَكَ اسْمُكَ، وَلاَ إِلَهَ غَيْرُكَ",
             "Allah Maha Besar. Allah Maha Perkasa dari segala makhlukNya. Allah Maha Perkasa dari apa yang aku takutkan dan khawatirkan. Aku berlindung kepada Allah, yang tiada Tuhan yang berhak disembah selain Dia, yang menahan tujuh langit agar tidak menjatuhi bumi kecuali dengan izinNya, dari kejahatan hambaMu Fulan, serta para pembatunya, pengikutnya dan pendukungnya, dari jenis jin dan manusia. Ya Allah, jadilah Engkau pelindungku dari kejahatan mereka. Agunglah pujiMu, kuatlah perlindunganMu dan Maha Suci asma-Mu. Tiada Tuhan yang berhak disembah selain Engkau.",
             "Allah is the Most Great, Mightier than all His creation. He is Mightier than what fear and dread. I seek refuge in Allah, Who there is none worthy of worship but Him. He is the One Who holds the seven heavens from falling upon the earth except by His command. [I seek refuge in You Allah] from the evil of Your slave [name of the person], and his helpers, his followers and his supporters from among the jinn and mankind. O Allah, be my support against their evil. Glorious Your praises and mighty is Your patronage. Blessed is Your Name, there is no true God but You.",
             count="3"),
    ]),
    # The three times apply to spitting; the words are said, the count is left out.
    "waswas_04": ("", [
        Part("waswas_04", "أَعُوذُ بِاللَّهِ مِنَ الشَّيطَانِ الرَّجِيمِ", "aku berlindung kepada Allah dari godaan syaitan yang terkutuk", None),
    ]),
    "news_05": ("", [
        Part("news_05", "بِسْمِ اللَّهِ", "Dengan nama Allah.", "In the name of Allah", count="3", tid_from="row:travel_05"),
        Part("news_06", "أَعُوذُ بِاللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ",
             "Aku berlindung kepada Allah dan kekuasaanNya dari kejahatan sesuatu yang aku jumpai dan yang aku takuti.",
             "I take refuge in Allah and within His omnipotence from the evil that I feel and am wary of.", count="7"),
    ]),
    "eating_01": ("", [
        Part("eating_01", "بِسْمِ اللَّهِ", "Dengan nama Allah.", None, tid_from="row:travel_05"),
        Part("eating_08", "بسمِ اللَّهِ فِي أَوَّلِهِ وَآخِرِهِ", None, "In the name of Allah in it’s beginning and end.", tid_from="none"),
    ]),
    "eating_02": ("", [
        Part("eating_02", "اللَّهُمَّ بَارِكْ لَنَا فِيهِ وَأَطْعِمْنَا خَيْراً مِنْهُ",
             "Ya Allah! berilah kami berkah dengan makan itu dan berilah makanan yang lebih baik.",
             "O Allah, bless it for us and feed us better than it."),
        Part("eating_09", "اللَّهُمَّ بَارِكْ لَنَا فِيهِ وَزِدْنَا مِنْهُ", None, "O Allah, bless it for usand give us more of it.", tid_from="none"),
    ]),
    # The third saying (yahdikumullah...) is already its own row, sneeze_02.
    "sneeze_01": ("", [
        Part("sneeze_01", "الْحَمْدُ لِلَّهِ", "Segala puji bagi Allah", "All praise if for Allah."),
        Part("sneeze_03", "يَرْحَمُكَ اللَّهُ", "Semoga Allah memberi rahmat kepadaMu", "May Allah have mercy upon you."),
    ]),
    "travel_12": ("", [
        Part("travel_12", "الْحَمْدُ لِلَّهِ الَّذِي بِنِعْمَتِهِ تَتِمُّ الصَّالِحَاتُ",
             "Segala puji bagi Allah yang dengan nikmatNya segala amal shalih sempur-na.",
             "Praise is to Allah Who by His blessings all good things are perfected."),
        Part("travel_13", "الْحَمْدُ لِلَّهِ عَلَى كُلِّ حَالٍ", "Segala puji bagi Allah, atas segala keadaan.",
             "Praise is to Allah in all circumstances."),
    ]),
    # The aside "if he knows that" stays: it sits inside the words to say.
    "manners_07": ("", [
        Part("manners_07", "أَحْسِبُ فُلاَناً وَاللَّهُ حَسِيبُهُ، وَلاَ أُزَكِّي عَلَى اللَّهِ أَحَداً، أَحْسِبُهُ – إِنْ كَانَ يَعْلَمُ ذَاكَ – كَذَا وَكَذَا",
             "Aku kira Fulan .. dan Allahlah yang mengawasi perbuatannya. Dan aku tidak akan memuji seseorang dihadapan Allah’. Apabila seseorang mengetahui hendaklah berkata: ‘Aku kira begini dan begini",
             "I deem so-and-so to be…and Allah is his reckoner…and I don’t praise anyone, putting it (i.e. my praising) forward, in front of Allah’s commendation, however I assume him so and so’…if he knows that of him."),
    ]),
    "tasbih_02": ("", [
        Part("tasbih_02", "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ، وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
             "Tidak ada Tuhan yang berhak disembah selain Allah Yang Maha Esa, tidak ada sekutu bagiNya. BagiNya kerajaan dan segala pujian. Dia-lah yang berkuasa atas segala sesuatu.",
             "None has the right to be worshipped except Allah, alone, without partner. To Him belongs all sovereignty and praise and He is over all things omnipotent.",
             count="10", tid_from="row:morning_13"),
    ]),
    "tasbih_03": ("", [
        Part("tasbih_03", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحانَ اللَّهِ الْعَظِيمِ", None,
             "How perfect Allah is and I praise Him. How perfect Allah is, The Supreme.", tid_from="none"),
    ]),
    "tasbih_04": ("", [
        Part("tasbih_04", "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلاَ إِلَهَ إِلاَّ اللَّهُ، وَاللَّهُ أَكْبَرُ",
             "Mahasuci Allah, segala puji bagi Allah, tidak ada sembahan yang hak selain Allah, dan Allah Mahabesar",
             "How perfect Allah is, and all praise is for Allah. None has the right to be worshipped except Allah, and Allah is the greatest.",
             tid_from="hadeethenc:6211"),
    ]),
    "tasbih_06": ("", [
        Part("tasbih_06", "سُبْحَانَ اللَّهِ الْعَظِيمِ وَبِحَمْدِهِ", None, "How perfect Allah is . The Supreme, and I praise Him.", tid_from="none"),
    ]),
    "tasbih_07": ("", [
        Part("tasbih_07", "لاَ حَوْلَ وَلاَ قُوَّةَ إِلاَّ بِاللَّهِ", "tidak ada daya serta kekuatan kecuali dari Allah",
             "There is no might nor power except with Allah.", tid_from="hadeethenc:6273"),
    ]),
    "istighfar_03": ("", [
        Part("istighfar_03", "أَسْتَغْفِرُ اللَّهَ الْعَظيمَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَيُّ القَيّوُمُ وَأَتُوبُ إِلَيهِ",
             "Aku minta ampun kepada Allah, tiada Tuhan yang berhak disembah kecuali Dia, Yang Hidup dan terus-menerus mengurus makhlukNya.",
             "I seek Allah’s forgiveness, besides whom, none has the right to be worshipped except He, The Ever Living, The Self-Subsisting and Supporter of all, and I turn to Him in repentance."),
    ]),
    "morning_03": ("", [
        Part("morning_03", "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيرَ مَا بَعْدَهُ ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ", "", ""),
    ]),
    "evening_03": ("", [
        Part("evening_03", "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيرَ مَا بَعْدَهُ ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ", "", ""),
    ]),
    "evening_11": ("", [
        Part("evening_11", "أَصْبَحْنا عَلَى فِطْرَةِ الْإِسْلاَمِ، وَعَلَى كَلِمَةِ الْإِخْلاَصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ صلى الله عليه وسلم، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ، حَنِيفاً مُسْلِماً وَمَا كَانَ مِنَ الْمُشرِكِينَ", "",
             "We rise upon the fitrah of Islam, and the word of pure faith, and upon the religion of our Prophet Muhammad and the religion of our forefather Ibraheem, who was a Muslim and of true faith and was not of those who associate others with Allah."),
    ]),
    "evening_18": ("", [
        Part("evening_18", "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", "", "", count="3"),
    ]),
    # Owner (listening review): one part per surah, and the closing "after every prayer" goes,
    # from the text and the audio. Its English only names the surahs; morning_02's English is
    # the three surahs' translation. A mark goes where al-Falaq's "﴾،" runs into the next
    # basmalah with no space: the build puts the space in; no word changes.
    "after_salah_05": ("", [
        Part("after_salah_05", "بسم الله الرحمن الرحيم ﴿قُلْ هُوَ اللَّهُ أَحَدٌ … مِنَ الْجِنَّةِ وَ النَّاسِ﴾",
             "Dengan menyebut nama Allah Yang Maha Pemurah lagi Maha Penyayang 1. Katakanlah … 6. dari (golongan) jin dan manusia.",
             "", ten_from="row:morning_02"),
    ]),
    # The three surahs said before sleep; the cupping and wiping are the instruction around them.
    "sleep_01": ("", [
        Part("sleep_01", "بسم الله الرحمن الرحيم ﴿قُلْ هُوَ اللَّهُ أَحَدٌ … مِنَ الْجِنَّةِ وَ النَّاسِ﴾",
             "Dengan menyebut nama Allah Yang Maha Pemurah lagi Maha Penyayang 1. Katakanlah … 6. dari (golongan) jin dan manusia.",
             "", count="3", ten_from="row:morning_02"),
    ]),
    "istiftah_05": ("", [
        Part("istiftah_05", "", "Ya, Allah! BagiMu segala puji … tidak ada Tuhan yang hak disembah kecuali Engkau", ""),
    ]),
    "sleep_02": ("", [
        Part("sleep_02", "", "Allah, tidak ada Tuhan (yang berhak disembah) … dan Allah Maha Tinggi lagi Maha Besar.", ""),
    ]),
    "waswas_03": ("", [
        Part("waswas_03", "هُوَ الْأوَّلُ وَالْآخِرُ وَالظّاهِرُ وَالْباطِنُ وَهُوَ بِكُلِّ شَيْءٍ عَلِيمٌ",
             "Dia-lah yang Awal … Dia-lah Yang Maha Mengetahui atas segala sesuatu.",
             "He is The First and The Last, Aththahir and Al-Batin and He knows well all things."),
    ]),
    "children_02": ("", [
        Part("children_02", "أُعِيذُكُمَا بِكَلِمَاتِ اللَّهِ التَّامَّةِ … وَمِنْ كُلِّ عَيْنٍ لاَمَّةٍ",
             "Aku berlindung kepada Allah untukmu berdua … pandangan mata yang jahat.",
             "I commend you two to the protection of Allah’s perfect words from every devil, vermin, and every evil eye."),
    ]),
    "illness_04": ("", [
        Part("illness_04", "لاَ إِلَهَ إِلاَّ اللَّهُ إِنَّ لِلْمَوْتِ سَكَرَاتٍ",
             "Tiada Tuhan yang berhak disembah selain Allah, sesungguhnya mati itu mempunyai sekarat.",
             "None has the right to be worshipped except Allaah, death does indeed contain agony."),
    ]),
    "manners_01": ("", [Part("manners_01", "", "", "May Allah reward you with goodness.")]),
    # Its English is Aisha's reply about others ("them"), not this wording: left out.
    "manners_05": ("", [Part("manners_05", "", "", None)]),
    "salam_04": ("", [Part("salam_04", "وَعَلَيْكُمْ", None, "And upon you.", tid_from="none")]),
    "travel_04": ("", [
        Part("travel_04", "", "Tiada Tuhan yang berhak disembah selain Allah Yang Maha Esa … mengalahkan golongan musuh dengan sendirian",
             "None has the right to be worshipped except Allah, alone, without partner. … single-handedly defeated the allies."),
    ]),
    "hajj_03": ("", [Part("hajj_03", "", "", "O our Lord, grant us the best in this life … protect us from the punishment of the Fire.")]),
    # At Safa: the verse and "I begin with what Allah began with"; then the tahlil, three times.
    "hajj_04": ("", [
        Part("hajj_04", "لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، … وَهَزَمَ الْأَحْزَابَ وَحْدَهُ",
             "Tiada Tuhan yang berhak disembah selain Allah, Yang Maha Esa, … mengalahkan golongan musuh sendirian.",
             "None has the right to be worshipped except Allah, alone, without partner. … single-handedly defeated the allies.",
             count="3"),
        Part("hajj_10", "﴿إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَآئِرِ اللَّهِ﴾ أَبْدَأُ بِمَا بَدَأَ اللَّهُ بِهِ",
             "Sesungguhnya Shafa dan Marwah ada-lah termasuk sy’iar agama Allah. Aku memulai sa’i dengan apa yang didahulukan oleh Allah.",
             "Indeed Safa and Marwah are from the places of worship of Allah…) (I begin with what Allah began with."),
    ]),
}

# Phrase starts for rows split by hand (one phrase per surah).
PHRASES = {rid: ["بسم الله الرحمن الرحيم ﴿قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "بسم الله الرحمن الرحيم ﴿قُلْ أَعُوذُ بِرَبِّ النَّاسِ"]
           for rid in ("after_salah_05", "sleep_01")}

# Entries with nothing to recite: shown as reading cards (rule 4).
KINDS = {rid: "hadith" for rid in [
    "adhan_01",  # the owner marked it "virtue" in the listening review
    "shalawat_01", "shalawat_02", "shalawat_03", "shalawat_04", "shalawat_05",
    "salam_01", "salam_03",
    "istighfar_01", "istighfar_02", "istighfar_04", "istighfar_05", "istighfar_06",
    "illness_06", "travel_09", "tasbih_05", "night_03",
    "tasbih_08",  # its four words are tasbih_04's, in the same chapter
    # instructions and practices, with no words given to recite:
    "adhan_03", "sleep_12", "fear_09", "waswas_07", "waswas_08", "news_04", "funeral_01", "fasting_01",
    "protection_01", "salam_02", "animals_01", "animals_02", "hajj_02", "hajj_06", "hajj_07",
]}

# Evening wording given by the book as a note (rule 5): verbatim runs of the entry.
FOOTNOTES = {
    "evening_03": "[ وإذا أمسى قال: أمسينا وأمسى الملك للَّه] [وإذا أمسى قال: رب أسألك خير ما في هذه الليلة، وخير ما بعدها، وأعوذ بك من شر ما في هذه الليلة، وشر ما بعدها.]",
    "evening_11": "[وإذا أمسى قال: أمسينا على فطرة الإسلام...]",
}

# Source errors (owner's list, 31 Oct): a translation that does not translate its Arabic.
#   after_salah_06: the English only says "recite Ayat al-Kursi"; morning_01's English is that
#   verse's translation, so it is taken from there (rule 3).
TAKE = {"after_salah_06": {"en": "row:morning_01"}}
#   tasbih_02: the entry's English translates another hadith (the 100x one); the dua row's English
#   is only the dhikr, the same in both, and the wrong full English is left out.
DROP_FULL = {"tasbih_02": "en"}

# Rule 7: a repetition or time note inside a translation ("(Dibaca tiga kali)", "(three times)",
# "( [ten times or once if lazy])") is the book's instruction, not the dua; it is cut, and the
# whole translation stays in translation_*_full. Only short parentheses made of such words count.
NOTE = re.compile(
    r"\s*\((?=[^()]{0,70}\))\s*\[?\s*(?:[Dd]ibaca|[Nn]ote:|(?:one|ten|three|seven|a)?\s*(?:hundred\s+)?"
    r"(?:times?|once|twice|thrice)\b|(?:ten|three|seven|one hundred|thirty-three|thirty-four)\s+times)"
    r"[^()]*\)")


TRAILING_NOTE = re.compile(r"(?<=[”\"])\s*[Dd]ibaca\b[^”\"]*$")  # an unbracketed note after the closing quote
ETC = re.compile(r"\s*\(dst\.\)")


def cut_notes(text: str) -> str:
    out = TRAILING_NOTE.sub("", ETC.sub("", NOTE.sub("", text)))
    out = re.sub(r"\.(\s*\.)+", ".", out)  # the note's own full stop, left behind
    return re.sub(r"\s{2,}", " ", out).strip()


# Said at the other time of day, by the book's own note (rule 6).
REMOVE = {
    "morning_18": "three times in the evening; evening_18 is the same entry",
    "evening_16": "said in the morning (إذا أصبح); morning_16 is the same entry",
}


def verbatim_run(part: str, full: str) -> str | None:
    """The run of `full` that reads as `part`, returned as written in `full`.

    Matching ignores the order of stacked marks (the source has shadda before fatha; text typed
    or copied elsewhere often has the canonical order), and lets the first and last word of
    `part` be the end or start of a word of `full` (so "((" or "))." around it stay outside).
    The characters returned are always the source's own."""
    nfc = lambda t: unicodedata.normalize("NFC", t)
    pw = part.split()
    spans = [(m.start(), m.end()) for m in re.finditer(r"\S+", full)]
    fw = [nfc(full[a:b]) for a, b in spans]
    pw = [nfc(w) for w in pw]
    n = len(pw)
    for i in range(len(fw) - n + 1):
        win = fw[i:i + n]
        if n == 1:
            ok = pw[0] in win[0]
        else:
            ok = win[0].endswith(pw[0]) and win[-1].startswith(pw[-1]) and win[1:-1] == pw[1:-1]
        if not ok:
            continue
        a, b = spans[i][0], spans[i + n - 1][1]
        if n == 1:
            k = win[0].find(pw[0])
            a, b = spans[i][0] + k, spans[i][0] + k + len(pw[0])
        else:
            a += len(win[0]) - len(pw[0])
            b -= len(win[-1]) - len(pw[-1])
        run = full[a:b]
        if nfc(run) == nfc(part):
            return run
    return None


def run_of(spec: str, full: str) -> str | None:
    """`spec` is a verbatim run, or "<start> … <end>": from the start anchor to the end of the end
    anchor after it (both verbatim), for long passages."""
    if " … " not in spec:
        return verbatim_run(spec, full)
    first, last = spec.split(" … ", 1)
    a = verbatim_run(first, full)
    if a is None:
        return None
    i = full.find(a)
    z = verbatim_run(last, full[i + len(a):])
    if z is None:
        return None
    j = full.find(z, i + len(a))
    return full[i:j + len(z)]


def sha1(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def apply(duas: Path = sa.DUAS) -> tuple[list[str], list[str]]:
    fields, rows, newline = sa.read_csv(duas)
    for c in ["arabic_full", "translation_id_full", "translation_en_full", "audio_range", *NEW_COLUMNS]:
        if c not in fields:
            fields.append(c)
    for r in rows:
        for c in fields:
            r.setdefault(c, "")
    by_id = {r["id"]: r for r in rows}
    problems, changed = [], []
    out: list[dict] = []
    for row in rows:
        rid = row["id"]
        if rid in REMOVE:
            changed.append(f"-{rid}")
            continue
        if rid in KINDS and row.get("kind") != KINDS[rid]:
            row["kind"] = KINDS[rid]
            changed.append(rid)
        for lang, src in TAKE.get(rid, {}).items():
            other = by_id.get(src[4:])
            if other and row.get(f"translation_{lang}_from") != src:
                row[f"translation_{lang}_full"] = ""
                row[f"translation_{lang}"] = other[f"translation_{lang}"]
                row[f"translation_{lang}_from"] = src
                changed.append(rid)
        if rid not in SPLITS:
            out.append(row)
            continue
        pinned, parts = SPLITS[rid]
        full_ar = row.get("arabic_full") or bc.split_phrases(row["arabic"])[0]
        if pinned and sha1(full_ar) != pinned:
            problems.append(f"{rid}: the entry changed since these decisions")
            out.append(row)
            continue
        full_id = row.get("translation_id_full") or row["translation_id"]
        full_en = row.get("translation_en_full") or row["translation_en"]
        for k, part in enumerate(parts):
            new = dict(row) if k == 0 else {**row, "id": part.id}
            if part.arabic:  # "" keeps the Arabic, its audio and count: only translations are cut
                arabic = run_of(part.arabic, full_ar)
                if arabic is None:
                    problems.append(f"{part.id}: Arabic is not a verbatim run of {rid}'s entry")
                    continue
                new["arabic"] = arabic
                new["arabic_full"] = full_ar
                new["audio_segments"] = ""
                new["audio_range"] = ""
                new["count"] = part.count
                new["transliteration"] = ""
            for lang, cut, src, full in (("id", part.tid, part.tid_from, full_id), ("en", part.ten, part.ten_from, full_en)):
                base = full
                if src.startswith("row:"):
                    other = by_id.get(src[4:])
                    base = other[f"translation_{lang}"] if other else ""
                elif src.startswith("hadeethenc:"):
                    snap = json.loads(HADEETHENC.read_text(encoding="utf-8"))["hadeeths"]
                    base = snap.get(src[11:], {}).get(lang, {}).get("hadeeth", "")
                if cut is None:
                    new[f"translation_{lang}"] = ""
                elif cut == "" and src:
                    new[f"translation_{lang}"] = base  # that row's whole translation
                elif cut == "":
                    new[f"translation_{lang}"] = row[f"translation_{lang}"]  # unchanged
                    src = ""
                elif run_of(cut, base) is None:
                    problems.append(f"{part.id}: translation_{lang} is not a verbatim run of {src or rid}")
                    continue
                else:
                    new[f"translation_{lang}"] = run_of(cut, base)
                new[f"translation_{lang}_full"] = full if new[f"translation_{lang}"].strip() != full.strip() else ""
                new[f"translation_{lang}_from"] = src
            if rid in FOOTNOTES and k == 0:
                note = verbatim_run(FOOTNOTES[rid], full_ar)
                if note is None:
                    problems.append(f"{rid}: footnote is not a verbatim run of the entry")
                new["footnote"] = note or ""
            for before in PHRASES.get(part.id, []):
                at = run_of(before, new["arabic"])
                if at is None:
                    problems.append(f"{part.id}: phrase start '{before[:20]}' not found")
                    continue
                k2 = new["arabic"].find(at)
                new["arabic"] = new["arabic"][:k2].rstrip() + " | " + new["arabic"][k2:]
            if DROP_FULL.get(rid):
                new[f"translation_{DROP_FULL[rid]}_full"] = ""
            if new.get("arabic_full", "").strip() == bc.split_phrases(new["arabic"])[0]:
                new["arabic_full"] = ""
            out.append(new)
            changed.append(part.id)
    # orders: keep each event's sequence, numbering 1..n
    for r in out:
        for lang in ("id", "en"):
            text = r.get(f"translation_{lang}", "")
            cut = cut_notes(text)
            if cut and cut != text.strip():
                if not r.get(f"translation_{lang}_full"):
                    r[f"translation_{lang}_full"] = text
                r[f"translation_{lang}"] = cut
                changed.append(f"{r['id']}~{lang}")
    touched = {by_id[c.lstrip("-")]["event"] for c in changed if c.lstrip("-") in by_id}  # (~lang entries are skipped)
    touched |= {by_id[s]["event"] for s in SPLITS}
    seen: dict[str, int] = {}
    for r in out:
        if r["event"] in touched:
            seen[r["event"]] = seen.get(r["event"], 0) + 1
            r["order"] = str(seen[r["event"]])
    if problems:
        return [], problems
    sa.write_csv(duas, fields, out, newline)
    return changed, []


def main() -> int:
    changed, problems = apply()
    print(f"changed {len(changed)}: {' '.join(changed) or '-'}")
    for p in problems:
        print("problem:", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
