"""Export the Adwam mark for animation: the official outline plus its centreline.

The centreline comes from tools/brand/build_mark.py (the same geometry the outline is built from),
mapped into adwam-mark.svg's coordinates, so a stroke drawn along it lands exactly under the
outline. Scenes draw the stroke, then hand over to the official outline; the wordmark is always
the outlined path from the lockup, never retyped.

Writes marketing/build/mark.json and mark.js (window.MARK). Needs shapely.
"""
import json
import re
import sys
from pathlib import Path

from shapely.ops import unary_union

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "brand"))
import build_mark  # noqa: E402

BRAND = ROOT / "docs" / "brand"


def paths(svg):
    out = []
    for p in re.findall(r"<path ([^>]*)>", svg):
        fill = re.search(r'fill="([^"]*)"', p)
        out.append({"d": re.search(r'\bd="([^"]*)"', p).group(1), "fill": fill and fill.group(1)})
    return out


def main():
    shape, dot = build_mark.mark()
    minx, miny, maxx, maxy = unary_union([shape, dot]).bounds
    mark_svg = (BRAND / "adwam-mark.svg").read_text()
    vw, vh = [float(v) for v in re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', mark_svg).groups()]
    s = vw / (maxx - minx)
    pts = build_mark._centreline()
    base = build_mark.BASE
    # The outline's baseline runs past the loop's end to the last centreline x; include it.
    line = [(pts[0][0], base)] + pts
    cl = [((x - minx) * s, (y - miny) * s) for x, y in line]
    d = "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in cl)
    cx, cy = dot.centroid.x, dot.centroid.y
    lock = (BRAND / "adwam-lockup-dark.svg").read_text()
    lock_vb = [float(v) for v in re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', lock).groups()]
    lp = paths(lock)
    out = {
        "viewBox": [vw, vh],
        "strokeWidth": build_mark.W * s,
        "centreline": d,
        "dot": {"cx": (cx - minx) * s, "cy": (cy - miny) * s, "r": build_mark.W / 2 * s},
        "outline": paths(mark_svg),
        "lockup": {"viewBox": lock_vb, "markScale": 134.4 / vh, "wordmark": lp[2:]},
    }
    dst = ROOT / "marketing" / "build" / "mark.json"
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(json.dumps(out))
    # Scenes load over file://, where fetch() is blocked; give them a script too.
    (dst.parent / "mark.js").write_text("window.MARK = " + json.dumps(out) + ";\n")
    print("wrote", dst.relative_to(ROOT), "stroke", round(out["strokeWidth"], 2))


if __name__ == "__main__":
    main()
