"""Prepare app screenshots for promo scenes.

The CI screenshots show the app's font-test Arabic string, not a dua. The promo rules forbid any
frame that reads like a dua card, so every Arabic glyph on these screens is removed:
  - "box":   each word becomes an empty hidden-word box (the app's own 100 %-hidden state)
  - "erase": glyphs are painted out with the chip's own fill (for screens that already draw chips)
Regions are in the 800 px-wide screenshot's coordinates. Output: marketing/build/screens/.

Usage: python3 marketing/tools/prep_screens.py   (needs Pillow, numpy, scipy)
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "docs" / "screenshots"
OUT = ROOT / "marketing" / "build" / "screens"
CROP_H = 1700

# name -> (mode, [regions]); None means "copy as is" (screen has no Arabic in the crop).
SCREENS = {
    "52-memorize-recall": ("box", [(130, 770, 740, 1095)]),
    "51-memorize-build": ("box", [(360, 400, 780, 520)]),
    "53-memorize-check": ("erase", [(70, 520, 790, 870)]),
    "50-memorize-home": ("box", [(480, 440, 760, 520), (480, 780, 760, 860),
                                 (480, 1155, 760, 1235), (480, 1490, 760, 1570)]),
    "10-today-morning": None,
    "100-onboarding": None,
    "41-settings-prayer": None,
}
# Tablet screens are copied whole (no Arabic script on them); font200 renders are light-only.
TABLET = ["102-today-tablet"]
FONT200 = ["10-today-morning"]
THEMES = ("light", "dark")
# English-UI shots (PromoEnglishScreenshotTest) -> the same scene name with an "-en" suffix. The
# English layouts match the Indonesian ones, so the regions above apply unchanged.
EN_SRC = {
    "50-memorize-home": "150-memorize-home-en", "51-memorize-build": "151-memorize-build-en",
    "52-memorize-recall": "152-memorize-recall-en", "53-memorize-check": "153-memorize-check-en",
    "10-today-morning": "154-today-morning-en", "100-onboarding": "155-onboarding-en",
    "41-settings-prayer": "156-settings-prayer-en", "102-today-tablet": "157-today-tablet-en",
}


def glyph_mask(a, region, light):
    lum = a.mean(2)
    m = np.zeros(lum.shape, bool)
    x0, y0, x1, y1 = region
    sub = lum[y0:y1, x0:x1]
    m[y0:y1, x0:x1] = (sub < 90) if light else (sub > 200)
    return m


def treat(img, mode, regions, light):
    a = np.asarray(img).astype(int)
    out = img.copy()
    d = ImageDraw.Draw(out)
    # Box colours sampled from the app's own hidden-word box on the recall screen.
    fill, edge = ((229, 226, 219), (111, 121, 120)) if light else ((51, 53, 51), (136, 147, 145))
    for reg in regions:
        mask = glyph_mask(a, reg, light)
        if mode == "erase":
            lab, _ = ndimage.label(ndimage.binary_dilation(mask, iterations=3))
            for sl in ndimage.find_objects(lab):
                y0, y1, x0, x1 = sl[0].start, sl[0].stop, sl[1].start, sl[1].stop
                patch = a[y0:y1, x0:x1]
                pm = ndimage.binary_dilation(mask[y0:y1, x0:x1], iterations=3)
                bg = np.median(patch[~pm], 0) if (~pm).any() else patch.reshape(-1, 3).mean(0)
                arr = np.asarray(out).copy()
                region = arr[y0:y1, x0:x1]
                region[pm] = bg.astype(np.uint8)
                out = Image.fromarray(arr)
                d = ImageDraw.Draw(out)
            continue
        # Paint glyphs out with the surrounding ground (normalised Gaussian fill keeps gradients).
        hole = ndimage.binary_dilation(mask, iterations=4)
        w = (~hole).astype(float)
        cur = np.asarray(out).astype(float)
        num = np.stack([ndimage.gaussian_filter(cur[..., c] * w, 12) for c in range(3)], -1)
        den = ndimage.gaussian_filter(w, 12)[..., None]
        fillimg = num / np.maximum(den, 1e-6)
        cur[hole] = fillimg[hole]
        out = Image.fromarray(cur.clip(0, 255).astype(np.uint8))
        d = ImageDraw.Draw(out)
        lab, _ = ndimage.label(ndimage.binary_dilation(mask, structure=np.ones((31, 17))))
        objs = [s for s in ndimage.find_objects(lab) if s[0].stop - s[0].start >= 14]
        for sl in objs:
            cy = (sl[0].start + sl[0].stop) // 2
            d.rounded_rectangle((sl[1].start, cy - 30, sl[1].stop, cy + 30), radius=6,
                                fill=fill, outline=edge, width=2)
    return out


def soften(img, regions, light):
    """Blur the glyphs past reading (used only for a few frames while words turn into boxes)."""
    a = np.asarray(img).astype(int)
    mask = np.zeros(a.shape[:2], bool)
    for reg in regions:
        mask |= glyph_mask(a, reg, light)
    mask = ndimage.binary_dilation(mask, iterations=8)
    out = img.copy()
    out.paste(img.filter(ImageFilter.GaussianBlur(14)), mask=Image.fromarray((mask * 255).astype("uint8")))
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for lang in ("", "-en"):
        src_of = (lambda n: n) if not lang else (lambda n: EN_SRC[n])
        for name, spec in SCREENS.items():
            for theme in THEMES:
                img = Image.open(SRC / f"{src_of(name)}-{theme}.png").convert("RGB")
                img = img.crop((0, 0, img.width, min(img.height, CROP_H)))
                if spec:
                    if spec[0] == "box":
                        soften(img, spec[1], theme == "light").save(OUT / f"{name}{lang}-{theme}-soft.png", optimize=True)
                    img = treat(img, spec[0], spec[1], theme == "light")
                img.save(OUT / f"{name}{lang}-{theme}.png", optimize=True)
                print("wrote", f"{name}{lang}-{theme}.png")
        for name in FONT200:
            img = Image.open(SRC / f"{src_of(name)}-font200.png").convert("RGB")
            img.crop((0, 0, img.width, CROP_H)).save(OUT / f"{name}{lang}-font200.png", optimize=True)
            print("wrote", f"{name}{lang}-font200.png")
        for name in TABLET:
            for theme in THEMES:
                Image.open(SRC / f"{src_of(name)}-{theme}.png").convert("RGB").save(OUT / f"{name}{lang}-{theme}.png", optimize=True)
                print("wrote", f"{name}{lang}-{theme}.png")


if __name__ == "__main__":
    main()
