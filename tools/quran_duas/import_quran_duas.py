"""Adds supplications from the Qur'an to content/source/duas.csv (category `quran`).

    python3 -I tools/quran_duas/import_quran_duas.py

Each entry is one or more *whole* ayat, never trimmed, because both sources allow verbatim
reuse only:
  * Arabic: Tanzil Quran Text (Uthmani), https://tanzil.net, "copy and distribute verbatim
    copies ... changing it is not allowed", source credited with a link.
  * Translations: QuranEnc.com, "no modification, addition, or deletion", publisher, source and
    version credited. Indonesian: Kemenag RI (indonesian_affairs); English: Saheeh International
    (english_saheeh). Footnotes are kept, after the translation.
Quran text needs no grading. Rows are written as `quran_NN`; existing quran_ rows are replaced,
everything else in the CSV is left as it is. Downloads are cached in build/quran/.
"""
import csv
import io
import json
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
CACHE = ROOT / "build/quran"
TANZIL = ("https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&agree=true"
          "&marks=true&sajdah=true&rub=false&tatweel=true")
QURANENC = "https://quranenc.com/api/v1/translation/aya/{key}/{sura}/{aya}"
ID_KEY, ID_VERSION, ID_NAME = "indonesian_affairs", "1.0.1", "Kementerian Agama RI"
EN_KEY, EN_NAME = "english_saheeh", "Saheeh International"

SURAH = {
    2: "Al-Baqarah", 3: "Ali 'Imran", 7: "Al-A'raf", 14: "Ibrahim", 17: "Al-Isra'", 18: "Al-Kahf",
    20: "Taha", 21: "Al-Anbiya'", 23: "Al-Mu'minun", 25: "Al-Furqan", 26: "Asy-Syu'ara'",
    27: "An-Naml", 28: "Al-Qasas", 37: "As-Saffat", 46: "Al-Ahqaf", 59: "Al-Hasyr",
    60: "Al-Mumtahanah", 66: "At-Tahrim", 71: "Nuh",
}

# (event, sura, first ayah, last ayah), in display order within each event.
DUAS = [
    ("quran_forgiveness", 7, 23, 23),     # Adam and Hawa
    ("quran_forgiveness", 3, 16, 16),
    ("quran_forgiveness", 3, 147, 147),
    ("quran_forgiveness", 23, 118, 118),
    ("quran_forgiveness", 28, 16, 16),    # Musa
    ("quran_forgiveness", 59, 10, 10),
    ("quran_forgiveness", 66, 8, 8),
    ("quran_forgiveness", 71, 28, 28),    # Nuh
    ("quran_goodness", 2, 201, 201),
    ("quran_goodness", 2, 127, 128),      # Ibrahim and Isma'il
    ("quran_goodness", 2, 250, 250),
    ("quran_goodness", 3, 8, 8),
    ("quran_goodness", 25, 65, 66),
    ("quran_goodness", 26, 83, 85),       # Ibrahim
    ("quran_goodness", 60, 5, 5),
    ("quran_ease", 20, 25, 28),           # Musa
    ("quran_ease", 20, 114, 114),
    ("quran_ease", 17, 80, 80),
    ("quran_ease", 18, 10, 10),           # the youths of the cave
    ("quran_ease", 28, 24, 24),           # Musa
    ("quran_ease", 23, 29, 29),           # Nuh
    ("quran_ease", 21, 83, 83),           # Ayyub
    ("quran_ease", 23, 97, 98),
    ("quran_family", 17, 24, 24),
    ("quran_family", 14, 40, 41),         # Ibrahim
    ("quran_family", 25, 74, 74),
    ("quran_family", 3, 38, 38),          # Zakariya
    ("quran_family", 21, 89, 89),         # Zakariya
    ("quran_family", 37, 100, 100),       # Ibrahim
    ("quran_family", 27, 19, 19),         # Sulaiman
    ("quran_family", 46, 15, 15),
]


def get(url: str, cache: Path) -> str:
    if cache.exists():
        return cache.read_text("utf-8")
    req = urllib.request.Request(url, headers={"User-Agent": "curl/8.5.0"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                text = r.read().decode("utf-8")
            break
        except OSError:
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)
    cache.parent.mkdir(parents=True, exist_ok=True)
    cache.write_text(text, "utf-8")
    return text


def tanzil() -> dict[tuple[int, int], str]:
    verses = {}
    for line in get(TANZIL, CACHE / "tanzil-uthmani.txt").splitlines():
        parts = line.split("|", 2)
        if len(parts) == 3 and parts[0].isdigit():
            verses[(int(parts[0]), int(parts[1]))] = parts[2].strip()
    if len(verses) != 6236:
        raise SystemExit(f"Tanzil text has {len(verses)} ayat, expected 6236")
    return verses


def translation(key: str, sura: int, ayat: range) -> str:
    """The ayat's translations, numbered when there are several, then any footnotes."""
    texts, notes = [], []
    for a in ayat:
        data = json.loads(get(QURANENC.format(key=key, sura=sura, aya=a), CACHE / key / f"{sura}_{a}.json"))["result"]
        text = data["translation"].strip()
        texts.append(f"({a}) {text}" if len(ayat) > 1 else text)
        if (data.get("footnotes") or "").strip():
            notes.append(data["footnotes"].strip())
    return "\n".join(texts + notes)


def arabic_digits(n: int) -> str:
    return str(n).translate(str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩"))


def main():
    verses = tanzil()
    raw = CSV_PATH.read_text("utf-8")
    rows = [r for r in csv.DictReader(io.StringIO(raw)) if not r["id"].startswith("quran_")]
    fields = list(csv.DictReader(io.StringIO(raw)).fieldnames)
    order = {}
    for i, (event, sura, first, last) in enumerate(DUAS, start=1):
        ayat = range(first, last + 1)
        # Each ayah followed by its number in ornate brackets, as Indonesian dua books print it.
        arabic = " ".join(f"{verses[(sura, a)]} ﴿{arabic_digits(a)}﴾" for a in ayat)
        span = f"{first}" if first == last else f"{first}–{last}"
        tid = translation(ID_KEY, sura, ayat)
        ten = translation(EN_KEY, sura, ayat)
        order[event] = order.get(event, 0) + 1
        r = {f: "" for f in fields}
        r.update({
            "id": f"quran_{i:02d}",
            "category": "quran",
            "event": event,
            "order": str(order[event]),
            "arabic": arabic,
            "is_quran": "ya",
            "translation_id": tid,
            "translation_en": ten,
            "source_1_book": "QS.",
            "source_1_no": f"{SURAH[sura]} [{sura}]: {span}",
            "reference": (f"Teks Arab: Tanzil Quran Text (Uthmani), tanzil.net. Terjemah: {ID_NAME} "
                          f"via QuranEnc.com ({ID_KEY} v{ID_VERSION}); English: {EN_NAME} via QuranEnc.com."),
            "notes": f"QS {sura}:{span}. Ayat utuh, tanpa dipotong (syarat lisensi Tanzil dan QuranEnc).",
        })
        rows.append(r)
    out = io.StringIO()
    w = csv.DictWriter(out, fieldnames=fields, lineterminator="\r\n" if "\r\n" in raw else "\n")
    w.writeheader()
    w.writerows(rows)
    CSV_PATH.write_text(out.getvalue(), "utf-8")
    print(f"{len(DUAS)} Qur'anic duas written; {len(rows)} rows in total")


if __name__ == "__main__":
    main()
