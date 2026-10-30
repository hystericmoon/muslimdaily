#!/usr/bin/env python3
"""Indonesian-style Latin for fully vowelled Arabic, as a reading aid.

    python3 -I tools/transliterate.py "بِسْمِ اللَّهِ"      # prints: Bismillāh

The owner allowed this one exception to the sourced-content rule (2026-10-29): the Latin is not
taken from a source but derived, by these fixed rules only, from the sourced Arabic. It follows
the Indonesian standard (SKB Menteri Agama dan Mendikbud 1987, as commonly printed without dots
under letters): ث ts, ج j, ح h, خ kh, ذ dz, ش sy, ص sh, ض dh, ط th, ظ zh, ع and ء ', غ gh;
long vowels ā ī ū; diphthongs ai and au; the article before a sun letter assimilates (ar-rahmān);
an alif of joining (hamzat waṣl) is not read after a word (rabbil-'ālamīn); words end in their
pausal form before punctuation and at the end (ة becomes h, a final short vowel drops, -an
becomes ā). Text the rules can't read (a word with no vowel marks, such as the book's notes)
makes `transliterate` return None rather than a guess.
"""
from __future__ import annotations

import re
import sys
import unicodedata

FATHA, KASRA, DAMMA = "َ", "ِ", "ُ"
FATHATAN, KASRATAN, DAMMATAN = "ً", "ٍ", "ٌ"
SUKUN, SHADDA = "ْ", "ّ"
DAGGER_ALIF, MADDAH, HAMZA_ABOVE = "ٰ", "ٓ", "ٔ"
SMALL_WAW, SMALL_YEH, SMALL_HIGH_YEH = "ۥ", "ۦ", "ۧ"
SILENT = "۟"  # small high rounded zero: the letter is not read
TATWEEL = "ـ"

CONSONANTS = {
    "ب": "b", "ت": "t", "ث": "ts", "ج": "j", "ح": "h", "خ": "kh", "د": "d", "ذ": "dz",
    "ر": "r", "ز": "z", "س": "s", "ش": "sy", "ص": "sh", "ض": "dh", "ط": "th", "ظ": "zh",
    "ع": "'", "غ": "gh", "ف": "f", "ق": "q", "ك": "k", "ل": "l", "م": "m", "ن": "n",
    "ه": "h", "و": "w", "ي": "y", "ة": "t",
}
HAMZAS = set("أإؤئء")
ALIFS = set("اٱى")
SUN = set("تثدذرزسشصضطظلن")
LETTERS = set(CONSONANTS) | HAMZAS | ALIFS | {"آ"}
VOWELS = {FATHA: "a", KASRA: "i", DAMMA: "u", FATHATAN: "an", KASRATAN: "in", DAMMATAN: "un"}
# Punctuation where a reader pauses: the word before it takes its pausal form.
PAUSE = {"،": ",", ".": ".", "؛": ";", ":": ":", "؟": "?", "!": "!", "۝": ""}
KEEP = {"[": "[", "]": "]", "–": "–", "-": "-"}
IGNORE = set("﴿﴾()«»\"'") | {"۞"}
# The book often leaves out the small alif in these words; its letter (by index in the bare
# skeleton) is read long. Skeletons are matched inside a word, so prefixes (و، ف، ب، ل) work.
LONG_IN = {"إله": 1, "اله": 1, "رحمن": 2, "هذا": 0, "هذه": 0, "ذلك": 0, "لكن": 0, "أولئك": 2, "سموات": 1}
PREFIXES = set("وفبلك")


class Token:
    def __init__(self, letter: str):
        self.letter = letter
        self.marks: set[str] = set()

    @property
    def vowel(self) -> str:
        for m, v in VOWELS.items():
            if m in self.marks:
                return v
        return ""

    def has(self, mark: str) -> bool:
        return mark in self.marks


def tokenize(text: str) -> list:
    """Words (lists of Tokens) and separators (strings), in order."""
    out: list = []
    word: list[Token] = []
    for ch in unicodedata.normalize("NFC", text):
        if ch == TATWEEL:
            word.append(Token(TATWEEL))
            continue
        if ch == HAMZA_ABOVE and word and word[-1].letter == TATWEEL:
            word[-1].letter = "ء"
            continue
        if ch in LETTERS:
            word.append(Token(ch))
        elif unicodedata.combining(ch) or ch in (SMALL_WAW, SMALL_YEH):
            if word:
                word[-1].marks.add(ch)
        else:
            if word:
                out.append(word)
                word = []
            if ch.isdigit():
                continue
            out.append(ch)
    if word:
        out.append(word)
    # A tatweel with no hamza only stretches the line: its marks belong to the letter before.
    for k, item in enumerate(out):
        if isinstance(item, list):
            kept: list[Token] = []
            for t in item:
                if t.letter == TATWEEL:
                    if kept:
                        kept[-1].marks |= t.marks
                    continue
                kept.append(t)
            out[k] = kept
    # The book often puts the fatha (and the shadda) of لا on the alif: they belong to the lam.
    for item in out:
        if isinstance(item, list):
            for a, b in zip(item, item[1:]):
                if b.letter == "ا" and (b.marks & {FATHA, SHADDA}) and not a.vowel and not a.has(SUKUN):
                    a.marks |= b.marks & {FATHA, SHADDA}
                    b.marks -= {FATHA, SHADDA}
    # The name Allah is often printed without the marks on its doubled lam (اللهِ، اللهُمَّ، لِلهِ):
    # it is always read as the full form اللَّه.
    for item in out:
        if isinstance(item, list):
            m = re.fullmatch(r"([وفب]?)(ال|ٱل|ل)لهم?", skeleton(item))
            if m:
                lam = item[len(m.group(1)) + len(m.group(2))]
                if not lam.marks:
                    lam.marks |= {SHADDA, FATHA}
    return out


def skeleton(word: list[Token]) -> str:
    return "".join(t.letter for t in word)


READ_MARKS = set(VOWELS) | {SUKUN, SHADDA, DAGGER_ALIF, MADDAH, SMALL_WAW, SMALL_YEH, SMALL_HIGH_YEH, SILENT}
LONG_LETTERS = set("اىوي") | {"ٱ"}


def vowelled(word: list[Token]) -> bool:
    """A word the rules can read: every consonant carries a mark, except at the end of the word,
    before a letter of length (ما, أعوذ) and the lam of the article."""
    for idx, t in enumerate(word):
        # A doubled letter whose vowel isn't written (the book's السّماء) can't be read, unless an
        # alif follows: that is only read after a fatha (القهّار).
        if (t.has(SHADDA) and not t.vowel and idx < len(word) - 1 and t.letter in CONSONANTS
                and word[idx + 1].letter != "ا"):
            return False
        if t.marks & READ_MARKS or t.letter not in CONSONANTS:
            continue
        if idx == len(word) - 1 or word[idx + 1].letter in LONG_LETTERS:
            continue
        if t.letter in "وي" and idx and word[idx - 1].vowel:
            continue
        if t.letter == "ل" and idx and word[idx - 1].letter in "اٱ":
            continue
        if t.letter == "ل" and idx + 1 < len(word) and word[idx + 1].letter == "ل" and word[idx + 1].has(SHADDA):
            continue  # the book's للَّهِ
        # A bare letter between two vowelled ones closes the syllable: an unwritten sukun
        # (أَنتَ، يَرحَمُكَ). A bare و or ي after a bare letter is its long vowel (وَأَتوبُ).
        nxt = word[idx + 1]
        if idx and word[idx - 1].vowel and (nxt.vowel or nxt.has(SHADDA)):
            continue
        if t.letter in "وي" and idx:
            continue
        if idx and word[idx - 1].letter in "اٱ" and not word[idx - 1].vowel:
            continue  # after an alif of joining (وَٱنصُرْنَا): an unwritten sukun
        return False
    return True


def has_article(word: list[Token], start: int) -> bool:
    """ال at [start] followed by a letter: the article (not the lam of a root)."""
    return (
        start + 2 < len(word)
        and word[start].letter in "اٱ"
        and word[start + 1].letter == "ل"
        and not word[start].vowel
        and (word[start + 1].has(SUKUN) or not word[start + 1].vowel or word[start + 2].has(SHADDA))
    )


def is_jalalah(word: list[Token], i: int) -> bool:
    """The doubled lam of Allah: لّ before a final ه, after another lam."""
    return (
        word[i].letter == "ل" and word[i].has(SHADDA) and i + 1 < len(word)
        and word[i + 1].letter == "ه" and i >= 1 and word[i - 1].letter == "ل"
    )


def render(word: list[Token], at_pause: bool, joined: bool) -> str:
    """One word in Latin. joined: an alif of joining at the start is not read."""
    long_at = set()
    sk = skeleton(word)
    for pattern, offset in LONG_IN.items():
        for m in re.finditer(re.escape(pattern), sk):
            long_at.add(m.start() + offset)

    out: list[str] = []
    i = 0
    n = len(word)
    while i < n:
        t = word[i]
        prev = word[i - 1] if i else None
        last = i == n - 1

        if t.has(SILENT):
            i += 1
            continue

        # The article: al-, or assimilated before a sun letter (ar-, as-, ...).
        if t.letter in "اٱ" and has_article(word, i) and (i == 0 or prev.letter in PREFIXES):
            nxt = word[i + 2]
            if i == 0 and not joined:
                out.append("a")
            if is_jalalah(word, i + 2):
                nxt.marks.add("jalalah")
                i += 2
                continue
            if nxt.letter in SUN:
                out.append(CONSONANTS[nxt.letter] + "-")
                nxt.marks.discard(SHADDA)
            else:
                out.append("l-")
            i += 2
            continue

        # ٱ is always an alif of joining; so is ا after a one-letter prefix before a vowelless
        # letter (فَاغْفِرْ): neither is read inside a word.
        if t.letter == "ٱ" and i > 0:
            i += 1
            continue
        if (t.letter == "ا" and i == 1 and prev.letter in PREFIXES and prev.vowel and i + 1 < n
                and (word[i + 1].has(SUKUN) or not word[i + 1].vowel) and not t.marks):
            i += 1
            continue
        # ى reads ā (or ī after a kasra); with a shadda it is the consonant y.
        if t.letter == "ى" and not t.has(SHADDA):
            before = out[-1] if out else ""
            if before.endswith("i") and not t.has(DAGGER_ALIF):
                out[-1] = before[:-1] + "ī"
            elif before.endswith("a"):
                out[-1] = before[:-1] + "ā"
            elif before and not before.endswith(("ā", "ī", "ū")):
                out[-1] = before + "ā"
            i += 1
            continue
        if t.letter == "ى":
            t.letter = "ي"
        # A madd sign on an alif after a fatha is only length (رَبَّنَآ).
        if t.letter == "آ" and i > 0 and out and out[-1].endswith("a"):
            out[-1] = out[-1][:-1] + "ā"
            i += 1
            continue
        # Alif: lengthens a preceding fatha, is silent after -an or the plural -ū, and at the
        # start of a word is an alif of joining (read only when nothing comes before it).
        if t.letter in ALIFS and not t.has(MADDAH) and not t.vowel:
            if i == 0:
                if not joined:
                    third = word[2] if n > 2 else None
                    out.append("u" if third is not None and third.vowel == "u" else "i")
            elif prev is not None and prev.has(FATHATAN):
                if last and at_pause and out and out[-1].endswith("an"):
                    out[-1] = out[-1][:-2] + "ā"  # the pausal form: خَيْرًا -> khairā
            elif out and out[-1].endswith("a") and not prev.has(SUKUN):
                out[-1] = out[-1][:-1] + "ā"
            elif out and out[-1].endswith("ū"):
                pass
            elif out and prev is not None and not prev.vowel and not prev.has(SUKUN):
                out[-1] = out[-1] + "ā"
            i += 1
            continue
        if t.letter == "آ" or (t.letter in ALIFS and t.has(MADDAH)):
            out.append(("" if i == 0 else "'") + "ā")
            i += 1
            continue

        # Long vowels and diphthongs written with و and ي.
        if t.letter in "وي" and not t.vowel and not t.has(SHADDA):
            before = out[-1] if out else ""
            if prev is not None and not prev.vowel and not prev.has(SUKUN) and before and before[-1] not in "aiuāīū-" and t.letter in "وي" and i > 0 and not (i + 1 < n and word[i + 1].letter in "اى"):
                out[-1] = before + ("ū" if t.letter == "و" else "ī")
                i += 1
                continue
            if t.letter == "و" and before.endswith("u"):
                out[-1] = before[:-1] + "ū"
                i += 1
                continue
            if t.letter == "ي" and before.endswith("i"):
                out[-1] = before[:-1] + "ī"
                i += 1
                continue
            if before.endswith("a") and (t.has(SUKUN) or not t.marks) and not (i + 1 < n and word[i + 1].letter in "اى"):
                out[-1] = before + ("u" if t.letter == "و" else "i")
                i += 1
                continue
            if t.has(DAGGER_ALIF):  # Uthmani ٱلصَّلَوٰة: the waw carries the long a
                out[-1] = before[:-1] + "ā" if before.endswith("a") else before + "ā"
                i += 1
                continue

        # Hamza: an apostrophe inside a word, nothing at its start; after a one-letter prefix
        # (wa-, fa-, bi-) the hamza starts a new word in Latin.
        if t.letter in HAMZAS:
            v = t.vowel or ("i" if t.letter == "إ" else "a" if t.letter == "أ" else "")
            if t.has(DAGGER_ALIF) or t.has(MADDAH):
                v = "ā"
            if last and at_pause and v in ("a", "i", "u", "an", "in", "un"):
                v = ""  # the pausal form: السَّمَاءِ -> samā'
            if i == 0 or (out and out[-1].endswith("-")):
                out.append(v)
            elif i == 1 and prev.letter in PREFIXES and prev.vowel:
                out.append(" " + v)
            else:
                out.append("'" + v)
            i += 1
            continue

        cons = CONSONANTS.get(t.letter, "")
        if t.letter == "ة":
            cons = "h" if (last and at_pause) or not t.vowel else "t"
        if t.has(SHADDA) and cons and i > 0:
            cons = cons + cons
        vowel = t.vowel
        if is_jalalah(word, i):
            t.marks.add("jalalah")
            if prev is not None and not prev.vowel and out and out[-1] == "l":
                out[-1] = "li"
        if "jalalah" in t.marks or t.has(DAGGER_ALIF) or (i in long_at and vowel == "a"):
            vowel = "ā"
        if t.has(SMALL_WAW):
            vowel = "ū"
        if t.has(SMALL_YEH) or t.has(SMALL_HIGH_YEH):
            vowel = "ī"
        if last and at_pause:
            if t.letter == "ة":
                vowel = ""
            elif t.has(FATHATAN):
                vowel = "ā"
            elif vowel in ("a", "i", "u", "an", "in", "un"):
                vowel = ""
        out.append(cons + vowel)
        i += 1
    return "".join(out)


def transliterate(arabic: str) -> str | None:
    """Indonesian-style Latin for [arabic], or None when a word can't be read by the rules.

    A bracketed note the rules can't read (the book's [ثلاثاً]) is left out; anywhere else an
    unreadable word gives None rather than a guess.
    """
    items = tokenize(arabic)
    # Drop bracketed groups that hold an unreadable word.
    kept: list = []
    k = 0
    while k < len(items):
        if items[k] == "[" and "]" in items[k:]:
            end = items.index("]", k)
            group = items[k:end + 1]
            if all(vowelled(w) for w in group if isinstance(w, list)):
                kept.extend(group)
            k = end + 1
            continue
        kept.append(items[k])
        k += 1
    items = kept
    words = [x for x in items if isinstance(x, list)]
    if not words or not all(vowelled(w) for w in words):
        return None

    pieces: list[str] = []
    for k, item in enumerate(items):
        if not isinstance(item, list):
            if item in PAUSE:
                mark = PAUSE[item]
                if mark:
                    pieces.append(mark)
            elif item in KEEP:
                pieces.append(KEEP[item])
            elif item.isspace():
                pieces.append(" ")
            elif item in IGNORE:
                pieces.append(" ")
            else:
                pieces.append(item)
            continue
        rest = items[k + 1:]
        nxt = next((x for x in rest if not (isinstance(x, str) and (x.isspace() or x in IGNORE))), None)
        at_pause = nxt is None or (isinstance(nxt, str) and (nxt in PAUSE or nxt == "]"))
        so_far = "".join(pieces).rstrip()
        joined = bool(so_far) and not so_far.endswith((",", ".", ";", ":", "?", "!", "["))
        starts_with_wasl = item[0].letter in "اٱ" and not item[0].vowel and not item[0].has(MADDAH)
        text = render(item, at_pause, joined and starts_with_wasl)
        if joined and starts_with_wasl:
            # Read on from the previous word: drop the space, shorten a long final vowel and give
            # a closed ending a helping vowel (min + al- -> minal-).
            while pieces and pieces[-1].isspace():
                pieces.pop()
            if pieces:
                tail = pieces[-1]
                if tail[-1:] in "āīū":
                    tail = tail[:-1] + {"ā": "a", "ī": "i", "ū": "u"}[tail[-1]]
                elif re.search(r"(?:^|[ -]|\b(?:wa|fa))min$", tail):
                    tail += "a"  # min + al- -> minal-
                elif tail[-1:] not in "aiu":
                    tail += "i"
                pieces[-1] = tail
        pieces.append(text)

    latin = "".join(pieces)
    latin = re.sub(r"\s+", " ", latin).strip()
    latin = re.sub(r"\s+([,.;:?!\]])", r"\1", latin)
    latin = re.sub(r"([,.;:?!])(?=\S)", r"\1 ", latin)
    latin = re.sub(r"\[\s+", "[", latin)
    latin = re.sub(r"(?<![\w'])allāh", "Allāh", latin)  # not when joined: sami'allāhu
    # A capital at the start and after a full stop.
    latin = re.sub(r"(^|[.?!]\s+)([a-zāīū'])", lambda m: m.group(1) + m.group(2).upper(), latin)
    return latin or None


if __name__ == "__main__":
    for arg in sys.argv[1:]:
        print(transliterate(arg))
