"""Fills `translation_en` for Hisnul Muslim rows from hisnmuslim.com's English API.

    python3 -I tools/hisnul_muslim/fetch_english.py --dry-run   # report coverage only
    python3 -I tools/hisnul_muslim/fetch_english.py --apply     # write content/source/duas.csv

NOT APPLIED YET. hisnmuslim.com names no translator or licence for the English text, so it
stays out of the app until IslamHouse confirms the use (docs/islamhouse-licence-request.md).

Entries are keyed by the book's standard number, the same number each CSV row records in
`notes` ("Hisnul Muslim no. N"). The text is taken unchanged apart from collapsing whitespace;
rows that already have an English translation (the Qur'anic duas) are left alone.
"""
import argparse
import csv
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
CACHE = ROOT / "build/hisnmuslim_en"
INDEX = "https://www.hisnmuslim.com/api/en/husn_en.json"
HISN_NO = re.compile(r"Hisnul Muslim no\. (\d+)")


def get(url: str) -> dict:
    path = CACHE / url.rsplit("/", 1)[-1]
    if not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(url, headers={"User-Agent": "MuslimDaily-content-tools"})
        with urllib.request.urlopen(req, timeout=60) as r:
            path.write_bytes(r.read())
        time.sleep(0.3)  # be gentle with the site
    # Some chapters contain raw tab characters inside strings.
    return json.loads(path.read_text("utf-8-sig"), strict=False)


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
            text = " ".join(str(item.get("TRANSLATED_TEXT", "")).split())
            if text:
                out[int(item["ID"])] = text
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
            w = csv.DictWriter(f, fieldnames=fields)
            w.writeheader()
            w.writerows(rows)
        print(f"Wrote {CSV_PATH}; now run tools/build_content.py and credit the source in About.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
