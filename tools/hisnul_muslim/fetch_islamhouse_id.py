"""Snapshots entries of IslamHouse's Indonesian Hisnul Muslim PDF for rows our PDF lacks.

    python3 -I tools/hisnul_muslim/fetch_islamhouse_id.py 69:178 69:179    # chapter:entry

The edition: «Kumpulan Doa dalam Alquran & Hadits» (Hisnul Muslim), Sa'id bin Ali al-Qahthani,
translated by Mahrus Ali, corrected by Abdullah Khaidhir, Fir'adi Nasruddin and Erwandi Tarmizi,
Maktab Dakwah dan Bimbingan Jaliyat Rabwah, 1430/2010, published by IslamHouse.com (PDF below).
hisnmuslim.com/i/id serves the same edition's text without its title page.

Writes content/source/import/islamhouse_hisn_id.json: the PDF's sha256 and, per book number,
the entry's text from its number to the next entry (lines of the PDF's broken Arabic glyphs
dropped, whitespace collapsed; nothing else changed). Used by build_content.py for
`translation_id_from = islamhouse:<no>`: the row's Indonesian must be cut verbatim from that
entry, and the build credits the translator and edition (IslamHouse's licence terms, 2026:
keep translator, publisher and edition where the source gives them).

Owner's choices: eating_08 (3 Nov 2026, decision page
https://claude.ai/artifact/EKRbEQQWuChwfejNvX93wC) and eating_09 (3 Nov, "do all the Opus work").
"""
import hashlib
import json
import re
import sys
import urllib.request
from datetime import date
from pathlib import Path

import pdfplumber

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "content/source/import/islamhouse_hisn_id.json"
PDF_URL = "https://d1.islamhouse.com/data/id/ih_books/single/id_hisn_almuslim.pdf"
EDITION = ("Hisnul Muslim (Kumpulan Doa dalam Alquran & Hadits), terj. Mahrus Ali, "
           "Maktab Dakwah dan Bimbingan Jaliyat Rabwah, 1430/2010, IslamHouse.com")
CID = re.compile(r"\(cid:\d+\)")
WORDS = re.compile(r"[a-z]{3,}")
HEADING = re.compile(r"[A-Z’'/ ,()\-;.]+")  # a chapter title in capitals
REPEATS = re.compile(r"(.)\1\1\1")        # the broken glyphs come in runs of four


def pdf_lines(path: Path) -> list[str]:
    """The PDF's text lines with the broken Arabic glyph lines left out."""
    with pdfplumber.open(path) as pdf:
        text = "\n".join(page.extract_text() or "" for page in pdf.pages)
    lines = (CID.sub("", line).strip() for line in text.splitlines())
    return [line for line in lines if not REPEATS.search(line) and (
        WORDS.search(line) or re.fullmatch(r"\d{1,3}", line)
        or (HEADING.fullmatch(line) and re.search(r"[AIUEO]", line)))]


def entry(lines: list[str], chapter: int, number: int) -> str:
    """Entry [number] of chapter [chapter]: from "N. " after the chapter heading to the next entry."""
    start = next(i for i in range(len(lines) - 1)
                 if lines[i] == str(chapter) and lines[i + 1].isupper())
    first = next(i for i in range(start, len(lines)) if lines[i].startswith(f"{number}. "))
    end = next((i for i in range(first + 1, len(lines) - 1)
                if re.match(rf"{number + 1}\. (?!HR)", lines[i])
                or (re.fullmatch(r"\d{1,3}", lines[i]) and lines[i + 1].isupper())), len(lines))
    return re.sub(r"\s+", " ", " ".join(lines[first:end])).strip()


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    req = urllib.request.Request(PDF_URL, headers={"User-Agent": "adwam-content-import"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = resp.read()
    tmp = ROOT / "build" / "islamhouse_hisn_id.pdf"
    tmp.parent.mkdir(exist_ok=True)
    tmp.write_bytes(data)
    lines = pdf_lines(tmp)
    doc = {"source": PDF_URL, "edition": EDITION, "sha256": hashlib.sha256(data).hexdigest(),
           "fetched": date.today().isoformat(), "entries": {}}
    for arg in argv:
        chapter, number = map(int, arg.split(":"))
        doc["entries"][str(number)] = {"chapter": chapter, "text": entry(lines, chapter, number)}
        print(f"{number}: {doc['entries'][str(number)]['text'][:90]}…")
    OUT.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
