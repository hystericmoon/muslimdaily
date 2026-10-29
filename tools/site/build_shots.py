"""Crops the site's app screenshots (127+ in docs/screenshots, Forest dark) to phone-height images."""
import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "docs" / "screenshots"
OUT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "site" / "shots"

# name -> (screenshot, crop top and height in px at 800 wide)
SHOTS = {
    "today": ("127-site-today.png", 0, 1700),
    "prayers": ("127-site-today.png", 1500, 1700),
    "run": ("128-site-morning.png", 0, 1700),
    "dua": ("129-site-ayat-kursi.png", 0, 1700),
    "recall": ("130-site-recall.png", 0, 1700),
    "fasting": ("131-site-fasting.png", 0, 1700),
}
WIDGET = "80-widget-4x2.png"


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (file, top, height) in SHOTS.items():
        im = Image.open(SRC / file).convert("RGB")
        im = im.crop((0, top, im.width, top + height)).resize((560, height * 560 // 800), Image.LANCZOS)
        im.save(OUT / f"{name}.webp", quality=82, method=6)
    Image.open(SRC / WIDGET).save(OUT / "widget.webp", lossless=True, method=6)


if __name__ == "__main__":
    main()
