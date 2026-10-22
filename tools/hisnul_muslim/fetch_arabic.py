"""Downloads the Arabic text of Hisnul Muslim from hisnmuslim.com (the book's official site).

Entries are keyed by the book's standard numbering (1-267), the same numbers the Indonesian
PDF uses, so the two sources join without guessing. Responses are cached on disk.
"""
from __future__ import annotations

import json
import time
import urllib.request
from pathlib import Path

INDEX = "https://www.hisnmuslim.com/api/ar/husn_ar.json"


def _get(url: str, cache: Path) -> dict:
    path = cache / url.rsplit("/", 1)[-1]
    if not path.exists():
        cache.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(url, headers={"User-Agent": "MuslimDaily content importer"})
        with urllib.request.urlopen(req, timeout=30) as r:
            path.write_bytes(r.read())
        time.sleep(0.3)  # be gentle with the site
    return json.loads(path.read_text(encoding="utf-8-sig"))


def fetch(cache: Path) -> dict[int, dict]:
    """Returns {entry number: {"arabic", "repeat", "audio", "chapter_id", "chapter_ar"}}."""
    index = _get(INDEX, cache)
    chapters = next(iter(index.values()))
    out: dict[int, dict] = {}
    for ch in chapters:
        data = _get(ch["TEXT"].replace("http://", "https://"), cache)
        for item in next(iter(data.values())):
            n = int(item["ID"])
            out[n] = {
                "arabic": item["ARABIC_TEXT"].strip(),
                "repeat": int(item["REPEAT"]) if str(item.get("REPEAT", "")).isdigit() else None,
                "audio": item.get("AUDIO", ""),
                "chapter_id": int(ch["ID"]),
                "chapter_ar": ch["TITLE"],
            }
    return out
