#!/usr/bin/env python3
"""The third accent colour (the 10 of 60:30:10), one per palette and mode.

Reads the palette tokens in docs/design-tokens.md, takes the accent hue for each palette from
HUES (spread around the OKLCH wheel, at least 50 degrees from the palette's own colour and clear
of gold at 90 and the error red at 28), then picks the lightest or darkest tone that reaches the
contrast targets. Prints a markdown table, or JSON with --json. Decided 18 Nov 2026, see
docs/accent.md. Run: python3 tools/accent_palette.py [--json]
"""
import json
import math
import re
import sys
from pathlib import Path

HUES = {"TEAL": 295, "NAVY": 130, "FOREST": 325, "SLATE": 200, "OLIVE": 350, "ROSE": 235, "LAVENDER": 165, "PEACH": 265}
CHROMA = 0.13
TEXT_ON_SURFACE = 4.6  # needs 4.5
TEXT_ON_CONTAINER = 7.0
DOCS = Path(__file__).resolve().parent.parent / "docs" / "design-tokens.md"


def _lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gamma(c):
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def _rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]


def _hex(rgb):
    return "#%02X%02X%02X" % tuple(round(max(0, min(1, x)) * 255) for x in rgb)


def luminance(h):
    r, g, b = (_lin(x) for x in _rgb(h))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    la, lb = sorted((luminance(a), luminance(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)


def oklch_hue(h):
    r, g, b = (_lin(x) for x in _rgb(h))
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    b2 = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return math.degrees(math.atan2(b2, a)) % 360


def make(hue, chroma, light):
    """The colour at OKLCH (light, chroma, hue), with chroma lowered until it fits sRGB."""
    c = chroma
    while c > 0:
        a, b = c * math.cos(math.radians(hue)), c * math.sin(math.radians(hue))
        l_ = light + 0.3963377774 * a + 0.2158037573 * b
        m_ = light - 0.1055613458 * a - 0.0638541728 * b
        s_ = light - 0.0894841775 * a - 1.2914855480 * b
        l3, m3, s3 = l_ ** 3, m_ ** 3, s_ ** 3
        rgb = [
            4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
            -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
            -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
        ]
        if all(-1e-4 <= x <= 1 + 1e-4 for x in rgb):
            return _hex([_gamma(max(0, min(1, x))) for x in rgb])
        c -= 0.002
    return _hex([_gamma(light)] * 3)


def tone_for(hue, chroma, background, target, lighter):
    """The tone closest to the background that still reaches the contrast target."""
    found = [(i / 200, make(hue, chroma, i / 200)) for i in range(10, 196)]
    found = [(t, h) for t, h in found if contrast(h, background) >= target]
    if not found:
        raise ValueError("no tone reaches %.1f on %s" % (target, background))
    return (min(found) if lighter else max(found))[1]


def read_tokens():
    out = {}
    for sec in re.split(r"\n### ", DOCS.read_text())[1:]:
        m = re.match(r"(\w+) (light|dark)", sec.split("\n", 1)[0])
        if not m:
            continue
        d = out.setdefault(m.group(1), {}).setdefault(m.group(2), {})
        for row in re.finditer(r"\|\s*`(\w+)`\s*(#[0-9a-fA-F]{6})\s*\|\s*`(\w+)`\s*(#[0-9a-fA-F]{6})", sec):
            d.setdefault(row.group(1), row.group(2))
            d.setdefault(row.group(3), row.group(4))
    return out


def build():
    tokens = read_tokens()
    result = {}
    for palette, hue in HUES.items():
        result[palette] = {"hue": hue}
        for mode in ("light", "dark"):
            t, dark = tokens[palette][mode], mode == "dark"
            accent = tone_for(hue, CHROMA, t["surface"], TEXT_ON_SURFACE, dark)
            if contrast(accent, t["surfaceContainerHighest"]) < 4.5:
                accent = tone_for(hue, CHROMA, t["surfaceContainerHighest"], TEXT_ON_SURFACE, dark)
            on = "#FFFFFF" if contrast("#FFFFFF", accent) >= contrast("#000000", accent) else "#000000"
            container = make(hue, 0.05, 0.32) if dark else make(hue, 0.045, 0.92)
            on_container = tone_for(hue, 0.06, container, TEXT_ON_CONTAINER, dark)
            result[palette][mode] = {
                "accent": accent,
                "onAccent": on,
                "accentContainer": container,
                "onAccentContainer": on_container,
                "accentOnSurface": round(contrast(accent, t["surface"]), 2),
                "accentOnHighest": round(contrast(accent, t["surfaceContainerHighest"]), 2),
                "onAccentOnAccent": round(contrast(on, accent), 2),
                "onContainerOnContainer": round(contrast(on_container, container), 2),
            }
    return result


def main():
    result = build()
    if "--json" in sys.argv:
        print(json.dumps(result, indent=2))
        return
    print("| Palette | Hue | Mode | accent | onAccent | accentContainer | onAccentContainer | on surface | on highest |")
    print("|---|---|---|---|---|---|---|---|---|")
    for palette, v in result.items():
        for mode in ("light", "dark"):
            d = v[mode]
            print("| %s | %d | %s | `%s` | `%s` | `%s` | `%s` | %.2f | %.2f |" % (
                palette, v["hue"], mode, d["accent"], d["onAccent"], d["accentContainer"],
                d["onAccentContainer"], d["accentOnSurface"], d["accentOnHighest"]))


if __name__ == "__main__":
    main()
