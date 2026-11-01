#!/usr/bin/env python3
"""Checks each phrase's audio by ear, with a second speech model: what is heard in the cut.

    venv/bin/python tools/verify_audio.py [--id ID ...] [--resume]   # writes build/align/verify.csv

`align_audio.py` times the written words with one model (wav2vec2, forced alignment: it is told
the text and finds it). This is the independent check that stands in for listening: Whisper
(openai/whisper-large-v3-turbo, MIT) transcribes each phrase segment (within `audio_range`)
without being told the text, and the words it hears are compared with the phrase's words. A cut
that starts inside a title the reciter reads ("doa ... yaqulu"), carries a word of the narration
or splits a phrase shows up as words heard that are not written, or written words not heard.

Nothing is written into the content: the CSV is for a person to act on. `common` is the longest
common subsequence of written and heard words (harakat stripped, alif/ya/ta marbuta unified), so
a single misheard word (a low-confidence word in a fast recitation) costs one, while a title or a
phrase in the wrong segment costs several. Rows are flagged `check` when a segment misses more
than one written word or hears more than one extra word.

Venv (as for `align_audio.py timings`; ~9 s a segment on 4 CPU cores, ~2.5 h for every row):

    python3 -m venv venv && venv/bin/pip install --index-url https://download.pytorch.org/whl/cpu \\
        torch && venv/bin/pip install transformers numpy
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import hadith_numbers as hn  # noqa: E402
import segment_audio as sa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/align/verify.csv"
MODEL = "openai/whisper-large-v3-turbo"
FIELDS = ["id", "status", "segment", "written", "heard", "common", "heard_text", "written_text"]


def lcs(a: list[str], b: list[str]) -> int:
    d = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]
    for i, x in enumerate(a):
        for j, y in enumerate(b):
            d[i + 1][j + 1] = d[i][j] + 1 if x == y else max(d[i][j + 1], d[i + 1][j])
    return d[-1][-1]


def judge(written: list[str], heard: list[str]) -> tuple[int, str]:
    common = lcs(written, heard)
    ok = len(written) - common <= 1 and len(heard) - common <= 1
    return common, "ok" if ok else "check"


def segments_of(row: dict) -> list[tuple[float, float]]:
    segs = bc.parse_segments(row.get("audio_segments") or "")
    if segs:
        return [(a, b) for a, b in segs]
    rng = bc.parse_segments(row.get("audio_range") or "")
    return [(rng[0][0], rng[0][1])] if rng else [(0.0, 0.0)]  # (0, 0): the whole file


def main() -> int:  # pragma: no cover - needs torch and the model
    import numpy as np
    import torch
    from transformers import pipeline

    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--id", action="append", default=[])
    ap.add_argument("--resume", action="store_true", help="keep build/align/verify.csv and skip its rows")
    args = ap.parse_args()
    torch.set_num_threads(4)
    asr = pipeline("automatic-speech-recognition", model=MODEL, device="cpu")
    _, rows, _ = sa.read_csv(sa.DUAS)
    out = sa.read_csv(OUT)[1] if args.resume and OUT.exists() else []
    done = {r["id"] for r in out}
    for row in rows:
        if not row.get("audio_file") or (args.id and row["id"] not in args.id) or row["id"] in done:
            continue
        phrases = [p.strip() for p in row["arabic"].split(bc.PHRASE_MARK)]
        segs = list(zip(segments_of(row), phrases))
        wavs = []
        for (a, b), _ in segs:
            cmd = ["ffmpeg", "-v", "quiet"] + (["-ss", str(a), "-to", str(b)] if b > a else []) + \
                ["-i", str(sa.AUDIO_DIR / row["audio_file"]), "-ac", "1", "-ar", "16000", "-f", "f32le", "-"]
            wav = np.frombuffer(subprocess.run(cmd, capture_output=True, check=True).stdout, dtype=np.float32).copy()
            wavs.append({"raw": wav, "sampling_rate": 16000})
        kw = {"generate_kwargs": {"language": "arabic", "task": "transcribe"}}
        long = [len(w["raw"]) > 30 * 16000 for w in wavs]  # over 30 s: long-form, one at a time
        short = iter(asr([w for w, x in zip(wavs, long) if not x], batch_size=8, **kw)) if not all(long) else iter(())
        heard_all = [asr(w, return_timestamps=True, **kw) if x else next(short) for w, x in zip(wavs, long)]
        for k, (((a, b), text), h) in enumerate(zip(segs, heard_all)):
            heard_text = h["text"].strip()
            written, heard = hn.words(text), hn.words(heard_text)
            common, status = judge(written, heard)
            out.append({"id": row["id"], "status": status, "segment": f"{a:.2f}-{b:.2f}", "written": len(written),
                        "heard": len(heard), "common": common, "heard_text": heard_text, "written_text": " ".join(written)})
            print(row["id"], k + 1, status, f"{common}/{len(written)} heard {len(heard)}", flush=True)
        OUT.parent.mkdir(parents=True, exist_ok=True)
        sa.write_csv(OUT, FIELDS, out)  # after every row, so a long run keeps what it has
    return 0


if __name__ == "__main__":
    sys.exit(main())
