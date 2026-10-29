"""Prints the GitHub Release body for a version: the app's own What's new, Indonesian then English.

    python3 -I tools/release_notes.py 1.4.2 > notes.md

Read by .github/workflows/release.yml. The text comes from WhatsNew.kt and the whats_new_*
arrays (see tools/site/build_whatsnew.py). The in-app updater shows the release body as plain
text, so it uses no Markdown headings: a label line, then "- " items, which read the same in the
app and on GitHub. A version without an entry prints nothing (the workflow then falls back to
GitHub's list of merged PRs).
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent / "site"))
import build_whatsnew as wn  # noqa: E402


def notes(version: str) -> str:
    for v, array in wn.RELEASE.findall(wn.KOTLIN.read_text("utf-8")):
        if v == version:
            id_ = wn.arrays(wn.RES / "values/strings.xml")[array]
            en = wn.arrays(wn.RES / "values-en/strings.xml")[array]
            bullets = lambda items: "\n".join(f"- {t}" for t in items)  # noqa: E731
            return f"Yang baru:\n{bullets(id_)}\n\nWhat's new:\n{bullets(en)}\n"
    return ""


if __name__ == "__main__":
    print(notes(sys.argv[1]), end="")
