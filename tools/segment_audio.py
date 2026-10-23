#!/usr/bin/env python3
"""Suggests phrase breaks and audio segments for duas, for a person to check and approve.

    python3 -I tools/segment_audio.py suggest --event morning --event evening
    python3 -I tools/segment_audio.py suggest --id morning_03 --id morning_04
    python3 -I tools/segment_audio.py apply              # writes approved rows to duas.csv

`suggest` reads content/source/duas.csv and, for each selected dua that has audio but no
phrases yet, finds:
  * text breaks: places where the source text already has punctuation (، . : ؛ ؟ ! ۝, the end
    of a Qur'anic quote ﴾ or the start of one ﴿). Only a "|" is inserted at an existing space;
    the words, marks and spacing are never changed (checked for every row).
  * audio pauses: stretches of near-silence in the recitation (ffmpeg decodes the file).
It pairs breaks with pauses in order, using where each break falls in the text to estimate when
it is recited, and keeps only the pairs that agree. The result goes to:
    build/segments/review.csv    one row per dua: proposed Arabic with "|" and segments
    build/segments/review.html   open in a browser; tap a phrase to hear just that segment

A reviewer listens, fixes `arabic_marked` or `audio_segments` where needed (the counts must
stay equal), and sets `approved` to "ya". `apply` copies approved rows into duas.csv, refusing
any row whose Arabic changed since the suggestion. Then run tools/build_content.py.

Re-running tools/hisnul_muslim/import_hisn.py --merge replaces imported rows, including their
phrase marks; apply segmentation after the final import.
"""
from __future__ import annotations

import argparse
import array
import csv
import hashlib
import html
import math
import os
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DUAS = ROOT / "content/source/duas.csv"
AUDIO_DIR = ROOT / "content/audio"
OUT_DIR = ROOT / "build/segments"
ARABIC_FONT = ROOT / "app/src/main/res/font/noto_naskh_arabic.ttf"  # the app's default face

SAMPLE_RATE = 8000
FRAME = 0.02  # seconds per loudness frame
MIN_PAUSE = 0.25  # seconds of near-silence that count as a pause
QUIET_DB = -28.0  # a frame is quiet when this far below the loud (95th percentile) level
MIN_SPEECH = 0.3  # a sound shorter than this before the first pause is a click, not speech

# Punctuation after which a phrase may end, and the Qur'anic opening bracket before which one
# may start. These are already in the source text; the tool only marks them.
BREAK_AFTER = set("،.:؛؟!۝﴾")
BREAK_BEFORE = set("﴿")

REVIEW_FIELDS = [
    "id", "event", "audio_file", "duration", "text_breaks", "audio_pauses", "phrases", "status",
    "approved", "arabic_marked", "audio_segments", "arabic_sha1",
]


@dataclass(frozen=True)
class Pause:
    start: float
    end: float

    @property
    def mid(self) -> float:
        return (self.start + self.end) / 2

    @property
    def length(self) -> float:
        return self.end - self.start


# ---------------------------------------------------------------- audio

def decode(path: Path) -> list[float]:
    """Mono samples in [-1, 1] at SAMPLE_RATE."""
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-ac", "1", "-ar", str(SAMPLE_RATE), "-f", "s16le", "-"],
        capture_output=True, check=True,
    ).stdout
    pcm = array.array("h")
    pcm.frombytes(raw[: len(raw) // 2 * 2])
    if sys.byteorder == "big":
        pcm.byteswap()
    return [s / 32768 for s in pcm]


def loudness(samples: list[float], rate: int = SAMPLE_RATE) -> list[float]:
    """RMS level in dB per FRAME."""
    hop = int(rate * FRAME)
    out = []
    for i in range(0, len(samples) - hop + 1, hop):
        chunk = samples[i:i + hop]
        rms = math.sqrt(sum(s * s for s in chunk) / hop)
        out.append(20 * math.log10(rms + 1e-9))
    return out


def find_pauses(levels: list[float]) -> tuple[float, float, list[Pause]]:
    """Returns (speech start, speech end, pauses inside the speech) in seconds."""
    if not levels:
        return 0.0, 0.0, []
    loud = sorted(levels)[int(0.95 * (len(levels) - 1))]
    quiet = [db < loud + QUIET_DB for db in levels]

    runs, i = [], 0  # (is_quiet, first frame, end frame)
    while i < len(quiet):
        j = i
        while j < len(quiet) and quiet[j] == quiet[i]:
            j += 1
        runs.append((quiet[i], i, j))
        i = j
    speech = [(a, b) for q, a, b in runs if not q and (b - a) * FRAME >= MIN_SPEECH]
    if not speech:
        return 0.0, len(levels) * FRAME, []
    first, last = speech[0][0], speech[-1][1]
    pauses = [
        Pause(round(a * FRAME, 2), round(b * FRAME, 2))
        for q, a, b in runs
        if q and a > first and b < last and (b - a) * FRAME >= MIN_PAUSE
    ]
    return round(first * FRAME, 2), round(last * FRAME, 2), pauses


# ---------------------------------------------------------------- text

def break_candidates(arabic: str) -> list[int]:
    """Offsets of spaces where a phrase may end, from punctuation already in the text."""
    spots = set()
    for i, ch in enumerate(arabic):
        if ch != " " or i == 0 or i == len(arabic) - 1:
            continue
        before, after = arabic[i - 1], arabic[i + 1]
        # A verse sign may be followed by its number; the break goes after the number.
        j = i - 1
        while j > 0 and before.isdigit():
            j -= 1
            before = arabic[j]
        if before in BREAK_AFTER or after in BREAK_BEFORE:
            spots.add(i)
    return sorted(spots)


def mark(arabic: str, spaces: list[int]) -> str:
    """Puts " | " at the given spaces. Raises if the text would change when the marks are removed."""
    out, prev = [], 0
    for s in sorted(spaces):
        out.append(arabic[prev:s])
        out.append(" | ")
        prev = s + 1
    out.append(arabic[prev:])
    marked = "".join(out)
    if bc.split_phrases(marked)[0] != arabic.strip():
        raise ValueError("marking would change the text")
    return marked


# ---------------------------------------------------------------- pairing

def align(arabic: str, spaces: list[int], start: float, end: float, pauses: list[Pause]) -> list[tuple[int, Pause]]:
    """Pairs text breaks with pauses in order.

    Each break's expected time is its share of the letters times the speech length. A pair is
    allowed when the pause is within a tolerance of that estimate; among the allowed pairings the
    one with the most pairs, then the closest fit, wins (an ordered alignment, like a diff).
    """
    letters = [sum(1 for c in arabic[:s] if not c.isspace()) for s in spaces]
    total = max(1, sum(1 for c in arabic if not c.isspace()))
    span = max(end - start, 1e-6)
    expected = [start + span * n / total for n in letters]
    tol = max(1.5, 0.12 * span)

    def score(i: int, j: int) -> float | None:
        d = abs(expected[i] - pauses[j].mid)
        return None if d > tol else 1.0 + 0.5 * (1 - d / tol) + min(pauses[j].length, 1.0) * 0.2

    n, m = len(spaces), len(pauses)
    best = [[0.0] * (m + 1) for _ in range(n + 1)]
    step = [[""] * (m + 1) for _ in range(n + 1)]
    for i in range(n - 1, -1, -1):
        for j in range(m - 1, -1, -1):
            options = [(best[i + 1][j], "t"), (best[i][j + 1], "p")]
            s = score(i, j)
            if s is not None:
                options.append((best[i + 1][j + 1] + s, "m"))
            best[i][j], step[i][j] = max(options)
        best[i][m], step[i][m] = best[i + 1][m], "t"
    for j in range(m - 1, -1, -1):
        best[n][j], step[n][j] = best[n][j + 1], "p"

    pairs, i, j = [], 0, 0
    while i < n and j < m:
        if step[i][j] == "m":
            pairs.append((spaces[i], pauses[j]))
            i, j = i + 1, j + 1
        elif step[i][j] == "t":
            i += 1
        else:
            j += 1
    return pairs


def segments(duration: float, pauses: list[Pause]) -> list[tuple[float, float]]:
    """Contiguous segments split at each pause's midpoint, so one phrase is always active."""
    cuts = [0.0] + [round(p.mid, 2) for p in pauses] + [round(duration, 2)]
    return list(zip(cuts, cuts[1:]))


def format_segments(segs: list[tuple[float, float]]) -> str:
    return "; ".join(f"{a:.2f}-{b:.2f}" for a, b in segs)


def sha1(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]


def review_status(breaks: int, pauses: int, pairs: int) -> str:
    """match: every break and pause paired. close: all of the scarcer side paired (the rest is
    a pause the text has no punctuation for, or punctuation the reciter ran through).
    partial: some pairs. none: nothing to split."""
    if pairs == 0:
        return "none"
    if pairs == breaks == pauses:
        return "match"
    return "close" if pairs == min(breaks, pauses) else "partial"


def suggest_row(row: dict, samples: list[float]) -> dict:
    arabic = row["arabic"]
    duration = len(samples) / SAMPLE_RATE
    start, end, pauses = find_pauses(loudness(samples))
    spaces = break_candidates(arabic)
    pairs = align(arabic, spaces, start, end, pauses)
    status = review_status(len(spaces), len(pauses), len(pairs))
    return {
        "id": row["id"], "event": row["event"], "audio_file": row["audio_file"],
        "duration": f"{duration:.2f}", "text_breaks": len(spaces), "audio_pauses": len(pauses),
        "phrases": len(pairs) + 1 if pairs else 1, "status": status, "approved": "",
        "arabic_marked": mark(arabic, [s for s, _ in pairs]) if pairs else "",
        "audio_segments": format_segments(segments(duration, [p for _, p in pairs])) if pairs else "",
        "arabic_sha1": sha1(arabic),
    }


# ---------------------------------------------------------------- files

def read_csv(path: Path) -> tuple[list[str], list[dict], str]:
    raw = path.read_bytes().decode("utf-8-sig")
    rows = list(csv.DictReader(raw.splitlines(keepends=True)))
    fields = list(rows[0].keys()) if rows else []
    return fields, rows, "\r\n" if "\r\n" in raw else "\n"


def write_csv(path: Path, fields: list[str], rows: list[dict], newline: str = "\n") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields, lineterminator=newline)
        w.writeheader()
        w.writerows(rows)


def review_html(results: list[dict], audio_rel: str, font_rel: str) -> str:
    cards = []
    for r in results:
        if not r["audio_segments"]:
            continue
        phrases = [p.strip() for p in r["arabic_marked"].split(bc.PHRASE_MARK) if p.strip()]
        segs = bc.parse_segments(r["audio_segments"])
        rows = "".join(
            f'<li><button data-a="{a}" data-b="{b}">{a:.2f}–{b:.2f}</button>'
            f'<span lang="ar" dir="rtl">{html.escape(p)}</span></li>'
            for p, (a, b) in zip(phrases, segs)
        )
        cards.append(
            f'<section><h2>{html.escape(r["id"])} <small>{r["status"]}: {r["text_breaks"]} text breaks, '
            f'{r["audio_pauses"]} pauses, {r["phrases"]} phrases</small></h2>'
            f'<audio controls preload="none" src="{audio_rel}/{html.escape(r["audio_file"])}"></audio>'
            f"<ol>{rows}</ol></section>"
        )
    return f"""<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Segment review</title>
<style>
@font-face{{font-family:"Noto Naskh Arabic";src:url("{font_rel}")}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0 auto;max-width:56rem;padding:1rem;background:#fbf8f1;color:#1c1c18}}
section{{border-top:1px solid #6f7976;padding:1rem 0}} small{{font-weight:400;color:#3f4947}}
ol{{padding:0;list-style:none}} li{{display:flex;gap:1rem;align-items:baseline;padding:.25rem 0}}
li span{{flex:1;font-family:"Noto Naskh Arabic",serif;font-size:1.5rem;line-height:2;text-align:right}}
button{{min-width:7rem;min-height:48px;font:inherit;font-variant-numeric:tabular-nums}}
li.on span{{background:#cce8e5;outline:2px solid #006a65}}
</style>
<h1>Segment review</h1>
<p>Tap a time to hear that phrase. Approve rows in <code>review.csv</code>, then run
<code>segment_audio.py apply</code>.</p>
{''.join(cards)}
<script>
document.querySelectorAll('section').forEach(s => {{
  const audio = s.querySelector('audio'); let stopAt = null;
  audio.addEventListener('timeupdate', () => {{ if (stopAt !== null && audio.currentTime >= stopAt) {{ audio.pause(); stopAt = null; }} }});
  s.querySelectorAll('button').forEach(b => b.addEventListener('click', () => {{
    s.querySelectorAll('li').forEach(li => li.classList.remove('on')); b.parentElement.classList.add('on');
    audio.currentTime = +b.dataset.a; stopAt = +b.dataset.b; audio.play();
  }}));
}});
</script></html>
"""


def suggest(ids: set[str], events: set[str], duas: Path = DUAS, audio_dir: Path = AUDIO_DIR,
            out_dir: Path = OUT_DIR, decoder=decode, font: Path = ARABIC_FONT) -> list[dict]:
    _, rows, _ = read_csv(duas)
    results = []
    for row in rows:
        if (ids or events) and row["id"] not in ids and row["event"] not in events:
            continue
        if not row["audio_file"] or bc.PHRASE_MARK in row["arabic"] or row["audio_segments"].strip():
            continue
        results.append(suggest_row(row, decoder(audio_dir / row["audio_file"])))
    write_csv(out_dir / "review.csv", REVIEW_FIELDS, results)
    rel = Path(os.path.relpath(audio_dir, out_dir)).as_posix()
    font_rel = Path(os.path.relpath(font, out_dir)).as_posix()
    (out_dir / "review.html").write_text(review_html(results, rel, font_rel), encoding="utf-8")
    return results


def apply(review: Path = OUT_DIR / "review.csv", duas: Path = DUAS) -> tuple[list[str], list[str]]:
    """Copies approved rows into duas.csv. Returns (applied ids, problems)."""
    _, reviewed, _ = read_csv(review)
    fields, rows, newline = read_csv(duas)
    by_id = {r["id"]: r for r in rows}
    applied, problems = [], []
    for r in reviewed:
        if (r.get("approved") or "").strip().lower() not in bc.YES:
            continue
        target = by_id.get(r["id"])
        if target is None:
            problems.append(f"{r['id']}: not in duas.csv")
            continue
        if sha1(target["arabic"]) != r["arabic_sha1"]:
            problems.append(f"{r['id']}: Arabic changed since the suggestion; run suggest again")
            continue
        try:
            text, breaks = bc.split_phrases(r["arabic_marked"])
            segs = bc.parse_segments(r["audio_segments"])
        except ValueError:
            problems.append(f"{r['id']}: audio_segments must look like '0.00-1.80; 1.80-3.90'")
            continue
        if text != target["arabic"].strip():
            problems.append(f"{r['id']}: arabic_marked differs from the source text beyond the '|' marks")
            continue
        if len(breaks) + 1 != len(segs):
            problems.append(f"{r['id']}: {len(breaks) + 1} phrases but {len(segs)} segments")
            continue
        target["arabic"] = r["arabic_marked"]
        target["audio_segments"] = r["audio_segments"]
        applied.append(r["id"])
    if applied:
        write_csv(duas, fields, rows, newline)
    return applied, problems


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("suggest", help="write build/segments/review.csv and review.html")
    s.add_argument("--id", action="append", default=[], help="dua id (repeatable)")
    s.add_argument("--event", action="append", default=[], help="event code (repeatable)")
    sub.add_parser("apply", help="copy approved rows from review.csv into duas.csv")
    args = ap.parse_args()

    if args.cmd == "suggest":
        results = suggest(set(args.id), set(args.event))
        counts = {k: sum(1 for r in results if r["status"] == k) for k in ("match", "close", "partial", "none")}
        print(f"{len(results)} duas: " + ", ".join(f"{n} {k}" for k, n in counts.items()))
        print(f"Review: {OUT_DIR / 'review.html'}\nApprove in: {OUT_DIR / 'review.csv'}")
        return 0
    applied, problems = apply()
    for p in problems:
        print(f"skipped {p}")
    if applied:
        print(f"applied {len(applied)} row(s) to {DUAS.relative_to(ROOT)}; now run tools/build_content.py")
    else:
        print("nothing applied: set approved to 'ya' in build/segments/review.csv first")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
