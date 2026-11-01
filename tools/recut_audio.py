#!/usr/bin/env python3
"""Re-times rows whose cuts `verify_audio.py` flagged, from where the words are actually heard.

    venv/bin/python tools/recut_audio.py words --id ID ...   # Whisper word times + loudness (model)
    python3 -I tools/recut_audio.py propose [--id ID ...]    # build/align/recut.csv (pure)
    python3 -I tools/recut_audio.py apply                    # writes the rows that pass

Found by the Whisper check (1 Nov): the hisnmuslim.com recordings open with a chapter title
("دعاء ... يقول") and some end with a count note read aloud ("ثلاث مرات"). The forced aligner,
told only the dua's words, squeezed them across the title, so in about thirty rows the clip
starts on the title and in some every phrase plays one segment late.

`words` stores, per recording, Whisper's word times (openai/whisper-large-v3-turbo, word
timestamps, not told the text) and the loudness every 20 ms in
`tools/align/whisper_words.json`.

`propose`, per row: the written words of each phrase are matched to the heard words (consonant
skeletons, one letter or a dropped suffix apart allowed, longest common subsequence; a match standing alone, one word away from any other
match, is dropped, so a "الله" in the title does not anchor a phrase). A phrase runs from its
first heard word to its last. Each cut goes at the quietest 20 ms in the gap between two
phrases (or between the title and the first phrase, the last phrase and the note), never more
than EDGE_PAD outside the words. A row is proposed only when every phrase has at least
MIN_HEARD of its words heard and the words at both its ends heard, and both edges of the clip sit at
least QUIET dB below the recording's loudest frame (a break between phrases goes at the quietest
point, as a reciter may run straight on). Phrase marks are not touched: only `audio_range`
and `audio_segments` change, and only when some boundary moves by more than MIN_MOVE.
Re-run `verify_audio.py --id` on the rows applied.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import hadith_numbers as hn  # noqa: E402
import segment_audio as sa  # noqa: E402
import verify_audio as va  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
WORDS = ROOT / "tools/align/whisper_words.json"
OUT = ROOT / "build/align/recut.csv"
FRAME = 0.02      # seconds per loudness frame
EDGE_PAD = 0.35   # at most this much before a phrase's first word or after its last
QUIET = 22.0      # dB below the loudest frame for a cut
MIN_HEARD = 0.6   # share of a phrase's words that must be heard
MIN_MOVE = 0.25   # seconds; smaller differences are left alone
SLACK = 0.3       # Whisper's word times run end to start; a cut is looked for this far around
FIELDS = ["id", "status", "reason", "audio_range", "audio_segments", "old_range", "old_segments"]


def near(a: str, b: str) -> bool:
    """Same word, or one letter apart, or one a prefix of the other (Whisper dropping a suffix:
    "ثبت" for "ثبته"), for words of three letters or more."""
    if a == b:
        return True
    if min(len(a), len(b)) < 3:
        return False
    if a.startswith(b) or b.startswith(a):
        return True
    if abs(len(a) - len(b)) > 1:
        return False
    d = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        prev, d[0] = d[0], i
        for j, y in enumerate(b, 1):
            prev, d[j] = d[j], min(d[j] + 1, d[j - 1] + 1, prev + (x != y))
    return d[-1] <= 1


def match(a: list[str], b: list[str]) -> list[tuple[int, int]]:
    """Index pairs of a longest common subsequence under `near`."""
    d = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]
    for i in range(len(a) - 1, -1, -1):
        for j in range(len(b) - 1, -1, -1):
            d[i][j] = d[i + 1][j + 1] + 1 if near(a[i], b[j]) else max(d[i + 1][j], d[i][j + 1])
    pairs, i, j = [], 0, 0
    while i < len(a) and j < len(b):
        if near(a[i], b[j]) and d[i][j] == d[i + 1][j + 1] + 1:
            pairs.append((i, j)); i += 1; j += 1
        elif d[i + 1][j] >= d[i][j + 1]:
            i += 1
        else:
            j += 1
    return pairs


def anchored(pairs: list[tuple[int, int]]) -> list[tuple[int, int]]:
    """Drops matches with no other match next to them in both texts (a lone common word)."""
    keep = []
    for k, (i, j) in enumerate(pairs):
        near = [p for p in pairs[max(0, k - 1):k] + pairs[k + 1:k + 2] if abs(p[0] - i) <= 2 and abs(p[1] - j) <= 2]
        if near:
            keep.append((i, j))
    return keep


def quietest(rms: list[float], a: float, b: float) -> tuple[float, float]:
    """(time, dB) of the quietest frame in [a, b]."""
    lo, hi = max(0, int(a / FRAME)), min(len(rms) - 1, int(b / FRAME))
    if hi < lo:
        k = min(max(lo, 0), len(rms) - 1)
        return k * FRAME, rms[k]
    k = min(range(lo, hi + 1), key=lambda i: rms[i])
    return round(k * FRAME, 2), rms[k]


def propose(row: dict, entry: dict) -> dict:
    out = {"id": row["id"], "status": "fail", "reason": "", "audio_range": "", "audio_segments": "",
           "old_range": row.get("audio_range", ""), "old_segments": row.get("audio_segments", "")}
    heard = entry["words"]                      # [[start, end, word], ...]
    rms, dur = entry["rms"], entry["duration"]  # dB relative to the loudest frame
    hw = [va.skeleton(hn.words(w[2])) for w in heard]
    flat_h, owner_h = [], []
    for k, ws in enumerate(hw):
        for w in ws:
            flat_h.append(w); owner_h.append(k)
    phrases = [p.strip() for p in row["arabic"].split(bc.PHRASE_MARK)]
    flat_w, owner_w = [], []
    for k, p in enumerate(phrases):
        for w in va.skeleton(hn.words(p)):
            flat_w.append(w); owner_w.append(k)
    pairs = anchored(match(flat_w, flat_h))
    spans = []
    for k, p in enumerate(phrases):
        idx = [i for i, o in enumerate(owner_w) if o == k]
        mine = [(i, j) for i, j in pairs if owner_w[i] == k]
        if not idx or len(mine) < MIN_HEARD * len(idx) or mine[0][0] > idx[0] + 1 or mine[-1][0] < idx[-1] - 1:
            out["reason"] = f"phrase {k + 1}: its words are not heard clearly enough"
            return out
        spans.append((heard[owner_h[mine[0][1]]][0], heard[owner_h[mine[-1][1]]][1],
                      owner_h[mine[0][1]], owner_h[mine[-1][1]]))
    cuts = []
    # start: in the gap after the word heard before the first phrase (the title), if any
    s0, _, h0, _ = spans[0]
    prev_end = heard[h0 - 1][1] if h0 > 0 else 0.0
    t, db = quietest(rms, max(prev_end, s0 - EDGE_PAD), s0 + SLACK) if h0 > 0 else (max(0.0, s0 - EDGE_PAD), -99.0)
    cuts.append((t, db))
    for (_, e1, _, _), (s2, _, _, _) in zip(spans, spans[1:]):
        cuts.append(quietest(rms, e1 - SLACK, max(e1, s2) + SLACK))
    e9, _, _, h9 = spans[-1][1], None, None, spans[-1][3]
    if h9 + 1 < len(heard):
        t, db = quietest(rms, e9 - SLACK, min(heard[h9 + 1][0] + SLACK, e9 + EDGE_PAD + 0.5))
    else:
        t, db = dur, -99.0
    cuts.append((t, db))
    # the clip's two edges decide what is played, so they must be quiet; a break between two
    # phrases only moves the highlight, and a reciter may run straight on there
    loud = [f"{t:.2f}" for t, db in (cuts[0], cuts[-1]) if db > -QUIET]
    if loud:
        out["reason"] = f"a cut is not in a quiet stretch ({', '.join(loud)})"
        return out
    times = [t for t, _ in cuts]
    if any(b <= a for a, b in zip(times, times[1:])):
        out["reason"] = "cuts out of order"
        return out
    rng = f"{times[0]:.2f}-{times[-1]:.2f}"
    segs = "; ".join(f"{a:.2f}-{b:.2f}" for a, b in zip(times, times[1:])) if len(phrases) > 1 else ""
    old = [x for s in bc.parse_segments(out["old_segments"]) for x in s] or \
        [x for s in bc.parse_segments(out["old_range"]) for x in s] or [0.0, dur]
    new = [x for s in bc.parse_segments(segs) for x in s] if segs else times
    moved = len(old) != len(new) or any(abs(a - b) > MIN_MOVE for a, b in zip(old, new))
    out.update(audio_range=rng, audio_segments=segs, status="ok" if moved else "same")
    return out


def compute_words(ids: set[str]) -> None:  # pragma: no cover - needs torch and the model
    import numpy as np
    import torch
    from transformers import pipeline

    torch.set_num_threads(4)
    asr = pipeline("automatic-speech-recognition", model=va.MODEL, device="cpu")
    data = json.loads(WORDS.read_text(encoding="utf-8")) if WORDS.exists() else {}
    _, rows, _ = sa.read_csv(sa.DUAS)
    for row in rows:
        f = row.get("audio_file", "")
        if not f or row["id"] not in ids or f in data:
            continue
        raw = subprocess.run(["ffmpeg", "-v", "quiet", "-i", str(sa.AUDIO_DIR / f), "-ac", "1", "-ar", "16000",
                              "-f", "f32le", "-"], capture_output=True, check=True).stdout
        wav = np.frombuffer(raw, dtype=np.float32).copy()
        o = asr({"raw": wav, "sampling_rate": 16000}, return_timestamps="word",
                generate_kwargs={"language": "arabic", "task": "transcribe"})
        n = int(16000 * FRAME)
        db = [20 * float(np.log10(np.sqrt(np.mean(wav[i:i + n] ** 2)) + 1e-9)) for i in range(0, len(wav) - n, n)]
        peak = max(db)
        data[f] = {"duration": round(len(wav) / 16000, 2),
                   "words": [[round(c["timestamp"][0], 2), round(c["timestamp"][1] or c["timestamp"][0], 2), c["text"].strip()]
                             for c in o["chunks"]],
                   "rms": [round(x - peak, 1) for x in db]}
        print(f, len(o["chunks"]), flush=True)
        WORDS.parent.mkdir(parents=True, exist_ok=True)
        WORDS.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name in ("words", "propose"):
        sub.add_parser(name).add_argument("--id", action="append", default=[])
    sub.add_parser("apply")
    args = ap.parse_args()
    if args.cmd == "words":
        compute_words(set(args.id))
        return 0
    if args.cmd == "propose":
        data = json.loads(WORDS.read_text(encoding="utf-8"))
        _, rows, _ = sa.read_csv(sa.DUAS)
        res = [propose(r, data[r["audio_file"]]) for r in rows
               if r.get("audio_file") in data and (not args.id or r["id"] in args.id)]
        OUT.parent.mkdir(parents=True, exist_ok=True)
        sa.write_csv(OUT, FIELDS, res)
        for r in res:
            print(r["id"], r["status"], r["reason"], r["old_range"] or "-", "|", r["old_segments"] or "-", "->",
                  r["audio_range"], "|", r["audio_segments"] or "-")
        return 0
    fields, rows, nl = sa.read_csv(sa.DUAS)
    by = {r["id"]: r for r in sa.read_csv(OUT)[1] if r["status"] == "ok"}
    done = []
    for r in rows:
        if r["id"] in by:
            r["audio_range"], r["audio_segments"] = by[r["id"]]["audio_range"], by[r["id"]]["audio_segments"]
            done.append(r["id"])
    sa.write_csv(sa.DUAS, fields, rows, nl)
    print("applied:", " ".join(done) or "-")
    return 0


if __name__ == "__main__":
    sys.exit(main())
