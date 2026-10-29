"""Fills `translation_en` for Hisnul Muslim rows from hisnmuslim.com's English API.

    python3 -I tools/hisnul_muslim/fetch_english.py --dry-run   # report coverage only
    python3 -I tools/hisnul_muslim/fetch_english.py --apply     # write content/source/duas.csv

Applied 2026-10-29 on the owner's decision, credited to hisnmuslim.com (IslamHouse.com) in
About, under IslamHouse's published terms (republish unchanged, with the source named). The site
names no translator; see docs/islamhouse-licence-request.md.

Entries are keyed by the book's standard number, the same number each CSV row records in
`notes` ("Hisnul Muslim no. N"). The text is taken unchanged apart from collapsing whitespace;
rows that already have an English translation (the Qur'anic duas) are left alone.
"""
import argparse
import csv
import json
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
CACHE = ROOT / "build/hisnmuslim_en"
INDEX = "https://www.hisnmuslim.com/api/en/husn_en.json"
HISN_NO = re.compile(r"Hisnul Muslim no\. (\d+)")
# A chapter title whose closing quote and colon are missing ("What to say …: " then "["), the
# one malformation seen at the source (chapter 126).
UNCLOSED_TITLE = re.compile(r'^(\s*"[^"\n]*?):?\s*$(?=\s*\[)', re.M)
# Entries whose English the site stores in the transliteration field instead of TRANSLATED_TEXT,
# checked by hand against the IslamHouse PDF (2025 edition, same numbering). No. 185 is stored
# the same way but reads "If you are not invited", reversing the hadith, so it is left out.
MISFILED = {133}
# Entries the site gets wrong, taken instead from IslamHouse's English PDF ("Fortress of the
# Muslim", Osoul Foundation 2025, same numbering) on the owner's decision (2026-10-29). Its
# wording is a revised translation. No. 185 at the site reads "If you are not invited".
PDF = "https://d1.islamhouse.com/data/en/ih_books/single/en-fortress-of-the-muslim.pdf"
FROM_PDF = {185}
ENTRY_MARK = re.compile(r"-(\d+)\s*$")
FOOTNOTE_END = re.compile(r"\(\d\)$")


def get(url: str) -> dict:
    path = CACHE / url.rsplit("/", 1)[-1]
    if not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(url, headers={"User-Agent": "MuslimDaily-content-tools"})
        with urllib.request.urlopen(req, timeout=60) as r:
            path.write_bytes(r.read())
        time.sleep(0.3)  # be gentle with the site
    # Some chapters contain raw tab characters inside strings.
    text = path.read_text("utf-8-sig")
    try:
        return json.loads(text, strict=False)
    except json.JSONDecodeError:
        return json.loads(UNCLOSED_TITLE.sub(r'\1":', text), strict=False)


def pdf_entry(text: str, no: int) -> str | None:
    """The English of entry [no] in the PDF's text (pdftotext): the Latin lines after the
    Arabic line that ends "-no", up to the footnote mark "(1)", which is dropped."""
    lines = text.splitlines()
    start = next((i for i, l in enumerate(lines) if (m := ENTRY_MARK.search(l)) and int(m.group(1)) == no), None)
    if start is None:
        return None
    out = []
    for line in lines[start + 1:]:
        line = line.strip()
        latin = sum(c.isascii() and c.isalpha() for c in line)
        if latin < max(8, len(line) // 2):
            if out:
                break  # the English ended without a footnote mark
            continue  # still in the Arabic
        if line.startswith("\u201c"):
            continue  # a transliteration in quotes
        out.append(line)
        if FOOTNOTE_END.search(line):
            out[-1] = FOOTNOTE_END.sub("", line)
            break
    return " ".join(" ".join(out).split()) or None


def from_pdf() -> dict[int, str]:
    path = CACHE / "fortress-of-the-muslim.pdf"
    if not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(PDF, headers={"User-Agent": "MuslimDaily-content-tools"})
        with urllib.request.urlopen(req, timeout=120) as r:
            path.write_bytes(r.read())
    text = subprocess.run(["pdftotext", str(path), "-"], capture_output=True, text=True, check=True).stdout
    return {no: t for no in FROM_PDF if (t := pdf_entry(text, no))}


def english() -> dict[int, str]:
    """Hisn number -> English translation as served."""
    out = {}
    for ch in next(iter(get(INDEX).values())):
        try:
            chapter = get(ch["TEXT"].replace("http://", "https://", 1))
        except json.JSONDecodeError as e:
            # A few chapters are malformed at the source; skip them rather than guess.
            print(f"skipped chapter {ch['ID']} ({ch['TITLE']}): {e}", file=sys.stderr)
            continue
        for item in next(iter(chapter.values())):
            field = "LANGUAGE_ARABIC_TRANSLATED_TEXT" if int(item["ID"]) in MISFILED else "TRANSLATED_TEXT"
            text = " ".join(str(item.get(field) or "").split())
            if text and int(item["ID"]) not in FROM_PDF:
                out[int(item["ID"])] = text
    out.update(from_pdf())
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    mode = ap.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true")
    mode.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    en = english()
    with CSV_PATH.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        fields = reader.fieldnames
        rows = list(reader)
    filled = missing = 0
    for r in rows:
        m = HISN_NO.search(r.get("notes", ""))
        if not m or r["translation_en"].strip():
            continue
        text = en.get(int(m.group(1)))
        if text:
            r["translation_en"] = text
            filled += 1
        else:
            missing += 1
    print(f"{len(en)} English entries; would fill {filled} rows, {missing} without a match")
    if args.apply:
        with CSV_PATH.open("w", encoding="utf-8", newline="") as f:
            w = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
            w.writeheader()
            w.writerows(rows)
        print(f"Wrote {CSV_PATH}; now run tools/build_content.py and credit the source in About.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
