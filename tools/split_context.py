#!/usr/bin/env python3
"""Suggests which words of a Hisnul Muslim entry are the dua itself, for a person to approve.

    python3 -I tools/split_context.py suggest                 # every row that needs it
    python3 -I tools/split_context.py suggest --id eating_01  # or chosen rows
    python3 -I tools/split_context.py apply                   # writes approved rows to duas.csv

The book prints each entry as the narration: the hadith wording around the dua ("when one of
you eats, let him say: …"), notes in [ ] (another form, or when it is said), and repetition
notes ("three times"). The recitation by Hamad ad-Duraihim reads the same text. `suggest` finds
rows where the Arabic carries more than the words to recite and proposes:

  * `arabic`: the words to recite, only by leaving words out of the entry. Nothing is added,
    reordered or respelled; build_content.py checks that for every row.
  * `audio_range`: where those words are recited, from the pauses in the recording.
  * `translation_id`: the Indonesian inside the quotation marks, when the translation also
    carries the narration ("Rasulullah bersabda: …"). Left as it is otherwise.

The full entry is kept in `arabic_full` (and `translation_id_full`) and shown in the app under
the dua, so nothing from the book is lost. The result goes to:

    build/split/review.csv    one row per dua: proposal, rule, approved
    build/split/review.html   open in a browser: the dua part in colour, the rest grey; play the
                              proposed audio range or the whole recording

A reviewer listens, fixes `arabic`, `audio_range` or `translation_id` where needed and sets
`approved` to "ya". `apply` copies approved rows into duas.csv, refusing any row whose text
changed since the suggestion or whose dua part is not words of the entry in order. Run it
before segment_audio.py: phrases are then marked inside the dua part only.
"""
from __future__ import annotations

import argparse
import html
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_content as bc  # noqa: E402
import segment_audio as sa  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "build/split"

REVIEW_FIELDS = [
    "id", "event", "audio_file", "duration", "rule", "confidence", "approved",
    "arabic", "audio_range", "translation_id", "translation_en", "arabic_full", "translation_id_full", "source_sha1",
]

HARAKAT = re.compile(r"[ً-ٰٟـ]")

# Words that introduce what is said: "let him say", "say", "he said", "he would say". A dua
# follows them; the words before them are the narration.
CUES = {"فليقل", "وليقل", "فليقرأ", "فقولوا", "قولوا", "فقل", "وقل", "قال", "فقال", "وقال", "يقول", "فيقول", "ويقول"}
# A [ ] note that starts with one of these is an instruction (when, what else to say), not
# words of another narration that belong in the dua.
NOTE_STARTS = {"وإذا", "إذا", "واذا", "اذا", "قال", "وقال", "ثم", "يقول", "ويقول", "وفي", "في", "يقرأ", "ويقرأ"}
# Repetition notes: "three times", "seven", "a hundred times", "thirty-three".
COUNT_WORDS = {"ثلاثا", "ثلاث", "ثلاثة", "سبعا", "سبع", "عشرا", "عشر", "مائة", "مئة", "مرات", "مرة",
               "مرتين", "مرتان", "سبعين", "وثلاثين", "أربعا", "اربعا", "وثلاثون"}


def bare(token: str) -> str:
    """A token without harakat and the brackets/punctuation around it, for matching rules."""
    return HARAKAT.sub("", token).strip(bc.FRAME_CHARS + "ـ-")


def groups(tokens: list[str], open_: str, close: str) -> list[tuple[int, int]]:
    """Spans (first, last token) of single-bracket groups such as "(ثلاثا)" or "[وإذا أمسى …]".
    A double "((" opens the dua quote, not a group."""
    out, i = [], 0
    while i < len(tokens):
        t = tokens[i].lstrip(open_ * 2) if tokens[i].startswith(open_ * 2) else tokens[i]
        if t.startswith(open_) or (open_ in tokens[i] and not tokens[i].startswith(open_ * 2) and tokens[i].index(open_) > 0):
            j = i
            depth = tokens[i].count(open_) - tokens[i].count(close)
            while depth > 0 and j + 1 < len(tokens):
                j += 1
                depth += tokens[j].count(open_) - tokens[j].count(close)
            out.append((i, j))
            i = j + 1
        else:
            i += 1
    return out


def propose_arabic(text: str) -> tuple[str, list[str]]:
    """(words to recite, rules applied). Returns the text unchanged with no rules when it looks clean.

    Only leaves words out. Errs towards keeping words: a proposal a reviewer has to shorten is
    safer than one that silently loses part of a dua."""
    tokens = text.replace(bc.PHRASE_MARK, " ").split()
    keep = [True] * len(tokens)
    rules: list[str] = []
    drop = lambda a, b: [keep.__setitem__(k, False) for k in range(a, b + 1)]  # noqa: E731

    # [ ] notes that are instructions (another narration's words in [ ] stay).
    for a, b in groups(tokens, "[", "]"):
        if bare(tokens[a]) in NOTE_STARTS:
            drop(a, b)
            rules.append("note")

    # ( ) notes with a count: "(ثلاثا)", "(مائة مرة في اليوم)", "(ثلاث مرات إذا أمسى)".
    for a, b in groups(tokens, "(", ")"):
        if any(bare(t) in COUNT_WORDS for t in tokens[a:b + 1]) and b - a <= 6:
            drop(a, b)
            if "count" not in rules:
                rules.append("count")

    # A count after the dua's closing "))": "ثلاث مرات." to the end.
    closing = max((i for i, t in enumerate(tokens) if "))" in t), default=None)
    if closing is not None and closing + 1 < len(tokens):
        tail = tokens[closing + 1:]
        if bare(tail[0]) in COUNT_WORDS and len(tail) <= 3:
            drop(closing + 1, len(tokens) - 1)
            if "count" not in rules:
                rules.append("count")

    # Narration before the dua: a cue word ("let him say", "and he says:") ahead of the quoted
    # dua, or, with no quote, in the first part of the entry. Several cues (one per thing said)
    # keep each piece, from after the cue (and after "…:" naming who says it) to its comma.
    quote = next((i for i, t in enumerate(tokens) if t.startswith("((") or t.startswith("«")), None)
    limit = quote if quote not in (None, 0) else (len(tokens) * 2 // 5 if quote is None else 0)
    cues = [i for i, t in enumerate(tokens) if keep[i] and bare(t) in CUES]
    lead = [c for c in cues if c < limit] if limit else []
    if not lead and quote == 0 and tokens[0].startswith("(("):
        # The whole entry is the hadith in the book's quote marks: "((when one of you … let him say …))".
        first_cue = next((c for c in cues if c < len(tokens) * 2 // 5), None)
        if first_cue is not None and bare(tokens[0]) not in {"اللهم", "بسم", "سبحان", "الحمد", "لا", "أعوذ", "استغفر", "أستغفر"}:
            lead = [first_cue]
    if lead:
        rules.append("narration")
        later = [c for c in cues if c > lead[0]] if len(lead) == 1 and quote in (None, 0) else []
        pieces = [lead[0]] + later
        for n, c in enumerate(pieces):
            start = c + 1
            colon = next((k for k in range(c, min(c + 8, len(tokens))) if tokens[k].endswith(":") or tokens[k].endswith(":((")), None)
            if colon is not None:
                start = colon + 1
            drop(0 if n == 0 else pieces[n - 1] + 1, start - 1) if n == 0 else None
            if n > 0:
                prev_end = next((k for k in range(pieces[n - 1] + 1, c) if tokens[k].endswith("،")), None)
                if prev_end is not None:
                    drop(prev_end + 1, start - 1)
        if len(pieces) > 1:
            rules.append("several")

    if not rules:
        return text, []
    kept = " ".join(t for t, k in zip(tokens, keep) if k)
    return tidy(kept), rules


def tidy(text: str) -> str:
    """Drops brackets left unpaired at either end once words around them are left out."""
    opens, closes = "([«", ")]»"
    count = lambda cs: sum(text.count(c) for c in cs)  # noqa: E731
    while text and text[0] in opens and count(opens) > count(closes):
        text = text[1:].lstrip()
    while text and text[-1] in closes and count(closes) > count(opens):
        text = text[:-1].rstrip()
    return text.strip(" .،:")



QUOTES = re.compile(r"[“\"]([^”\"]+)[”\"]")


def propose_translation(text: str) -> str:
    """The quoted part of a translation that also carries narration; the text itself otherwise."""
    quoted = [q.strip() for q in QUOTES.findall(text)]
    outside = QUOTES.sub(" ", text)
    outside_words = [w for w in outside.split() if re.search(r"[A-Za-z]", w)]
    if not quoted or len(outside_words) < 2:
        return text
    return " ".join(quoted)


def share_before(full: str, part: str) -> tuple[float, float]:
    """Where [part] starts and ends in [full], as shares of its letters (0..1)."""
    letters = lambda s: len(re.sub(r"[^ء-ي]", "", HARAKAT.sub("", s)))  # noqa: E731
    total = max(letters(full), 1)
    words = full.split()
    target = bc.words(part)
    # Find the first and last word of the part in order.
    norm = [w.strip(bc.FRAME_CHARS) for w in words]
    try:
        first = norm.index(target[0])
        last = len(norm) - 1 - norm[::-1].index(target[-1])
    except (ValueError, IndexError):
        return 0.0, 1.0
    before = letters(" ".join(words[:first]))
    through = letters(" ".join(words[:last + 1]))
    return before / total, through / total


def propose_range(full: str, part: str, samples: list[float]) -> str:
    """The proposed audio range: the letter shares mapped onto the speech, snapped to pauses."""
    start, end, pauses = sa.find_pauses(sa.loudness(samples))
    if end <= start:
        return ""
    a, b = share_before(full, part)
    if a <= 0.01 and b >= 0.99:
        return ""
    span = end - start
    snap = lambda t: min((p.mid for p in pauses), key=lambda m: abs(m - t), default=t)  # noqa: E731
    t0 = start if a <= 0.01 else snap(start + a * span)
    t1 = end if b >= 0.99 else snap(start + b * span)
    if abs(t0 - (start + a * span)) > 0.25 * span:
        t0 = start + a * span
    if abs(t1 - (start + b * span)) > 0.25 * span:
        t1 = start + b * span
    t0 = max(0.0, t0 - 0.05)
    t1 = min(len(samples) / sa.SAMPLE_RATE, t1 + 0.05)
    return f"{t0:.2f}-{t1:.2f}" if t1 > t0 else ""


# Words that, left in a proposal, mean it is still a hadith about the dhikr rather than the
# dhikr itself ("whoever says …", "when …", the Prophet's name ﷺ), or a dialogue.
STILL_NARRATION = {"يقول", "من", "إذا", "اذا", "وإذا", "فإذا", "كان", "كنا", "صلى", "النبي", "رسول", "فقال", "فقلت", "قال", "فسأله"}


# An intro naming the Prophet ﷺ or a Companion means the quote is a hadith: often about a dhikr's
# virtue, so which words are the dhikr is the reviewer's call.
ATTRIBUTION = {"صلى", "النبي", "رسول", "عن", "جابر", "جاء"}


def confidence(part: str, rules: list[str], full: str = "") -> str:
    """"likely" when the proposal looks like a dua alone; "check" when a person must decide."""
    kept = set(part.split())
    if "narration" in rules and any(bare(t) in ATTRIBUTION for t in full.split() if t not in kept):
        return "check"
    if "several" in rules:
        return "check"
    if any(bare(t) in STILL_NARRATION for t in part.split()):
        return "check"
    return "likely"


def source_sha1(row: dict) -> str:
    return sa.sha1(row["arabic"] + "\n" + row.get("translation_id", ""))


def suggest_row(row: dict, samples: list[float] | None) -> dict | None:
    if (row.get("arabic_full") or "").strip():
        return None
    full = row["arabic"]
    part, rules = propose_arabic(full)
    if not rules or not part or not bc.is_part_of(part, full):
        return None
    tr = row.get("translation_id", "")
    tr_part = propose_translation(tr)
    duration = len(samples) / sa.SAMPLE_RATE if samples else 0.0
    return {
        "id": row["id"], "event": row["event"], "audio_file": row["audio_file"],
        "duration": f"{duration:.2f}" if samples else "", "rule": "+".join(rules),
        "confidence": confidence(part, rules, full), "approved": "",
        "arabic": part,
        "audio_range": propose_range(full, part, samples) if samples else "",
        "translation_id": tr_part if tr_part != tr else "",
        "translation_en": "",
        "arabic_full": full, "translation_id_full": tr if tr_part != tr else "",
        "source_sha1": source_sha1(row),
    }


def review_html(results: list[dict], audio_rel: str, font_rel: str) -> str:
    cards = []
    for r in results:
        part_words = set(bc.words(r["arabic"]))
        full = " ".join(
            f'<b>{html.escape(t)}</b>' if t.strip(bc.FRAME_CHARS) in part_words else f'<i>{html.escape(t)}</i>'
            for t in r["arabic_full"].split()
        )
        rng = r["audio_range"]
        a, b = (bc.parse_segments(rng)[0] if rng else (0, 0))
        player = (
            f'<audio controls preload="none" src="{audio_rel}/{html.escape(r["audio_file"])}"></audio>'
            + (f'<button data-a="{a}" data-b="{b}">Play dua part {a:.2f}–{b:.2f}</button>' if rng else "")
        ) if r["audio_file"] else "<p>No audio.</p>"
        tr = (
            f'<p class="tr"><b>{html.escape(r["translation_id"])}</b><br><i>{html.escape(r["translation_id_full"])}</i></p>'
            if r["translation_id"] else ""
        )
        cards.append(
            f'<section class="{r["confidence"]}"><h2>{html.escape(r["id"])} <small>{html.escape(r["rule"])} · '
            f'{"likely right" if r["confidence"] == "likely" else "needs your decision"}</small></h2>'
            f'<p class="ar" lang="ar" dir="rtl">{full}</p>'
            f'<p class="ar part" lang="ar" dir="rtl">{html.escape(r["arabic"])}</p>{tr}{player}</section>'
        )
    return f"""<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dua and context review</title>
<style>
@font-face{{font-family:"Noto Naskh Arabic";src:url("{font_rel}")}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0 auto;max-width:56rem;padding:1rem;background:#fbf8f1;color:#1c1c18}}
section{{border-top:1px solid #6f7976;padding:1rem 0}} small{{font-weight:400;color:#3f4947}}
.ar{{font-family:"Noto Naskh Arabic",serif;font-size:1.5rem;line-height:2.2;text-align:right;margin:.25rem 0}}
.ar b{{font-weight:400;color:#00504c}} .ar i,.tr i{{font-style:normal;color:#6f7976}}
.part{{background:#e6f2f0;padding:.25rem .75rem;border-radius:.5rem}}
.tr b{{font-weight:400}} section.check h2::before{{content:"⚑ ";color:#8a4b00}}
button{{min-height:48px;font:inherit;margin-left:.5rem}}
</style>
<h1>Dua and context review</h1>
<p>Each entry shows the book's text with the proposed dua in green and the context in grey, then
the dua part alone. Entries marked ⚑ need your decision: most are hadiths about a dhikr's virtue,
where only you can say which words are the dhikr to recite, or whether the entry should stay whole. Play the dua part to check the recording starts and ends on it. Approve rows
in <code>review.csv</code> (fix <code>arabic</code>, <code>audio_range</code> or
<code>translation_id</code> there if needed), then run <code>split_context.py apply</code>.</p>
{''.join(cards)}
<script>
document.querySelectorAll('section').forEach(s => {{
  const audio = s.querySelector('audio'); const b = s.querySelector('button'); let stopAt = null;
  if (!audio || !b) return;
  audio.addEventListener('timeupdate', () => {{ if (stopAt !== null && audio.currentTime >= stopAt) {{ audio.pause(); stopAt = null; }} }});
  b.addEventListener('click', () => {{ audio.currentTime = +b.dataset.a; stopAt = +b.dataset.b; audio.play(); }});
}});
</script></html>
"""


def suggest(ids: set[str], duas: Path = sa.DUAS, audio_dir: Path = sa.AUDIO_DIR, out_dir: Path = OUT_DIR,
            decoder=sa.decode, font: Path = sa.ARABIC_FONT) -> list[dict]:
    _, rows, _ = sa.read_csv(duas)
    results = []
    for row in rows:
        if ids and row["id"] not in ids:
            continue
        if row.get("is_quran", "").strip().lower() in bc.YES:
            continue
        audio = audio_dir / row["audio_file"] if row.get("audio_file") else None
        r = suggest_row(row, decoder(audio) if audio and audio.exists() else None)
        if r:
            results.append(r)
    sa.write_csv(out_dir / "review.csv", REVIEW_FIELDS, results)
    rel = Path(os.path.relpath(audio_dir, out_dir)).as_posix()
    font_rel = Path(os.path.relpath(font, out_dir)).as_posix()
    (out_dir / "review.html").write_text(review_html(results, rel, font_rel), encoding="utf-8")
    return results


def apply(review: Path = OUT_DIR / "review.csv", duas: Path = sa.DUAS) -> tuple[list[str], list[str]]:
    """Copies approved rows into duas.csv. Returns (applied ids, problems)."""
    _, reviewed, _ = sa.read_csv(review)
    fields, rows, newline = sa.read_csv(duas)
    for column in ("arabic_full", "translation_id_full", "translation_en_full", "audio_range"):
        if column not in fields:
            fields.append(column)
            for row in rows:
                row[column] = ""
    by_id = {r["id"]: r for r in rows}
    applied, problems = [], []
    for r in reviewed:
        if (r.get("approved") or "").strip().lower() not in bc.YES:
            continue
        target = by_id.get(r["id"])
        if target is None:
            problems.append(f"{r['id']}: not in duas.csv")
            continue
        if source_sha1(target) != r["source_sha1"]:
            problems.append(f"{r['id']}: text changed since the suggestion; run suggest again")
            continue
        if not bc.is_part_of(r["arabic"], target["arabic"]):
            problems.append(f"{r['id']}: arabic must be words of the entry, in order")
            continue
        if r["translation_id"] and not bc.is_part_of(r["translation_id"], target["translation_id"]):
            problems.append(f"{r['id']}: translation_id must be words of the full translation, in order")
            continue
        if r.get("translation_en") and not bc.text_part_of(r["translation_en"], target["translation_en"]):
            problems.append(f"{r['id']}: translation_en must be words of the full translation, in order")
            continue
        if r["audio_range"]:
            try:
                ok = len(bc.parse_segments(r["audio_range"])) == 1
            except ValueError:
                ok = False
            if not ok:
                problems.append(f"{r['id']}: audio_range must look like '3.20-9.80'")
                continue
        target["arabic_full"] = target["arabic"]
        target["arabic"] = r["arabic"]
        target["audio_range"] = r["audio_range"]
        if r["translation_id"]:
            target["translation_id_full"] = target["translation_id"]
            target["translation_id"] = r["translation_id"]
        if r.get("translation_en"):
            target["translation_en_full"] = target["translation_en"]
            target["translation_en"] = r["translation_en"]
        applied.append(r["id"])
    if applied:
        sa.write_csv(duas, fields, rows, newline)
    return applied, problems


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("suggest")
    s.add_argument("--id", action="append", default=[])
    sub.add_parser("apply")
    args = ap.parse_args()
    if args.cmd == "suggest":
        results = suggest(set(args.id))
        print(f"{len(results)} row(s) to review: {OUT_DIR / 'review.html'}")
        return 0
    applied, problems = apply()
    for p in problems:
        print("skipped", p)
    print(f"applied {len(applied)} row(s); now run tools/build_content.py")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
