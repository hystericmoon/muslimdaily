"""Writes site/whatsnew.json: the latest releases' highlights in Indonesian and English.

    python3 -I tools/site/build_whatsnew.py [out.json]

The text is the app's own What's new (feature/whatsnew/WhatsNew.kt and the whats_new_* arrays
in values/ and values-en/strings.xml), so the site never needs its own copy. Pre-releases are
left out. Run by .github/workflows/pages.yml before each deploy.
"""
import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
KOTLIN = ROOT / "app/src/main/java/id/haho/muslimdaily/feature/whatsnew/WhatsNew.kt"
RES = ROOT / "app/src/main/res"
RELEASE = re.compile(r'Release\("([^"]+)",\s*R\.array\.(\w+)\)')
LATEST = 2


def arrays(path: pathlib.Path) -> dict[str, list[str]]:
    out = {}
    for arr in ET.parse(path).getroot().iter("string-array"):
        out[arr.get("name")] = [unescape("".join(item.itertext())) for item in arr.iter("item")]
    return out


def unescape(text: str) -> str:
    """Android string escapes (\\' \\" \\n) to plain text."""
    text = re.sub(r"\\([\"'@?])", r"\1", text.strip())
    return " ".join(text.replace("\\n", " ").split())


def build() -> list[dict]:
    releases = [(v, a) for v, a in RELEASE.findall(KOTLIN.read_text("utf-8")) if "-" not in v]
    id_, en = arrays(RES / "values/strings.xml"), arrays(RES / "values-en/strings.xml")
    return [{"version": v, "id": id_[a], "en": en[a]} for v, a in releases[:LATEST]]


def main() -> int:
    out = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "site/whatsnew.json"
    out.write_text(json.dumps(build(), ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Wrote {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
