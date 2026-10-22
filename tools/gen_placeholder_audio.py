#!/usr/bin/env python3
"""Generates content/audio/silence.opus (60 s), which memorize mode uses for repeat-aloud pauses.

    python3 tools/gen_placeholder_audio.py

Requires ffmpeg with libopus. Recitations themselves are never generated: every dua ships, so
a placeholder clip would reach users. Duas without a recording simply have no player.
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "content/audio"


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
    print(f"{silence} ready")
    return 0


if __name__ == "__main__":
    sys.exit(main())
