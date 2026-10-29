"""Attaches a per-ayah recitation to the supplications from the Qur'an (`quran_NN` rows).

    python3 -I tools/quran_duas/fetch_audio.py            # download, join, transcode, update CSV
    python3 -I tools/quran_duas/fetch_audio.py --dry-run  # report only

Audio: Sheikh Maher al-Muaiqly, Hafs 'an 'Asim, published per ayah by the King Fahd Glorious
Qur'an Printing Complex (https://qurancomplex.gov.sa/quran-audios/). Its "Rights of use" make
the listed recordings available free of charge for use in computer applications; the app
credits the reciter and the Complex in About and in Settings → Audio.

The Complex serves the per-ayah files only inside one large zip, so this reads the zip's
directory with HTTP range requests and fetches just the ayat it needs (cached in
build/quran_audio/). Each row is one or more whole ayat (see import_quran_duas.py): their files
are joined in order, without trimming or editing, and transcoded to mono Opus (speech-tuned,
~32 kbps) as content/audio/quran_NN.opus. Rows whose `audio_file` is already set are left alone.
"""
import argparse
import csv
import io
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "content/source/duas.csv"
AUDIO_DIR = ROOT / "content/audio"
CACHE = ROOT / "build/quran_audio"
ZIP_URL = "https://download.qurancomplex.gov.sa/new-sounds/muaiqly-08/hafs/ayat-12-1-2020.zip"
# "Al-Baqarah [2]: 127–128" -> surah 2, ayat 127 to 128
REF = re.compile(r"\[(\d+)\]: (\d+)(?:[–-](\d+))?")
AYAH_FILE = re.compile(r"/10-(\d{3})(\d{3})-A08\.mp3$")


class HttpFile(io.RawIOBase):
    """A seekable, read-only view of a remote file, fetched with HTTP range requests."""

    def __init__(self, url: str):
        self.url, self.pos = url, 0
        with urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=60) as r:
            self.size = int(r.headers["Content-Length"])

    def readable(self) -> bool:
        return True

    def seekable(self) -> bool:
        return True

    def tell(self) -> int:
        return self.pos

    def seek(self, offset: int, whence: int = 0) -> int:
        self.pos = {0: offset, 1: self.pos + offset, 2: self.size + offset}[whence]
        return self.pos

    def readinto(self, buf) -> int:
        if self.pos >= self.size or not len(buf):
            return 0
        end = min(self.size, self.pos + len(buf)) - 1
        req = urllib.request.Request(self.url, headers={"Range": f"bytes={self.pos}-{end}"})
        for attempt in range(5):
            try:
                with urllib.request.urlopen(req, timeout=120) as r:
                    data = r.read()
                break
            except (urllib.error.URLError, ConnectionError, TimeoutError):
                if attempt == 4:
                    raise
                time.sleep(2 ** attempt)
        buf[: len(data)] = data
        self.pos += len(data)
        return len(data)


def ayat(row: dict) -> list[tuple[int, int]]:
    m = REF.search(row["source_1_no"])
    if not m:
        sys.exit(f"{row['id']}: no surah/ayah in source_1_no {row['source_1_no']!r}")
    surah, first, last = int(m.group(1)), int(m.group(2)), int(m.group(3) or m.group(2))
    return [(surah, a) for a in range(first, last + 1)]


def fetch(needed: set[tuple[int, int]]) -> dict[tuple[int, int], Path]:
    CACHE.mkdir(parents=True, exist_ok=True)
    paths = {k: CACHE / f"{k[0]:03d}{k[1]:03d}.mp3" for k in needed}
    missing = {k for k, p in paths.items() if not p.exists() or p.stat().st_size == 0}
    if missing:
        z = zipfile.ZipFile(io.BufferedReader(HttpFile(ZIP_URL), buffer_size=1 << 16))
        names = {}
        for name in z.namelist():
            m = AYAH_FILE.search(name)
            if m:
                names[(int(m.group(1)), int(m.group(2)))] = name
        for k in sorted(missing):
            if k not in names:
                sys.exit(f"No recording for {k[0]}:{k[1]} in the archive")
            paths[k].write_bytes(z.read(names[k]))
    return paths


def transcode(sources: list[Path], dst: Path) -> None:
    inputs = [arg for src in sources for arg in ("-i", str(src))]
    joined = "".join(f"[{i}:a]" for i in range(len(sources))) + f"concat=n={len(sources)}:v=0:a=1[a]"
    subprocess.run(
        ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", *inputs, "-filter_complex", joined,
         "-map", "[a]", "-map_metadata", "-1", "-ac", "1", "-ar", "48000", "-c:a", "libopus",
         "-b:a", "32k", "-application", "voip", str(dst)],
        check=True,
    )


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    raw = CSV_PATH.read_text("utf-8")
    rows = list(csv.DictReader(io.StringIO(raw)))
    fields = list(rows[0].keys())
    todo = [r for r in rows if r["id"].startswith("quran_") and not r["audio_file"].strip()]
    needed = {k for r in todo for k in ayat(r)}
    print(f"{len(todo)} Qur'an rows without audio, {len(needed)} ayat")
    if args.dry_run or not todo:
        return 0

    paths = fetch(needed)
    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    for r in todo:
        name = f"{r['id']}.opus"
        transcode([paths[k] for k in ayat(r)], AUDIO_DIR / name)
        r["audio_file"] = name

    out = io.StringIO()
    w = csv.DictWriter(out, fieldnames=fields, lineterminator="\r\n" if "\r\n" in raw else "\n")
    w.writeheader()
    w.writerows(rows)
    CSV_PATH.write_text(out.getvalue(), "utf-8")
    size = sum(f.stat().st_size for f in AUDIO_DIR.glob("quran_*.opus"))
    print(f"Wrote {len(todo)} files ({size // 1024} KiB); now run tools/build_content.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
