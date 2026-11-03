#!/usr/bin/env python3
"""Checks the sources the app's text came from for corrections since we took it.

    python3 -I tools/check_sources.py                    # report; exit 1 if anything changed
    python3 -I tools/check_sources.py --update-baseline  # after reviewing, record the current state

IslamHouse's licence (docs/islamhouse-licence-request.md, "Reply") asks us to follow updated or
corrected versions when they are available. This compares, for the entries the app uses:

  * hisnmuslim.com's Arabic and English (by Hisnul Muslim number) against the fingerprints in
    tools/sources_baseline.json, recorded when we last checked;
  * IslamHouse's English and Indonesian PDFs (fortress-of-the-muslim, id_hisn_almuslim) by sha256;
  * every HadeethEnc hadith in content/source/import/hadeethenc.json against the live text,
    word for word (the snapshot is what the app shows).

A change is a lead to review, never something to copy in blindly: open the entry, compare it
with the row, and follow docs/content-guide.md ("Checking imported content") before importing.
Run it before each release that touches content, and at least once a month (docs/release.md).
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
import tempfile
import time
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools" / "hisnul_muslim"))
sys.path.insert(0, str(ROOT / "tools" / "hadeethenc"))

import fetch_arabic  # noqa: E402
import fetch_english  # noqa: E402
import import_hadeethenc  # noqa: E402

BASELINE = ROOT / "tools" / "sources_baseline.json"
CSV_PATH = ROOT / "content" / "source" / "duas.csv"
HADEETHENC = ROOT / "content" / "source" / "import" / "hadeethenc.json"
ISLAMHOUSE_ID = ROOT / "content" / "source" / "import" / "islamhouse_hisn_id.json"
EN_PDF = "https://d1.islamhouse.com/data/en/ih_books/single/en-fortress-of-the-muslim.pdf"
HISN_NO = re.compile(r"Hisnul Muslim no\. (\d+)")


def fingerprint(text: str) -> str:
    return hashlib.sha1(" ".join(text.split()).encode("utf-8")).hexdigest()[:16]


def used_numbers() -> set[int]:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as f:
        return {int(m.group(1)) for r in csv.DictReader(f) if (m := HISN_NO.search(r.get("notes") or ""))}


def pdf_sha256(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "adwam-content-check"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return hashlib.sha256(r.read()).hexdigest()


def current_state() -> dict:
    """Fingerprints of what the sources serve today, for the entries the app uses."""
    numbers = used_numbers()
    with tempfile.TemporaryDirectory() as tmp:
        arabic = fetch_arabic.fetch(Path(tmp) / "ar")
        fetch_english.CACHE = Path(tmp) / "en"  # always fetch fresh
        english = fetch_english.english()
    return {
        "hisnmuslim_ar": {str(n): fingerprint(arabic[n]["arabic"]) for n in sorted(numbers) if n in arabic},
        "hisnmuslim_en": {str(n): fingerprint(english[n]) for n in sorted(numbers) if n in english},
        "en_pdf_sha256": pdf_sha256(EN_PDF),
    }


def compare(old: dict, new: dict) -> list[str]:
    """What changed between two states, as readable lines."""
    out = []
    for key, label in (("hisnmuslim_ar", "hisnmuslim.com Arabic"), ("hisnmuslim_en", "hisnmuslim.com English")):
        before, after = old.get(key, {}), new.get(key, {})
        for no in sorted(set(before) | set(after), key=int):
            if before.get(no) != after.get(no):
                what = "gone" if no not in after else "new" if no not in before else "changed"
                out.append(f"{label}: Hisnul Muslim no. {no} {what}")
    if old.get("en_pdf_sha256") and old["en_pdf_sha256"] != new.get("en_pdf_sha256"):
        out.append(f"IslamHouse English PDF changed: {EN_PDF}")
    return out


def check_islamhouse_id() -> list[str]:
    snap = json.loads(ISLAMHOUSE_ID.read_text(encoding="utf-8"))
    if pdf_sha256(snap["source"]) != snap["sha256"]:
        return [f"IslamHouse Indonesian PDF changed: {snap['source']} "
                "(re-run tools/hisnul_muslim/fetch_islamhouse_id.py and review the diff)"]
    return []


def check_hadeethenc() -> list[str]:
    snap = json.loads(HADEETHENC.read_text(encoding="utf-8"))["hadeeths"]
    out = []
    for hid, langs in snap.items():
        for lang, fields in langs.items():
            if not isinstance(fields, dict):
                continue
            live = import_hadeethenc.fetch_one(lang, int(hid))
            for field, value in fields.items():
                if fingerprint(str(live.get(field, ""))) != fingerprint(str(value)):
                    out.append(f"HadeethEnc {hid} ({lang}) {field} changed")
            time.sleep(0.2)
    return out


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--update-baseline", action="store_true", help="record today's state as reviewed")
    args = ap.parse_args(argv)
    state = current_state()
    if args.update_baseline:
        BASELINE.write_text(json.dumps({"checked": date.today().isoformat(), **state}, indent=1) + "\n",
                            encoding="utf-8")
        print(f"baseline written: {len(state['hisnmuslim_ar'])} Arabic, {len(state['hisnmuslim_en'])} English entries")
        return 0
    old = json.loads(BASELINE.read_text(encoding="utf-8")) if BASELINE.exists() else {}
    changes = compare(old, state) + check_islamhouse_id() + check_hadeethenc()
    for line in changes:
        print(line)
    print(f"{len(changes)} change(s) since {old.get('checked', 'no baseline')}")
    return 1 if changes else 0


if __name__ == "__main__":
    sys.exit(main())
