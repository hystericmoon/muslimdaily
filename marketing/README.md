# marketing/

Promo material for Adwam. Not part of the app build (nothing here is referenced by Gradle).

- `brief.md`: idea, look, motion rules, claims and where the app proves each one.
- `copy.md`, `alt-text.md`: post captions (Instagram in Indonesian, LinkedIn in English), video
  descriptions and image alt text.
- `calendar.csv`: four-week posting plan (also a Google Sheet in Drive, "Adwam promo").
- `scenes/`: one HTML page per video or still set. Each frame is a pure function of time, so the
  render is deterministic. Query `fmt=916|45` (9:16 or 4:5) and `lang=id|en`.
- `engine/`: `render.js` (Playwright frames into ffmpeg, 60 fps sub-frames blended to 30 fps),
  `motion.js` (curves, springs), `scene.css` (shared look).
- `tools/prep_screens.py`: copies CI screenshots from `docs/screenshots` and removes every Arabic
  glyph (words become empty hidden-word boxes). `tools/prep_mark.py`: exports the mark's outline,
  centreline and outlined wordmark for the end card.
- `fonts/`: Plus Jakarta Sans (SIL OFL 1.1), downloaded from Google Fonts.
- `out/` (ignored): rendered videos (`.mp4` + `.srt`), images, carousel PDFs. Finished media is
  kept off `main` on the orphan branch `promo-assets` so the repo history stays small.
  `build/` is generated and ignored.

## Render

```
pip install pillow numpy scipy shapely
python3 marketing/tools/prep_screens.py
python3 marketing/tools/prep_mark.py
bash marketing/render_all.sh only-missing          # all videos (~1.5 h on 4 cores)
node marketing/engine/render.js marketing/scenes/stills.html out.png --still 0 --query "id=img1&lang=id"
```

## Rules (owner, 5 Nov 2026)

No music; no faces or awrah; no IslamHouse or HadeethEnc material, so no dua, hadith, verse or
recitation and no readable dua card; no streak or points language; claims the app proves only.
After a screen change, rerun `prep_screens.py` and check its output by eye: regions are set by hand.
