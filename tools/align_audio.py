#!/usr/bin/env python3
"""Times every word of a dua in its recording, then proposes the clip range and phrase breaks.

Two steps:

    # 1. Word timings (needs the speech model; run in a separate venv, see below):
    venv/bin/python tools/align_audio.py timings [--id ID ...]
    # 2. Proposals and apply (plain Python, uses the committed timings):
    python3 -I tools/align_audio.py suggest            # build/align/review.csv + review.html
    python3 -I tools/align_audio.py apply              # writes rows that pass every check

`timings` runs CTC forced alignment (wav2vec2 XLSR-53 Arabic, jonatasgrosman/
wav2vec2-large-xlsr-53-arabic, Apache-2.0) of the row's Arabic (`arabic_full` when the row has
one, else `arabic`) against its recording and stores, for each word, its start, end and the
model's confidence in `tools/align/word_timings.json`. The text is only read: harakat are
stripped for matching, nothing is written back. This stands in for listening: the timings say
where each written word is recited, so the clip and phrase boundaries come from the recording
itself, not from guessed pauses. Venv:

    python3 -m venv venv && venv/bin/pip install --index-url https://download.pytorch.org/whl/cpu \\
        torch torchaudio && venv/bin/pip install transformers numpy

`suggest` / `apply`, for each row with audio:

  * `audio_range`: from just before the first word of `arabic` to just after its last word, set
    when the recording carries more than the row (the narration around a dua, a repetition
    note read aloud, trailing silence over a second).
  * phrase marks `|` and `audio_segments`: a break may only go where the text already has
    punctuation (segment_audio.break_candidates); it is kept where the reciter pauses there
    (a gap of at least MIN_GAP between the two words) and both phrases last at least
    MIN_PHRASE. A segment boundary is the middle of that gap.

A row is applied only when every word is timed, the mean confidence is at least MIN_MEAN and the
words a cut rests on (first, last, either side of a break) are at least MIN_EDGE, so a recording
that does not match its text is never cut. A row whose Arabic already has phrase marks (set by
hand, e.g. one per surah) keeps them; only their times are computed. Rows that
fail are listed with the reason in review.csv. `apply` refuses rows whose Arabic changed since
the timings (sha1).
"""
from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import segment_audio as sa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TIMINGS = ROOT / "tools/align/word_timings.json"
OUT_DIR = ROOT / "build/align"
MODEL = "jonatasgrosman/wav2vec2-large-xlsr-53-arabic"

MIN_MEAN = 0.70   # mean word confidence for a row to be cut automatically
MIN_EDGE = 0.50   # the words a cut rests on (first, last, either side of a break)
MIN_GAP = 0.18    # seconds between two words for a phrase break there
MIN_PHRASE = 1.0  # seconds; shorter phrases are merged into the next
EDGE_PAD = 0.25   # seconds kept before the first and after the last word, at most
UNREAD = 0.25     # mean confidence below which words outside the dua are not in the recording
EXTRA = 1.0       # recording beyond the words (seconds) that makes a range worth setting

HARAKAT = re.compile("[\u0610-\u061a\u064b-\u065f\u0670\u06d6-\u06ed\u0640]")
NORM = str.maketrans({"ٱ": "ا", "ی": "ي", "ک": "ك"})

REVIEW_FIELDS = ["id", "status", "reason", "mean", "min", "audio_range", "phrases", "arabic_marked",
                 "audio_segments", "arabic_sha1"]


def words_of(text: str) -> list[str]:
    # "،" ":" "؛" run straight into the next word in some entries ("حسد﴾،بسم"); they end a word.
    return [w for w in re.sub(r"([،:؛])(?=\w)", r"\1 ", text.replace("|", " ")).split() if w.strip()]


def letters(word: str) -> str:
    return "".join(c for c in HARAKAT.sub("", word.translate(NORM)) if "ء" <= c <= "ي" or c in "آأؤإئ")


def sha1(text: str) -> str:
    """Of the text's words, so phrase marks and spacing do not count as a change."""
    return hashlib.sha1(" ".join(words_of(text)).encode("utf-8")).hexdigest()[:12]


# ---------------------------------------------------------------- step 1: timings (model)

def compute_timings(ids: set[str]) -> dict:  # pragma: no cover - needs torch and the model
    import subprocess
    import numpy as np
    import torch
    import torchaudio.functional as F
    from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

    sr = 16000
    proc = Wav2Vec2Processor.from_pretrained(MODEL)
    model = Wav2Vec2ForCTC.from_pretrained(MODEL).eval()
    torch.set_num_threads(os.cpu_count() or 4)
    vocab = proc.tokenizer.get_vocab()
    _, rows, _ = sa.read_csv(sa.DUAS)
    data = json.loads(TIMINGS.read_text(encoding="utf-8")) if TIMINGS.exists() else {}
    cache: dict[str, tuple] = {}
    for row in rows:
        rid, file = row["id"], row.get("audio_file", "").strip()
        if not file or (ids and rid not in ids):
            continue
        text = row.get("arabic_full") or row["arabic"]
        if file not in cache:
            raw = subprocess.run(["ffmpeg", "-v", "quiet", "-i", str(sa.AUDIO_DIR / file), "-ac", "1", "-ar",
                                  str(sr), "-f", "f32le", "-"], capture_output=True, check=True).stdout
            wav = np.frombuffer(raw, dtype=np.float32).copy()
            step, chunks = sr * 30, []
            starts = list(range(0, len(wav), step))
            if len(starts) > 1 and len(wav) - starts[-1] < sr:
                starts.pop()
            with torch.inference_mode():
                for k, s in enumerate(starts):
                    e = starts[k + 1] if k + 1 < len(starts) else len(wav)
                    x = proc(wav[s:e], sampling_rate=sr, return_tensors="pt").input_values
                    chunks.append(torch.log_softmax(model(x).logits, dim=-1)[0])
            em = torch.cat(chunks)
            cache[file] = (em, len(wav) / sr / em.shape[0], len(wav) / sr)
        em, sec, duration = cache[file]
        words = words_of(text)
        toks, owner = [], []
        for wi, w in enumerate(words):
            t = [vocab[c] for c in letters(w) if c in vocab]
            if not t:
                continue
            if toks:
                toks.append(vocab["|"]); owner.append(-1)
            toks += t; owner += [wi] * len(t)
        timed: list = [None] * len(words)
        if toks and len(toks) <= em.shape[0]:
            labels, scores = F.forced_align(em.unsqueeze(0), torch.tensor([toks]), blank=0)
            spans, prev, ti = [], 0, -1
            for f, (lab, sc) in enumerate(zip(labels[0].tolist(), scores[0].exp().tolist())):
                if lab == 0:
                    prev = 0
                    continue
                if lab != prev or spans[-1][2] != f - 1:
                    ti += 1
                    spans.append([ti, f, f, [sc]])
                else:
                    spans[-1][2] = f; spans[-1][3].append(sc)
                prev = lab
            acc: dict[int, list] = {}
            for ti, a, b, sc in spans:
                if ti < len(owner) and owner[ti] >= 0:
                    r = acc.setdefault(owner[ti], [a, b, []])
                    r[0], r[1] = min(r[0], a), max(r[1], b)
                    r[2] += sc
            for wi, (a, b, sc) in acc.items():
                timed[wi] = [round(a * sec, 2), round((b + 1) * sec, 2), round(sum(sc) / len(sc), 2)]
        data[rid] = {"file": file, "duration": round(duration, 2), "text_sha1": sha1(text), "words": timed}
        print(rid, flush=True)
    TIMINGS.parent.mkdir(parents=True, exist_ok=True)
    TIMINGS.write_text(json.dumps(dict(sorted(data.items())), ensure_ascii=False, separators=(",", ":")) + "\n",
                       encoding="utf-8")
    return data


# ---------------------------------------------------------------- step 2: proposals (pure)

def locate(arabic: str, full: str) -> int | None:
    """Index in `full`'s words where `arabic`'s words start as one contiguous run, by letters."""
    a = [letters(w) for w in words_of(arabic)]
    f = [letters(w) for w in words_of(full)]
    a = [w for w in a if w]
    fi = [i for i, w in enumerate(f) if w]
    fl = [f[i] for i in fi]
    for s in range(len(fl) - len(a) + 1):
        if fl[s:s + len(a)] == a:
            return fi[s]
    return None


def propose(row: dict, entry: dict) -> dict:
    rid, arabic = row["id"], bc.split_phrases(row["arabic"])[0]
    full = row.get("arabic_full") or ""
    out = {"id": rid, "status": "fail", "reason": "", "mean": "", "min": "", "audio_range": "",
           "phrases": "", "arabic_marked": "", "audio_segments": "", "arabic_sha1": sa.sha1(row["arabic"])}
    if entry["text_sha1"] != sha1(full or row["arabic"]):
        out["reason"] = "text changed since the timings; run timings again"
        return out
    timed = entry["words"]
    words = words_of(arabic)
    start = 0
    if full:
        start = locate(arabic, full)
        if start is None:
            out["reason"] = "the dua is not one run of words of arabic_full"
            return out
    # map the row's words onto the timed words (skipping tokens with no letters, e.g. "-")
    span = [i for i in range(start, len(timed)) if letters(words_of(full or arabic)[i])][:len([w for w in words if letters(w)])]
    own = [timed[i] for i in span]
    if not own or any(t is None for t in own):
        out["reason"] = "some words could not be timed"
        return out
    scores = [t[2] for t in own]
    mean = sum(scores) / len(scores)
    out["mean"], out["min"] = f"{mean:.2f}", f"{min(scores):.2f}"
    # A clip boundary rests on the first and last word, a phrase break on the two words around it;
    # those must be sure. A single unsure word elsewhere moves nothing.
    first, last = span[0], span[-1]
    others_before = [t[2] for t in timed[:first] if t]
    others_after = [t[2] for t in timed[last + 1:] if t]
    # Words the recording does not read (a note it skips) are squeezed in at near-zero confidence:
    # then that side of the dua is the end of the file, and the dua's own edge words beside them
    # are dragged down too, so their score says nothing.
    unread_after = bool(others_after) and sum(others_after) / len(others_after) < UNREAD
    unread_before = bool(others_before) and sum(others_before) / len(others_before) < UNREAD
    sure_first = scores[0] >= MIN_EDGE or not others_before or unread_before
    sure_last = scores[-1] >= MIN_EDGE or not others_after or unread_after
    if unread_after or unread_before:
        own_sure = [t[2] for t in own[1 if unread_before else 0:len(own) - (3 if unread_after else 0)]]
        mean = sum(own_sure) / len(own_sure) if own_sure else 0.0
        out["mean"] = f"{mean:.2f}"
    # For a dhikr of three words or fewer an average says little; its two sure edges are enough.
    if (mean < MIN_MEAN and len(scores) > 3) or not (sure_first and sure_last):
        out["reason"] = f"low confidence (mean {mean:.2f}, edges {scores[0]:.2f}/{scores[-1]:.2f}): the recording may not match"
        return out
    # clip range
    before = 0.0 if unread_before else next((timed[i][1] for i in range(first - 1, -1, -1) if timed[i]), 0.0)
    after = entry["duration"] if unread_after else next((timed[i][0] for i in range(last + 1, len(timed)) if timed[i]), entry["duration"])
    if unread_after:  # the dua runs to the end of the recording
        timed = [*timed[:last], [timed[last][0], entry["duration"] - EDGE_PAD, timed[last][2]], *timed[last + 1:]]
    a = max(before, timed[first][0] - EDGE_PAD, (before + timed[first][0]) / 2 if first > 0 else 0.0)
    b = min(after, timed[last][1] + EDGE_PAD, (timed[last][1] + after) / 2 if after < entry["duration"] else entry["duration"])
    a, b = round(max(0.0, a), 2), round(b, 2)
    has_range = bool((row.get("audio_range") or "").strip())
    extra = a + (entry["duration"] - b)
    rng = f"{a:.2f}-{b:.2f}" if (has_range or full or extra > EXTRA) else ""
    seg_from, seg_to = (a, b) if rng else (0.0, entry["duration"])
    # phrase breaks: existing punctuation, with a pause at it
    fixed = "|" in row["arabic"]
    spaces = bc.split_phrases(row["arabic"])[1] if fixed else sa.break_candidates(arabic)
    if fixed:  # split_phrases gives where phrases start; the space is just before
        spaces = [b - 1 for b in spaces]
    # word index ending before each space
    ends_at, pos = {}, 0
    for wi, w in enumerate(arabic.split(" ")):
        pos += len(w)
        ends_at[pos] = wi
        pos += 1
    lettered = [i for i, w in enumerate(arabic.split(" ")) if letters(w)]
    to_timed = {wi: own[k] for k, wi in enumerate(lettered)}
    chosen, bounds, last_cut = [], [], seg_from
    for s in spaces:
        wi = ends_at.get(s)
        nxt = wi + 1 if wi is not None else None
        while nxt is not None and nxt not in to_timed and nxt < len(arabic.split(" ")):
            nxt += 1
        prv = wi
        while prv is not None and prv >= 0 and prv not in to_timed:
            prv -= 1
        if prv is None or prv < 0 or nxt not in to_timed:
            continue
        gap_a, gap_b = to_timed[prv][1], to_timed[nxt][0]
        sure = min(to_timed[prv][2], to_timed[nxt][2]) >= MIN_EDGE
        cut = round((gap_a + gap_b) / 2, 2)
        if fixed:
            if not sure:
                out["reason"] = "a hand-set phrase break sits on an unsure word"
                return out
        elif gap_b - gap_a < MIN_GAP or not sure or cut - last_cut < MIN_PHRASE:
            continue
        chosen.append(s); bounds.append(cut); last_cut = cut
    if bounds and not fixed and seg_to - bounds[-1] < MIN_PHRASE:
        chosen.pop(); bounds.pop()
    out["audio_range"] = rng
    if chosen:
        edges = [seg_from] + bounds + [seg_to]
        out["arabic_marked"] = row["arabic"] if fixed else sa.mark(arabic, chosen)
        out["audio_segments"] = "; ".join(f"{x:.2f}-{y:.2f}" for x, y in zip(edges, edges[1:]))
    out["phrases"] = str(len(chosen) + 1)
    out["status"] = "ok"
    return out


def timing_for(row: dict, data: dict, by_text: dict) -> dict | None:
    """The row's timings: by id, or (for a row split from an entry) by recording and entry text."""
    text = row.get("arabic_full") or row["arabic"]
    entry = data.get(row["id"])
    if entry and entry["text_sha1"] == sha1(text):
        return entry
    return by_text.get((row.get("audio_file", ""), sha1(text))) or entry


def suggest(ids: set[str] = frozenset()) -> list[dict]:
    data = json.loads(TIMINGS.read_text(encoding="utf-8"))
    by_text = {(e["file"], e["text_sha1"]): e for e in data.values()}
    _, rows, _ = sa.read_csv(sa.DUAS)
    results = []
    for r in rows:
        if not r.get("audio_file") or (ids and r["id"] not in ids):
            continue
        if (r.get("audio_segments") or "").strip():
            continue  # already has phrases (some listened to by the owner): left as they are
        entry = timing_for(r, data, by_text)
        if entry is None:
            results.append({**{k: "" for k in REVIEW_FIELDS}, "id": r["id"], "status": "fail",
                            "reason": "no timings; run timings", "arabic_sha1": sa.sha1(r["arabic"])})
            continue
        results.append(propose(r, entry))
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    sa.write_csv(OUT_DIR / "review.csv", REVIEW_FIELDS, results)
    (OUT_DIR / "review.html").write_text(review_html(results, rows), encoding="utf-8")
    return results


def apply(review: Path = OUT_DIR / "review.csv", duas: Path = sa.DUAS) -> tuple[list[str], list[str]]:
    _, reviewed, _ = sa.read_csv(review)
    fields, rows, newline = sa.read_csv(duas)
    by_id = {r["id"]: r for r in rows}
    applied, problems = [], []
    for r in reviewed:
        if r["status"] != "ok":
            continue
        row = by_id.get(r["id"])
        if row is None or sa.sha1(row["arabic"]) != r["arabic_sha1"]:
            problems.append(f"{r['id']}: Arabic changed since the proposal")
            continue
        new = dict(row)
        new["audio_range"] = r["audio_range"]
        new["arabic"] = r["arabic_marked"] or bc.split_phrases(row["arabic"])[0]
        new["audio_segments"] = r["audio_segments"]
        if new != row:
            row.update(new)
            applied.append(r["id"])
    if applied:
        sa.write_csv(duas, fields, rows, newline)
    return applied, problems


def review_html(results: list[dict], rows: list[dict]) -> str:
    by_id = {r["id"]: r for r in rows}
    audio_rel = os.path.relpath(sa.AUDIO_DIR, OUT_DIR)
    font_rel = os.path.relpath(sa.ARABIC_FONT, OUT_DIR)
    items = []
    for r in results:
        row = by_id[r["id"]]
        text = r["arabic_marked"] or row["arabic"]
        segs = [s.split("-") for s in r["audio_segments"].split("; ")] if r["audio_segments"] else []
        phrases = [p.strip() for p in text.split("|")]
        spans = "".join(
            f'<span class="ph" data-a="{s[0]}" data-b="{s[1]}">{html.escape(p)}</span> ' if i < len(segs) else
            f"<span>{html.escape(p)}</span> " for i, (p, s) in enumerate(zip(phrases, segs + [None] * len(phrases))))
        rng = r["audio_range"].split("-") if r["audio_range"] else ["0", "9999"]
        items.append(
            f'<section class="{r["status"]}"><h2>{r["id"]} <small>{r["status"]} {html.escape(r["reason"])} '
            f'mean {r["mean"]} min {r["min"]} range {r["audio_range"] or "-"}</small></h2>'
            f'<audio preload="none" src="{audio_rel}/{row["audio_file"]}"></audio>'
            f'<button data-a="{rng[0]}" data-b="{rng[1]}">play clip</button>'
            f'<p dir="rtl">{spans}</p></section>')
    return f"""<!doctype html><meta charset="utf-8"><title>Alignment review</title>
<style>@font-face{{font-family:N;src:url({font_rel})}}body{{font:15px system-ui;margin:24px;max-width:900px}}
p{{font:26px/2 N,serif}}.ph{{cursor:pointer;border-bottom:2px solid #0a6}}.ph:nth-of-type(2n){{border-color:#06a}}
.fail h2{{color:#a00}}small{{font-weight:400;color:#555}}</style>
{''.join(items)}
<script>
function play(sec, a, b) {{ const au = sec.querySelector('audio'); au.currentTime = +a; au.play();
  clearInterval(au.t); au.t = setInterval(() => {{ if (au.currentTime >= +b) {{ au.pause(); clearInterval(au.t); }} }}, 30); }}
document.querySelectorAll('[data-a]').forEach(el => el.onclick = () => play(el.closest('section'), el.dataset.a, el.dataset.b));
</script>"""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name in ("timings", "suggest"):
        sub.add_parser(name).add_argument("--id", action="append", default=[])
    sub.add_parser("apply")
    args = ap.parse_args()
    if args.cmd == "timings":
        compute_timings(set(args.id))
        return 0
    if args.cmd == "suggest":
        results = suggest(set(args.id))
        ok = sum(r["status"] == "ok" for r in results)
        print(f"{ok} of {len(results)} rows pass; review: {OUT_DIR / 'review.html'}")
        return 0
    applied, problems = apply()
    print(f"applied {len(applied)}: {' '.join(applied) or '-'}")
    for p in problems:
        print("problem:", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
