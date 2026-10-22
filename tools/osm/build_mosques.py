"""Builds the offline mosque list (app/src/main/assets/mosques.bin) from OpenStreetMap.

    python3 -I tools/osm/build_mosques.py                 # fetch from Overpass, then build
    python3 -I tools/osm/build_mosques.py --csv FILE      # build from a saved Overpass CSV

Data: places of worship tagged religion=muslim in Indonesia (mosques and mushalla),
© OpenStreetMap contributors, ODbL 1.0. The app credits this on the mosque screen and in About.

Format (big-endian): b"MSQ1", int32 count, then per place: int32 lat×1e6, int32 lon×1e6,
uint16 name byte length, UTF-8 name ("" when OSM has none). Sorted by latitude.
"""
import argparse
import csv
import io
import math
import struct
import sys
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "app/src/main/assets/mosques.bin"
ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
]
QUERY = """[out:csv(::lat,::lon,name;false;"\\t")][timeout:500];
area["ISO3166-1"="ID"][admin_level=2]->.a;
nwr[amenity=place_of_worship][religion=muslim](area.a);
out center;"""
# Two entries with the same name closer than this are one building mapped twice (node + way).
DUPLICATE_METRES = 40


def fetch() -> str:
    body = urllib.parse.urlencode({"data": QUERY}).encode()
    for url in ENDPOINTS:
        try:
            with urllib.request.urlopen(url, body, timeout=600) as r:
                return r.read().decode("utf-8")
        except OSError as e:
            print(f"{url}: {e}", file=sys.stderr)
    sys.exit("No Overpass endpoint answered.")


def metres(a, b) -> float:
    lat = math.radians((a[0] + b[0]) / 2)
    dx = math.radians(b[1] - a[1]) * math.cos(lat)
    dy = math.radians(b[0] - a[0])
    return 6_371_000 * math.hypot(dx, dy)


def parse(text: str):
    places = []
    for row in csv.reader(io.StringIO(text), delimiter="\t"):
        if len(row) < 2 or not row[0] or not row[1]:
            continue
        lat, lon = float(row[0]), float(row[1])
        name = " ".join((row[2] if len(row) > 2 else "").split())[:120]
        places.append((lat, lon, name))
    return places


def dedupe(places):
    places.sort()
    kept = []
    for p in places:
        # Only neighbours in latitude order can be within DUPLICATE_METRES.
        if p[2] and any(q[2] == p[2] and metres(p, q) < DUPLICATE_METRES for q in kept[-20:]):
            continue
        kept.append(p)
    return kept


def write(places, out: Path):
    buf = bytearray(b"MSQ1")
    buf += struct.pack(">i", len(places))
    for lat, lon, name in places:
        n = name.encode("utf-8")
        buf += struct.pack(">iiH", round(lat * 1e6), round(lon * 1e6), len(n)) + n
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(bytes(buf))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", type=Path, help="Overpass CSV (lat, lon, name) saved earlier")
    ap.add_argument("--out", type=Path, default=OUT)
    args = ap.parse_args()
    text = args.csv.read_text("utf-8") if args.csv else fetch()
    places = dedupe(parse(text))
    write(places, args.out)
    named = sum(1 for p in places if p[2])
    print(f"{len(places)} places ({named} named), {args.out.stat().st_size // 1024} KiB -> {args.out}")


if __name__ == "__main__":
    main()
