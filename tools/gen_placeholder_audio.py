#!/usr/bin/env python3
"""Generates silent Opus placeholder clips so audio, phrase highlighting and memorize mode can be
built and tested before real recitations exist.

    python3 tools/gen_placeholder_audio.py

For every row in content/source/duas.csv that is not published and whose audio file is missing,
writes a silent clip as long as its last segment. Also writes content/audio/silence.opus (60 s),
which memorize mode uses for repeat-aloud pauses. Requires ffmpeg with libopus.

Silence, not tones: the audio rule is clear recitation only, with no sound effects.
"""
import csv
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "content/audio"
CSV = ROOT / "content/source/duas.csv"


def silent(path: Path, seconds: float) -> None:
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-f", "lavfi", "-i", "anullsrc=r=48000:cl=mono",
         "-t", f"{seconds:.3f}", "-c:a", "libopus", "-b:a", "12k", str(path)],
        check=True,
    )


def main() -> int:
    AUDIO.mkdir(parents=True, exist_ok=True)
    silence = AUDIO / "silence.opus"
    if not silence.exists():
        silent(silence, 60)
    made = 0
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            name = (row.get("audio_file") or "").strip()
            if not name or row.get("status", "").strip().lower() == "published":
                continue
            target = AUDIO / name
            if target.exists():
                continue
            ends = [float(c.split("-")[1]) for c in row.get("audio_segments", "").split(";") if c.strip()]
            silent(target, max(ends) if ends else 3.0)
            made += 1
    print(f"Generated {made} placeholder clip(s) in {AUDIO}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
