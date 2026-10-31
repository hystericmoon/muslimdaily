#!/usr/bin/env python3
"""Adds duas from the Sunnah that Hisnul Muslim does not carry (category `sunnah`), taken from
HadeethEnc.com (Encyclopedia of Translated Prophetic Hadiths).

    python3 -I tools/hadeethenc/import_hadeethenc.py --fetch   # download the snapshot
    python3 -I tools/hadeethenc/import_hadeethenc.py           # write the rows to duas.csv

HadeethEnc gives every hadith in Arabic with its grading and takhrij, and translations by its
publisher. Its terms allow republishing with "no modification, addition, or deletion of the
content", the publisher and source credited. So each row keeps the whole hadith verbatim in
`arabic_full`, `translation_id_full` and `translation_en_full` (shown under "full text" in the
app), and `arabic` / `translation_id` / `translation_en` hold only the dua: a span cut from that
text, never a word written or changed. Spans are located by the quotation marks (Arabic) and by
the first and last words of the dua (translations); a span that is not found stops the import.

The snapshot (`content/source/import/hadeethenc.json`) is the source of truth for the build, so
the import is repeatable offline. Rows are written as `sunnah_NN`; existing sunnah_ rows are
replaced, everything else in the CSV is left as it is.

Which hadiths are imported, and why the others are not, is decided in SELECTED and EXCLUDED
below. Every selected hadith was checked: graded sahih or hasan by HadeethEnc, not already in
the app (Hisnul Muslim or the Qur'anic duas), a dua the reader says (not an instruction about
one), and its Indonesian and English translating the same Arabic. A hadith failing any check is
left out, not corrected.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import re
import sys
import time
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
SNAPSHOT = ROOT / "content/source/import/hadeethenc.json"
API = "https://hadeethenc.com/api/v1/hadeeths/one/?language={lang}&id={hid}"
USER_AGENT = "adwam-content-import/1.0 (+https://github.com/hystericmoon/muslimdaily)"
GRADES = {"صحيح": "sahih", "صحيح لغيره": "sahih", "حسن": "hasan", "حسن لغيره": "hasan"}
GRADER = "HadeethEnc"
PHRASE_MARK = "|"

# hadeethenc id -> event, Arabic quote to take (0 = first «...») with a leading word to leave out
# and, when the quotation runs on past the dua, the text that follows the dua (or ""),
# (first, last) words of the dua in the Indonesian and in the English, and the sources the
# takhrij names (book, number), at most three.
SELECTED = [
    # Steadfastness and guidance
    (3142, "sunnah_heart", 0, ("", ""), ("Wahai Tuhan yang membolak", "agama-Mu"),
     ("O Turner of the hearts", "Your religion."), [("At-Tirmidzi", "2140"), ("Ahmad", "12107")]),
    (6331, "sunnah_heart", 1, ("", ""), ("Ya Allah, Tuhan yang membolak", "kepada-Mu"),
     ("O Allah, the Turner", "Your obedience"), [("Muslim", "2654")]),
    (3596, "sunnah_heart", 0, ("", ""), ("Ya Allah! Sesungguhnya", "kecukupan."),
     ("O Allah, I ask You for guidance", "self-sufficiency"), [("Muslim", "2721")]),
    (5915, "sunnah_heart", 0, ("قُلِ", "، وَاذْكُرْ"), ("Ya Allah! Tunjukilah", "bertindak."),
     ("O Allah, guide me", "straight path"), [("Muslim", "2725")]),
    (5482, "sunnah_heart", 0, ("", ""), ("Ya Allah! Perbaikilah", "bagiku."),
     ("O Allah, set right", "from every evil."), [("Muslim", "2720")]),
    (10410, "sunnah_heart", 0, ("", ""), ("Wahai Rabb-ku! Berilah", "dari hatiku."),
     ("My Lord, support me", "from my heart."), [("Abu Dawud", ""), ("At-Tirmidzi", ""), ("Ibnu Majah", "")]),
    # Forgiveness and all good
    (5483, "sunnah_goodness", 0, ("", ""), ("Ya Tuhanku! Ampuni", "segala sesuatu"),
     ("O Lord, forgive me", "power over everything."), [("Al-Bukhari", "6398"), ("Muslim", "2719")]),
    (5487, "sunnah_goodness", 0, ("", ""), ("Ya Allah! Sesungguhnya aku memohon kepada-Mu seluruh", "adalah kebaikan."),
     ("O Allah, I ask You for all", "decreed for me good."), [("Ibnu Majah", "3846"), ("Ahmad", "25019")]),
    (3056, "sunnah_goodness", 0, ("", ""), ("Ya Allah! Hanya", "pasti akan mati."),
     ("O Allah, I surrender", "humans die"), [("Muslim", "2717"), ("Al-Bukhari", "7383")]),
    # Seeking refuge
    (5488, "sunnah_refuge", 0, ("", ""), ("Ya Allah! Sesungguhnya aku berlindung", "semua murka-Mu."),
     ("O Allah, I seek refuge", "all of Your wrath."), [("Muslim", "2739")]),
    (5489, "sunnah_refuge", 0, ("", ""), ("Ya Allah! Sesungguhnya", "kegembiraan musuh."),
     ("O Allah, I seek refuge", "gloat of the enemies."), [("An-Nasai", "5475"), ("Ahmad", "6618")]),
    (5878, "sunnah_refuge", 0, ("", ""), ("Ya Allah, sesungguhnya aku berlindung kepada-Mu dari sifat lemah", "tidak terkabul."),
     ("O Allah, I seek refuge with You from inability", "is not answered"), [("Muslim", "2722")]),
    (5329, "sunnah_refuge", 0, ("", ""), ("Ya Allah, aku berlindung", "hawa nafsu."),
     ("O Allah, I seek refuge with You from evil morals", "desires"), [("At-Tirmidzi", "3591")]),
    (6047, "sunnah_refuge", 0, ("", ""), ("Ya Allah! Aku berlindung", "jelek lainnya."),
     ("O Allah, I seek refuge", "evil diseases."), [("Abu Dawud", ""), ("An-Nasai", ""), ("Ahmad", "")]),
    (6072, "sunnah_refuge", 0, ("", ""), ("Ya Allah aku berlindung", "kefakiran."),
     ("O Allah, verily", "affluence and poverty."), [("Abu Dawud", ""), ("At-Tirmidzi", ""), ("Ibnu Majah", "")]),
    (5883, "sunnah_refuge", 0, ("", ""), ("Ya Allah! Aku berlindung", "kawan dekat."),
     ("O Allah, I seek refuge", "evil hidden trait."), [("Abu Dawud", ""), ("An-Nasai", ""), ("Ibnu Majah", "")]),
]

# Checked and left out. Kept in the snapshot so the decision can be re-read.
EXCLUDED = {
    5491: "The Indonesian translation is of another hadith (al-huda wat-tuqa), not this Arabic.",
    6163: "The Indonesian translation leaves out 'Allahumma' (Ya Allah) from the dua.",
    2932: "An instruction to ask for 'afiyah, not the wording of a dua; morning_07 carries the dua.",
    3598: "The same hadith as 3056 (Muslim's wording is imported).",
    5485: "Already in the app from Hisnul Muslim (morning_07 / evening_07).",
    5884: "Already in the app from Hisnul Muslim (debt_01).",
    11300: "Already in the app from Hisnul Muslim (to_masjid_01).",
    5914: "Close to debt_02 from Hisnul Muslim; 5878 is imported for the refuge from weakness and laziness.",
}

LANG_FIELDS = {
    "ar": ("title", "hadeeth", "attribution", "grade", "reference"),
    "id": ("title", "hadeeth", "attribution", "grade"),
    "en": ("title", "hadeeth", "attribution", "grade"),
}


def fetch_one(lang: str, hid: int) -> dict:
    req = urllib.request.Request(API.format(lang=lang, hid=hid), headers={"User-Agent": USER_AGENT})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                data = json.loads(r.read().decode("utf-8"))
            return {k: data.get(k) or "" for k in LANG_FIELDS[lang]}
        except OSError:
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)
    raise AssertionError("unreachable")


# Kept in the snapshot only as the source of a translation for a Hisnul Muslim row whose book
# prints the dua in transliteration (tools/clean_entries.py, `translation_id_from`); not imported.
TRANSLATION_ONLY = {
    6211: "Indonesian of subhanallah, walhamdulillah, wa la ilaha illallah, wallahu akbar (tasbih_04)",
    6273: "Indonesian of la hawla wa la quwwata illa billah (tasbih_07)",
}


def fetch() -> None:
    ids = sorted({s[0] for s in SELECTED} | set(EXCLUDED) | set(TRANSLATION_ONLY))
    hadeeths = {str(hid): {lang: fetch_one(lang, hid) for lang in LANG_FIELDS} for hid in ids}
    snapshot = {
        "source": "HadeethEnc.com (Encyclopedia of Translated Prophetic Hadiths), https://hadeethenc.com",
        "terms": "Republished without modification, addition or deletion, with the publisher and source credited.",
        "fetched": date.today().isoformat(),
        "hadeeths": hadeeths,
    }
    SNAPSHOT.write_text(json.dumps(snapshot, ensure_ascii=False, indent=1) + "\n", "utf-8")
    print(f"{len(ids)} hadiths saved to {SNAPSHOT.relative_to(ROOT)}")


def quote(text: str, index: int) -> str:
    quotes = re.findall(r"«([^»]+)»", text)
    if index >= len(quotes):
        raise ValueError(f"no quotation {index + 1} in the Arabic")
    return quotes[index].strip()


def span(text: str, first: str, last: str) -> str:
    """The text from [first] to the end of the next [last], exactly as it stands."""
    start = text.find(first)
    end = text.find(last, start + len(first)) if start >= 0 else -1
    if start < 0 or end < 0:
        raise ValueError(f"'{first} … {last}' not found")
    return text[start:end + len(last)].strip()


def mark_phrases(arabic: str) -> str:
    """Marks a phrase after each Arabic comma or semicolon; the words stay as they are."""
    return re.sub(r"([،؛])\s+(?=\S)", rf"\1 {PHRASE_MARK} ", arabic)


def rows(snapshot: dict, fields: list[str]) -> list[dict]:
    out, order = [], {}
    for i, (hid, event, qi, (drop, after), tid, ten, sources) in enumerate(SELECTED, start=1):
        h = snapshot["hadeeths"][str(hid)]
        ar, idn, en = h["ar"], h["id"], h["en"]
        grade = GRADES.get(ar["grade"].strip())
        if not grade:
            raise SystemExit(f"{hid}: graded '{ar['grade']}', only sahih or hasan may ship")
        try:
            dua = quote(ar["hadeeth"], qi)
            if drop:
                if not dua.startswith(drop):
                    raise ValueError(f"the quotation does not start with '{drop}'")
                dua = dua[len(drop):].lstrip(" :")
            if after:
                if after not in dua:
                    raise ValueError(f"'{after}' not in the quotation")
                dua = dua[:dua.index(after)].rstrip()
            dua_id = span(idn["hadeeth"], *tid)
            dua_en = span(en["hadeeth"], *ten)
        except ValueError as e:
            raise SystemExit(f"{hid}: {e}")
        order[event] = order.get(event, 0) + 1
        r = {f: "" for f in fields}
        r.update({
            "id": f"sunnah_{i:02d}",
            "category": "sunnah",
            "event": event,
            "order": str(order[event]),
            "arabic": mark_phrases(dua),
            "arabic_full": ar["hadeeth"].strip(),
            "is_quran": "tidak",
            "translation_id": dua_id,
            "translation_id_full": idn["hadeeth"].strip(),
            "translation_en": dua_en,
            "translation_en_full": en["hadeeth"].strip(),
            "grade": grade,
            "grader": GRADER,
            "reference": (f"{idn['attribution'].strip()}. Derajat: {idn['grade'].strip()} (HadeethEnc). "
                          f"Teks dan terjemah: HadeethEnc.com, hadis no. {hid}, diambil {snapshot['fetched']}, "
                          "dimuat tanpa diubah."),
            "notes": f"HadeethEnc {hid}. Teks utuh di kolom *_full (syarat lisensi: tanpa diubah, ditambah, atau dikurangi).",
        })
        for n, (book, no) in enumerate(sources[:3], start=1):
            r[f"source_{n}_book"], r[f"source_{n}_no"] = book, no
        out.append(r)
    return out


def write(snapshot: dict) -> None:
    raw = CSV_PATH.read_text("utf-8")
    fields = list(csv.DictReader(io.StringIO(raw)).fieldnames)
    if "translation_en_full" not in fields:
        fields.insert(fields.index("translation_id_full") + 1, "translation_en_full")
    kept = [r for r in csv.DictReader(io.StringIO(raw)) if not r["id"].startswith("sunnah_")]
    new = rows(snapshot, fields)
    out = io.StringIO()
    w = csv.DictWriter(out, fieldnames=fields, lineterminator="\r\n" if "\r\n" in raw else "\n")
    w.writeheader()
    w.writerows([{f: r.get(f, "") for f in fields} for r in kept + new])
    CSV_PATH.write_text(out.getvalue(), "utf-8")
    print(f"{len(new)} duas from the Sunnah written; {len(kept) + len(new)} rows in total")


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--fetch", action="store_true", help="download the snapshot from HadeethEnc.com")
    args = ap.parse_args(argv)
    if args.fetch:
        fetch()
        return 0
    write(json.loads(SNAPSHOT.read_text("utf-8")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
