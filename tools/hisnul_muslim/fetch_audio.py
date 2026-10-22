"""Attaches the Hisnul Muslim recitation by Hamad al-Duraihim to every Hisnul Muslim row.

    python3 -I tools/hisnul_muslim/fetch_audio.py            # download, transcode, update CSV
    python3 -I tools/hisnul_muslim/fetch_audio.py --dry-run  # report only

Audio: hisnmuslim.com serves one MP3 per dhikr, keyed by its number in the book (the same
number each CSV row records in `notes`). The recording is published by IslamHouse.com, whose
FAQ allows republishing by any means, without changing the content, with the source credited
(https://d1.islamhouse.com/html/faq.htm). The app credits it in About and in Settings → Audio.

Files are transcoded to mono Opus (speech-tuned, ~32 kbps) without trimming or editing and
written to content/audio/hisn_NNN.opus. Rows whose `audio_file` is already set are left alone.
Downloads are cached in build/hisnmuslim_audio/.
"""
import argparse
import csv
import io
import json
import re
import subprocess
import time
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
AUDIO_DIR = ROOT / "content/audio"
CACHE = ROOT / "build/hisnmuslim_audio"
JSON_CACHE = ROOT / "build/hisnmuslim"
HISN_NO = re.compile(r"Hisnul Muslim no\. (\d+)")


def audio_urls() -> dict[int, str]:
    """Hisn number -> MP3 URL, from the chapter JSON that fetch_arabic.py cached."""
    urls = {}
    for f in JSON_CACHE.glob("*.json"):
        for items in json.loads(f.read_text("utf-8-sig")).values():
            for it in items:
                if it.get("AUDIO"):
                    urls[int(it["ID"])] = it["AUDIO"].replace("http://", "https://", 1)
    if not urls:
        sys.exit("No cached hisnmuslim.com JSON; run import_hisn.py (fetch_arabic) first.")
    return urls


def download(n: int, url: str) -> Path | None:
    """The cached MP3, or None when the site has no recording at that URL."""
    CACHE.mkdir(parents=True, exist_ok=True)
    out = CACHE / f"{n}.mp3"
    if not out.exists() or out.stat().st_size == 0:
        req = urllib.request.Request(url, headers={"User-Agent": "MuslimDaily-content-tools"})
        for attempt in range(4):
            try:
                with urllib.request.urlopen(req, timeout=120) as r:
                    data = r.read()
                break
            except urllib.error.HTTPError as e:
                if e.code == 404:
                    return None
                if attempt == 3:
                    raise
                time.sleep(2 ** attempt)
            except OSError:
                if attempt == 3:
                    raise
                time.sleep(2 ** attempt)
        if not data.startswith((b"ID3", b"\xff")):
            raise RuntimeError(f"{url}: not an MP3")
        out.write_bytes(data)
    return out


def transcode(src: Path, dst: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", str(src), "-map_metadata", "-1",
         "-vn", "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "32k", "-application", "voip", str(dst)],
        check=True,
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    raw = CSV_PATH.read_text("utf-8")
    rows = list(csv.DictReader(io.StringIO(raw)))
    fields = list(rows[0].keys())
    urls = audio_urls()
    attached, missing = 0, []
    for r in rows:
        m = HISN_NO.match(r.get("notes", ""))
        if not m or r.get("audio_file"):
            continue
        n = int(m.group(1))
        if n not in urls:
            missing.append(r["id"])
            continue
        name = f"hisn_{n:03d}.opus"
        if not args.dry_run:
            dst = AUDIO_DIR / name
            if not dst.exists():
                src = download(n, urls[n])
                if src is None:
                    missing.append(r["id"])
                    continue
                transcode(src, dst)
            r["audio_file"] = name
        attached += 1
    if not args.dry_run:
        out = io.StringIO()
        w = csv.DictWriter(out, fieldnames=fields, lineterminator="\r\n" if "\r\n" in raw else "\n")
        w.writeheader()
        w.writerows(rows)
        CSV_PATH.write_text(out.getvalue(), "utf-8")
    size = sum(f.stat().st_size for f in AUDIO_DIR.glob("hisn_*.opus"))
    print(f"{attached} rows with audio, {len(missing)} without a recording {missing[:10]}; "
          f"{len(list(AUDIO_DIR.glob('hisn_*.opus')))} files, {size // 1024} KiB")


if __name__ == "__main__":
    main()
