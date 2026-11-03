"""Snapshots hisnmuslim.com's Indonesian Hisn al-Muslim pages for rows the Indonesian PDF lacks.

    python3 -I tools/hisnul_muslim/fetch_indonesian.py 69          # chapter numbers on the site

Writes content/source/import/hisnmuslim_id.json: each entry ("thikr") of the chapter, keyed by
the book's standard number (the number of its recording, `.../audio/ar/<no>.mp3`, the same
number each CSV row records in `notes` as "Hisnul Muslim no. N"). The text is the page's,
unchanged apart from collapsing whitespace; the footnote with the takhrij stays in it.

Used by build_content.py for `translation_id_from = hisnmuslim:<no>`: the row's Indonesian must
be words of that entry, in order. Owner's choice (3 Nov 2026, decision page
https://claude.ai/artifact/EKRbEQQWuChwfejNvX93wC): eating_08 takes its Indonesian from chapter
69. The page names no translator, so none is named (IslamHouse, 2026: "If no translator is
listed for a particular edition, please do not infer or assign one"). The site serves
Indonesian as HTML only; its API has Arabic and English.
"""
import html
import json
import re
import sys
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "content/source/import/hisnmuslim_id.json"
URL = "https://hisnmuslim.com/i/id/{}"
THIKR = re.compile(r'<div class="thikr">(.*?)<div class="meaning">', re.S)
AUDIO_NO = re.compile(r'data-audio="[^"]*/audio/ar/(\d+)\.mp3"')
TITLE = re.compile(r"<title>(.*?)</title>", re.S)


def entries(page: str) -> dict[str, str]:
    """Book number -> the entry's text, from one chapter page."""
    out = {}
    for block in THIKR.findall(page):
        number = AUDIO_NO.search(block)
        if not number:
            raise ValueError("an entry without its recording number")
        text = html.unescape(re.sub(r"<[^>]+>", " ", block))
        out[number.group(1)] = re.sub(r"\s+", " ", text).strip()
    return out


def fetch(chapter: int) -> str:
    req = urllib.request.Request(URL.format(chapter), headers={"User-Agent": "adwam-content-import"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode("utf-8")


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    doc = json.loads(OUT.read_text(encoding="utf-8")) if OUT.exists() else {
        "source": "hisnmuslim.com (IslamHouse.com), Indonesian; no translator named", "entries": {}}
    for chapter in map(int, argv):
        page = fetch(chapter)
        title = TITLE.search(page)
        found = entries(page)
        if not found:
            print(f"chapter {chapter}: no entries found", file=sys.stderr)
            return 1
        for no, text in found.items():
            doc["entries"][no] = {"chapter": chapter, "url": URL.format(chapter), "fetched": date.today().isoformat(),
                                  "title": html.unescape(title.group(1)).strip() if title else "", "text": text}
        print(f"chapter {chapter}: entries {', '.join(found)}")
    doc["entries"] = dict(sorted(doc["entries"].items(), key=lambda kv: int(kv[0])))
    OUT.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
