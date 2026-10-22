#!/usr/bin/env python3
"""Builds the offline city list for the manual location picker.

    python3 tools/build_cities.py [--cache DIR]

Downloads GeoNames dumps (CC BY 4.0, https://www.geonames.org) and writes
app/src/main/assets/cities.tsv with: geonameid, name, ascii name, country code, region,
latitude, longitude, IANA time zone, population.

Coverage: every city worldwide with population >= 15,000, plus every Indonesian place with
population >= 5,000 so smaller kabupaten seats are findable.
"""
import argparse
import io
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# Plain TSV: the Android build unpacks *.gz assets and drops the extension, and the APK is
# zip-compressed anyway.
OUT = ROOT / "app/src/main/assets/cities.tsv"
BASE = "https://download.geonames.org/export/dump/"


def fetch(name: str, cache: Path) -> bytes:
    path = cache / name
    if not path.exists():
        cache.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(BASE + name) as r:
            path.write_bytes(r.read())
    return path.read_bytes()


def read_zip_txt(data: bytes) -> list[list[str]]:
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        name = next(n for n in z.namelist() if n.endswith(".txt"))
        return [line.split("\t") for line in z.read(name).decode("utf-8").splitlines() if line]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", type=Path, default=ROOT / "build/geonames")
    args = ap.parse_args()

    admin1 = {}
    for line in fetch("admin1CodesASCII.txt", args.cache).decode("utf-8").splitlines():
        code, name, *_ = line.split("\t")
        admin1[code] = name

    rows = {}
    for r in read_zip_txt(fetch("cities15000.zip", args.cache)):
        rows[r[0]] = r
    for r in read_zip_txt(fetch("cities5000.zip", args.cache)):
        if r[8] == "ID":
            rows.setdefault(r[0], r)

    out = io.StringIO()
    for r in sorted(rows.values(), key=lambda r: -int(r[14] or 0)):
        region = admin1.get(f"{r[8]}.{r[10]}", "")
        fields = [r[0], r[1], r[2], r[8], region, f"{float(r[4]):.4f}", f"{float(r[5]):.4f}", r[17], r[14]]
        out.write("\t".join(f.replace("\t", " ") for f in fields) + "\n")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(out.getvalue(), encoding="utf-8")
    print(f"Wrote {len(rows)} cities to {OUT} ({OUT.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
