# Accessibility (WCAG 2.2 AA)

Audit of the build on this branch. "Verified" means checked by a test, lint or the screenshot
set; "Implemented" means built to the rule but needs a device pass with TalkBack.

| Requirement | How it is met | Status |
|---|---|---|
| Text contrast ≥ 4.5:1 | All text roles measured in `docs/design-tokens.md` (lowest 6.09:1 light, 7.19:1 dark) | Verified |
| Non-text ≥ 3:1 | Outlines 4.23:1 / 5.84:1; active-phrase underline 6.09:1 (teal, light) / 7.65:1 (gold, dark); check glyph on gold 7.10:1 | Verified |
| Gold never as text on light | Gold only as fills; in light mode the underline is teal because raw gold is 2.28:1 on parchment | Verified |
| State not by colour alone | Grading chip has a text label; active phrase = fill + underline; completion = gold disc + check + "Selesai"; current prayer = "Sekarang" badge; prohibited = icon + text | Verified (screenshots) |
| Touch targets ≥ 48dp | Rows ≥ 64dp; player main button 72dp; run card is the whole card; checklist rows are whole-row toggles | Verified (screenshots) |
| 200% font, no clipping | Arabic leading 1.9× with `LineHeightStyle.Trim.None`; steppers stack and segmented choices become radio lists at ≥ 150%; button labels wrap; nav labels capped at 1.5× and step down to fit | Verified (`*-font200.png`) |
| In-app text size | Arabic and translation scales (80–180%) applied on top of the system scale | Implemented |
| TalkBack labels | Icon buttons have descriptions; decorative icons are null; settings rows merge label + value and read as buttons, not unselected toggles; segmented choices read "title: option" once; checklist rows say "Sudah/Belum dikerjakan"; steppers name their effect ("Tambah Subuh") | Device pass 2026-10-25 (emulator) |
| Headings | Section headers, screen titles, sheet titles marked as headings | Implemented |
| Live regions | Run-mode count ("2 dari 3"), memorize turn prompts, stepper values. Not the next-prayer countdown: it is read with the card when focused, so it doesn't interrupt every minute | Verified for run mode (UI test) |
| Arabic voice | Every Arabic string carries an Arabic `LocaleList` span (exported as `LocaleSpan`); English translations an English one | Implemented |
| Bidi isolation | City names inside Indonesian sentences wrapped in FSI…PDI | Implemented |
| Timing adjustable (2.2.1) | Auto-advance toggle (run mode menu and settings); memorize pauses auto or manual; no timeouts | Implemented |
| Reduced motion | `ANIMATOR_DURATION_SCALE == 0` → all springs snap; only count tap and card advance animate | Implemented |
| Recall without vision | Hidden words are blank boxes of the word's width (never faint text); TalkBack hears one description and uses "Putar dengan jeda" (hidden phrases muted) | Implemented |
| Drag alternatives (2.5.7) | Routine editor rows expose "Pindah ke atas / bawah" accessibility actions | To verify (the UI test went with Sesuaikan) |
| Keyboard / switch access | All actions are standard clickable/toggleable/selectable nodes | Implemented |

## Known gaps for the device pass

* TalkBack reading order on the Today screen with a long timeline expanded.
* Media notification controls with TalkBack on the lock screen (Media3 defaults).
* Arabic TTS quality depends on the user's installed TTS voice; there is no in-app fallback.
* Run-mode haptics on devices without `SegmentTick`/`Confirm` effects fall back to the system
  default.
