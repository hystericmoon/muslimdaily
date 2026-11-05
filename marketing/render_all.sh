#!/usr/bin/env bash
# Render every promo video variant. Run from the repo root:
#   pip install pillow numpy scipy shapely   # once
#   python3 marketing/tools/prep_screens.py && python3 marketing/tools/prep_mark.py
#   bash marketing/render_all.sh [only-missing]
# About 0.5 s per frame on 4 cores (60 fps sub-frames blended to 30 fps).
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=marketing/out
mkdir -p "$OUT"
ONLY_MISSING=0; [[ "${1:-}" == "only-missing" ]] && ONLY_MISSING=1

render() { # scene out-name ratio lang
  local f="$OUT/$2-$4-$3.mp4" fmt=45
  [[ "$3" == 9x16 ]] && fmt=916
  if [[ $ONLY_MISSING == 1 && -s "$f" ]]; then echo "skip $f"; return; fi
  node marketing/engine/render.js "marketing/scenes/$1.html" "$f" --query "fmt=$fmt&lang=$4"
}

for v in "v1-memorize v1-memorize" "v2-for-every-eye v2-every-eye"; do
  read -r scene name <<< "$v"
  for fl in "9x16 id" "4x5 id" "4x5 en" "9x16 en"; do
    read -r ratio lang <<< "$fl"
    render "$scene" "$name" "$ratio" "$lang"
  done
done
