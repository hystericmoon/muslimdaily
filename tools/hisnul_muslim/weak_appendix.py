"""Finds which entries the PDF's appendix ("Hadits-hadits lemah Hisnul Muslim") calls weak.

The appendix quotes each weak narration in translation followed by its takhrij and
al-Albani's verdict. Quotes are matched to entry translations by text similarity; matches
are best-effort and restricted to the section's own chapter, so a quote cannot match elsewhere.
"""
from __future__ import annotations

import difflib
import re

QUOTE = re.compile(r"[“\"]([^”\"]{25,}?)[”\"]\s*(\(dibaca[^)]*\))?\s*\(HR\.([^)]*)\)\s*(.*?)(?=(?:Dan juga hadits|\d+\.\s+D|$|[“\"]))", re.S)


def _norm(t: str) -> str:
    t = t.lower()
    t = re.sub(r"[^a-z\s]", " ", t)
    return re.sub(r"\s+", " ", t).strip()


# Appendix sections and the chapter (by words in the PDF chapter title) each one refers to.
SECTIONS = [
    ("Masuk Rumah", "MASUK RUMAH"),
    ("Istiftah", "ISTIFTAH"),
    ("Pagi dan Sore", "PAGI DAN SORE"),
    ("Ketika Tidur", "TIDUR"),
]


def items(appendix: str) -> list[dict]:
    start = appendix.upper().find("HADITS-HADITS LEMAH HISNUL MUSLIM")
    text = appendix[start:] if start >= 0 else ""
    # Section boundaries: "1. Do'a Ketika Masuk Rumah.", "2. Do'a Al-Istiftah." ...
    bounds = []
    for label, chapter_key in SECTIONS:
        i = text.find(label)
        if i >= 0:
            bounds.append((i, chapter_key))
    bounds.sort()

    def section_at(pos: int) -> str | None:
        key = None
        for i, k in bounds:
            if i <= pos:
                key = k
        return key

    out = []
    last_end = 0
    for m in QUOTE.finditer(text):
        # Text since the previous item belongs to this one too (e.g. a narration quoted in two
        # parts before its takhrij), unless a new section starts in between.
        lead = text[last_end:m.start()]
        lead = lead.split("hadits:")[-1]
        lead = re.split(r"\d+\.\s+(?:Do|Dz)[^.]*\.", lead)[-1]
        last_end = m.end()
        verdict = re.sub(r"\s+", " ", m.group(4)).strip()
        out.append({
            "section": section_at(m.start()),
            "weak": "didla" in verdict.lower() or "maudlu" in verdict.lower(),
            "quote": (lead.strip(" “”\"") + " " + m.group(1)).strip(),
            "count": (m.group(2) or "").strip(),
            "takhrij": "HR." + m.group(3).strip(),
            "verdict": verdict[:400],
        })
    return out


def match(
    appendix_items: list[dict],
    translations: dict[int, str],
    chapters: dict[int, str],
    threshold: float = 0.4,
) -> dict[int, dict]:
    """Returns {entry number: appendix item}: the best match for each quote within its section's
    chapter, above threshold."""
    normed = {n: _norm(t) for n, t in translations.items()}
    found: dict[int, dict] = {}
    for it in appendix_items:
        q = _norm(it["quote"])
        best, score = None, 0.0
        for n, t in normed.items():
            if it["section"] and it["section"] not in chapters.get(n, ""):
                continue
            s = difflib.SequenceMatcher(None, q, t[: len(q) + 40]).ratio()
            if s > score:
                best, score = n, s
        if best is not None and score >= threshold:
            found[best] = {**it, "score": round(score, 2)}
    return found
