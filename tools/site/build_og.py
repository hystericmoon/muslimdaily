"""Renders tools/site/og.html to site/og.png (1200×630), the image shown when the site is shared.

    python3 tools/site/build_og.py   (needs Node and Playwright with Chromium)

Re-run after changing the headline, the lockup or the Today screenshot (build_shots.py).
"""
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = """
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
  await page.goto('file://' + process.argv[1]);
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: process.argv[2] });
  await browser.close();
})();
"""


def main() -> int:
    out = ROOT / "site/og.png"
    subprocess.run(["node", "-e", SCRIPT, str(ROOT / "tools/site/og.html"), str(out)], check=True)
    print(f"Wrote {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
