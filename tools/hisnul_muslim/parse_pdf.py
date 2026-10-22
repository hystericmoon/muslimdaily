"""Reads the Indonesian Hisnul Muslim PDF into numbered entries and footnotes.

The PDF (164 pages, Word 2016) lays out each entry as: Arabic block (font ~28pt, glyphs not
reliably extractable), Indonesian translation starting "NN. " (~17pt) with superscript
footnote markers (~11pt), and footnotes at the page bottom (~14pt text, ~9pt markers).
Arabic is dropped here; it is taken from hisnmuslim.com by entry number instead.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field

import pdfplumber

BODY, SUPER, NOTE, NOTE_MARK, HEADING = "body", "super", "note", "note_mark", "heading"
ENTRY_START = re.compile(r"^(\d{1,3})[.-]\s*(?=[^\s\d.])")
CHAPTER = re.compile(r"^(\d{1,3})-\s*(.+)$")


def is_arabic(text: str) -> bool:
    return any(
        "؀" <= c <= "ۿ" or "ݐ" <= c <= "ݿ" or "ﭐ" <= c <= "﷿" or "ﹰ" <= c <= "﻾"
        for c in text
    )


def kind(size: float) -> str | None:
    if 16 <= size <= 18.5:
        return BODY
    if 10 <= size <= 12:
        return SUPER
    if 13 <= size <= 15:
        return NOTE
    if 8 <= size <= 9.9:
        return NOTE_MARK
    if 18.5 < size < 26 or size > 30:
        return HEADING
    return None  # Arabic (~28pt) and page numbers (22pt) handled below


def is_heading(text: str) -> bool:
    """Chapter headings are numbered and all caps, e.g. "26- BACAAN DI WAKTU PAGI DAN SORE"."""
    m = CHAPTER.match(text)
    return bool(m) and text.upper() == text and len(m.group(2)) > 3


@dataclass
class Entry:
    number: int
    chapter: int | None
    chapter_title: str
    text: list[str] = field(default_factory=list)
    notes: list[int] = field(default_factory=list)
    page: int = 0
    printed_number: int | None = None  # set when the PDF prints a wrong number

    @property
    def translation(self) -> str:
        t = " ".join(self.text)
        t = ENTRY_START.sub("", t, count=1)
        return re.sub(r"\s+", " ", t).strip()


def _lines(words, tol=3.5):
    lines: list[list[dict]] = []
    for w in sorted(words, key=lambda w: (w["top"], w["x0"])):
        if lines and abs(lines[-1][0]["top"] - w["top"]) <= tol:
            lines[-1].append(w)
        else:
            lines.append([w])
    return [sorted(l, key=lambda w: w["x0"]) for l in lines]


APPENDIX_START = "HUKUM MENGGUNAKAN HADITS"
WEAK_START = "HADITS-HADITS LEMAH HISNUL MUSLIM"


def parse(path: str) -> tuple[list[Entry], dict[int, str], str]:
    """Returns (entries, footnotes by number, text of the closing appendix on weak hadith)."""
    entries: list[Entry] = []
    notes: dict[int, list[str]] = {}
    chapter, chapter_title = None, ""
    current: Entry | None = None
    last_note: int | None = None
    started = False
    appendix: list[str] = []
    in_appendix = False
    rejected: list[tuple] = []
    with pdfplumber.open(path) as pdf:
        for pno, page in enumerate(pdf.pages, start=1):
            words = page.extract_words(extra_attrs=["size"], keep_blank_chars=False, use_text_flow=False)
            by_kind: dict[str, list] = {}
            for w in words:
                if is_arabic(w["text"]):
                    continue
                k = kind(w["size"])
                if k is None:
                    continue
                by_kind.setdefault(k, []).append(w)
            # Page numbers (~22pt, digits only) are not content.
            heads = [w for w in by_kind.get(HEADING, []) if not w["text"].isdigit()]
            body_lines = _lines(by_kind.get(BODY, []) + heads)
            notes_text = by_kind.get(NOTE, [])
            smalls = [w for w in by_kind.get(SUPER, []) + by_kind.get(NOTE_MARK, []) if w["text"].isdigit()]

            def starts_note(w) -> bool:
                return any(abs(t["top"] - w["top"]) < 6 and 0 <= t["x0"] - w["x1"] < 30 for t in notes_text)

            note_marks = [w for w in smalls if starts_note(w)]
            supers = [w for w in smalls if not starts_note(w)]
            # Body flow with footnote markers placed on their nearest line.
            flow: list[tuple[float, float, str, str]] = []
            for line in body_lines:
                top = line[0]["top"]
                text = " ".join(w["text"] for w in line)
                flow.append((top, line[0]["x0"], "line", text))
            for s in supers:
                if not s["text"].isdigit():
                    continue
                near = min(body_lines, key=lambda l: abs(l[0]["top"] - s["top"]), default=None)
                top = near[0]["top"] if near and abs(near[0]["top"] - s["top"]) < 12 else s["top"]
                flow.append((top, s["x0"] + 10_000, "mark", s["text"]))  # after the line's text
            flow.sort(key=lambda f: (round(f[0]), f[1]))
            for _, _, what, text in flow:
                if in_appendix:
                    if what == "line":
                        appendix.append(text)
                    continue
                if what == "line" and len(entries) > 200 and APPENDIX_START in text.upper():
                    in_appendix, current = True, None
                    appendix.append(text)
                    continue
                if what == "mark":
                    if current is not None:
                        current.notes.append(int(text))
                    continue
                m = CHAPTER.match(text)
                if is_heading(text):
                    started = True
                    chapter, chapter_title = int(m.group(1)), m.group(2).strip(" .")
                    current = None
                    continue
                if not started:
                    continue
                m = ENTRY_START.match(text)
                if m:
                    n = int(m.group(1))
                    if not entries or n > entries[-1].number:
                        current = Entry(n, chapter, chapter_title, page=pno)
                        entries.append(current)
                    elif current is None and entries:
                        # Right after a chapter heading only a new entry can start: a wrong
                        # number here is a misprint (e.g. entry 9 printed as "5.").
                        current = Entry(entries[-1].number + 1, chapter, chapter_title, page=pno, printed_number=n)
                        entries.append(current)
                    elif current is not None and current.text:
                        # Possibly a misprinted entry number; resolved after parsing.
                        rejected.append((current, len(current.text), n, chapter, chapter_title, pno))
                if current is not None:
                    current.text.append(text)
            # Footnotes: a marker (~9pt) starts a note; following note lines continue it.
            note_ids = {id(w) for w in note_marks}
            for line in _lines(notes_text + note_marks):
                first = line[0]
                if id(first) in note_ids:
                    last_note = int(first["text"])
                    rest = line[1:]
                else:
                    rest = line
                if last_note is not None:
                    notes.setdefault(last_note, []).append(" ".join(w["text"] for w in rest))
    # A number missing from the sequence with exactly one stray "N." line inside the previous
    # entry is a misprint (e.g. entry 9 printed as "5."): split the previous entry there.
    present = {e.number for e in entries}
    for missing in sorted(set(range(1, entries[-1].number + 1)) - present):
        prev = next((e for e in entries if e.number == missing - 1), None)
        cands = [r for r in rejected if r[0] is prev]
        if prev is None or len(cands) != 1:
            continue
        _, cut, printed, ch, title, pno = cands[0]
        new = Entry(missing, ch, title, text=prev.text[cut:], page=pno, printed_number=printed)
        prev.text = prev.text[:cut]
        # Footnote markers cannot be split reliably; keep them on both (rare; only when the split is ambiguous).
        new.notes = list(prev.notes)
        entries.insert(entries.index(prev) + 1, new)
    # An entry printed without any number (e.g. 106) ends up appended to the previous entry,
    # which then has two quoted texts and two footnote markers: split at the quote boundary.
    present = {e.number for e in entries}
    for missing in sorted(set(range(1, entries[-1].number + 1)) - present):
        prev = next((e for e in entries if e.number == missing - 1), None)
        if prev is None or len(prev.notes) < 2:
            continue
        text = prev.translation
        cut = max(text.rfind("” “"), text.rfind("”“"))
        if cut <= 0:
            continue
        head, tail = text[: cut + 1], text[cut + 1 :].strip()
        prev.text, prev.notes, notes_tail = [f"{prev.number}. {head}"], prev.notes[:-1], prev.notes[-1:]
        new = Entry(missing, prev.chapter, prev.chapter_title, text=[tail], notes=notes_tail, page=prev.page)
        entries.insert(entries.index(prev) + 1, new)
    joined = {k: re.sub(r"\s+", " ", " ".join(v)).strip() for k, v in notes.items()}
    return entries, joined, re.sub(r"\s+", " ", " ".join(appendix)).strip()
