---
name: Adwam home-screen widget
description: A smoked-glass instrument card over a sky that follows the time of day; the next prayer, the time left, and how far the day has gone.
colors:
  accent-signal: "#EEF26B"
  on-sky: "#FFFFFF"
  muted-white: "#FFFFFFA6"
  smoked-card: "#0E1218A8"
  card-hairline: "#FFFFFF14"
  header-divider: "#FFFFFF1A"
  bracket-white: "#FFFFFFD9"
  track-dash: "#FFFFFF38"
  chip-fill: "#EEF26B26"
typography:
  numeral:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "40sp"
    fontWeight: 500
    lineHeight: 1
    fontFeature: "tnum"
  countdown:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "15sp"
    fontWeight: 500
    lineHeight: 1
    fontFeature: "tnum"
  prayer-name:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  body:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: 1
    fontFeature: "tnum"
  label:
    fontFamily: "monospace"
    fontSize: "9.5sp"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0.06em"
  scale:
    fontFamily: "monospace"
    fontSize: "9sp"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0.06em"
rounded:
  bar: "1dp"
  chip: "3dp"
  sky-fallback: "24dp"
spacing:
  xs: "2dp"
  s: "4dp"
  m: "8dp"
  l: "12dp"
  inset: "6dp"
  inset-small: "4dp"
  pad: "14dp"
  pad-short: "10dp"
  pad-small: "8dp"
  bar: "5dp"
  bar-thin: "4dp"
components:
  card:
    backgroundColor: "{colors.smoked-card}"
    padding: "14dp"
  chip-hijri:
    backgroundColor: "{colors.chip-fill}"
    textColor: "{colors.accent-signal}"
    typography: "{typography.label}"
    rounded: "{rounded.chip}"
    padding: "3dp 6dp"
  next-label:
    textColor: "{colors.muted-white}"
    typography: "{typography.label}"
  next-name:
    textColor: "{colors.accent-signal}"
    typography: "{typography.prayer-name}"
  next-time:
    textColor: "{colors.on-sky}"
    typography: "{typography.numeral}"
  countdown:
    textColor: "{colors.on-sky}"
    typography: "{typography.countdown}"
  bar-fill:
    backgroundColor: "{colors.accent-signal}"
    rounded: "{rounded.bar}"
    height: "5dp"
  bar-track:
    backgroundColor: "{colors.track-dash}"
    height: "5dp"
  scale-stop:
    textColor: "{colors.muted-white}"
    typography: "{typography.scale}"
  scale-stop-next:
    textColor: "{colors.accent-signal}"
    typography: "{typography.scale}"
---

# Design System: Adwam home-screen widget

**Scope.** This file governs one surface: the home-screen and lock-screen prayer widget (`app/src/main/java/id/haho/muslimdaily/widget/`, the `widget_text*` layouts and `widget_chip` drawable). The app's own screens keep their established Material 3 system: palettes, surfaces, gold and contrast tables live in `docs/design-tokens.md`, which this file neither restates nor replaces. Nothing here applies to app screens, and nothing in `docs/design-tokens.md` applies to the widget.

## Overview

**Creative North Star: "The Instrument Over the Sky"**

The widget is a dark smoked-glass card set into a sky that changes with the time of day. It reads like a signal meter. One yellow accent says what comes next, a large figure gives its time, a smaller phrase says how long until then, and a bar shows how far the day (or the night) has gone. The sky is the only colour field. The card darkens it enough for every text colour to pass AA, and the sky still shows round the card's edge and faintly through it.

It is dense but calm. Each size class from 1×1 to 5×3 is put together from the same few parts, and spare room goes to the figures, never to padding. The type keeps two voices apart: Google Sans Flex for the content (numbers, names, the countdown), and the system monospace, letter-spaced and in caps, for the instrument's own labels and the bar's scale. The build rejects the category default: there is no gradient card holding a table of the five times. The times show up only as the bar's scale, and only where there is room.

**Key Characteristics:**
- One accent (`accent-signal`), used only for what is next: the prayer's name, its stop on the scale, the bar's filled part, the Hijri chip, and tomorrow's sunnah fast.
- A smoked card inset from the sky, with a hairline edge and white bracket corners drawn in the margin around it.
- Mono caps labels as instrument legends ("BERIKUTNYA", "TIBA DALAM") paired with the values they label.
- Figures grow to fill tall cells. Text never ellipsizes where it carries the answer.
- The sky follows the prayer times. The bar runs from Subuh to Isya by day and from Isya to Subuh at night.

## Colors

Colour comes from a single field, the sky. The card is near-black glass, the text is white at two strengths, and there is one yellow-green signal accent.

### Primary
- **Signal Yellow** (`accent-signal`): marks the next thing and nothing else. Used for the next prayer's name, its time and name on the bar's scale, the bar's filled run up to now, the Hijri chip's text, and the footer's "Besok: …" fast line. Every placement points forward.

### Neutral
- **On-Sky White** (`on-sky`): the answer text. Times, the countdown, the date in the crumb, the footer's "now" line.
- **Muted White** (`muted-white`, 65% white): legends and context. The mono labels, the place in the crumb, scale stops that are not next, the short countdown on 1- and 2-column sizes, the Hijri line on narrow sizes, and the footer icon's tint.
- **Smoked Card** (`smoked-card`, #0E1218 at 66%): the glass the text sits on. Its darkness is set so muted text and the accent pass AA over every sky.
- **Card Hairline** (`card-hairline`): a 1dp stroke just inside the card's edge.
- **Header Divider** (`header-divider`): a 1dp rule under the header row on Card layouts (3 to 5 columns, 2 to 3 rows).
- **Bracket White** (`bracket-white`): 1.5dp round-capped corner brackets.
- **Track Dash** (`track-dash`): the dashed part of the bar, the time still to come.
- **Chip Fill** (`chip-fill`, the accent at 15%): the Hijri chip's ground.

### The Sky (time-of-day field)
The sky is a four-stop vertical gradient (stops at 0, 0.42, 0.78, 1) with one soft elliptical glow and faint seeded stars. It has seven looks keyed to the prayer times and blended linearly between keys: Night, Predawn, Dawn, Morning, Noon, Golden, Dusk. The exact stops and glows are in the sidecar (`extensions.sky`). The sky edge carries a 1dp rim that fades from 25% white at the top to 5% at the bottom. The sky has no sun, moon or clouds, and every look is dark enough for white text at 4.5:1 (enforced by `SkyContrastTest`).

### Named Rules
**The One Signal Rule.** The accent marks what is next and nothing else. A new element gets the accent only if it points forward in time.

**The Glass Not Paint Rule.** The card is translucent smoke over the sky (`smoked-card`) and never an opaque fill. The sky must stay visible round the card and through it.

## Typography

**Content Font:** Google Sans Flex (system font name `google-sans-flex`; on phones without it the launcher falls back to the system sans), variable weight set through `wght`, tabular figures (`tnum`).
**Label/Mono Font:** the system `monospace`, letter-spaced 0.06em, uppercase for legends.

**Character:** A soft geometric sans gives the answer, and a typewriter mono gives the legend, so a value and its label never share a voice. The pairing is forced by RemoteViews, which allows system fonts only. The app's own Plus Jakarta Sans cannot load in a widget.

All sizes are sp, so the system font size still applies. Each layout multiplies them by the grid scale k (1 to 1.35 on large cells) and then by a fit factor (at most 1) that shrinks the type until every line fits.

### Hierarchy
- **Numeral** (Medium 500): the next prayer's time, which is always the largest thing in the card. A 12-hour "AM/PM" is set at 0.7×.
- **Countdown** (Medium 500): the time left in words ("6 jam 25 menit"). It is always smaller than the numeral and right-aligned on the numeral's baseline in Card layouts.
- **Prayer name** (SemiBold 600, accent): the next prayer. Where there is room it reads "Shalat Jumat" on Fridays.
- **Body** (Regular 400): the crumb ("Jakarta / Sen, 26 Okt", with the place muted), dates, the footer.
- **Label** (mono, uppercase): "BERIKUTNYA", "TIBA DALAM", and the Hijri chip (in mixed case).
- **Scale** (mono): the bar's stop times, plus prayer names above them at 8.5sp where shown.

### Sizes per size class (base sp before k and fit)
| Size class | Name | Time | Countdown | Labels / other |
|---|---|---|---|---|
| 1×1 | 11 SemiBold | 22 | (none) | (none) |
| 2×1 | 11 SemiBold | 28 | 10 Regular muted, short form | date line 10 muted |
| 3–5×1 | 11 SemiBold | 28 (22 when tight, under 70dp tall) | 14 Medium | TIBA DALAM 9; chip 10; date 11 |
| 1×2, 1×3 | 12 SemiBold | hours and minutes stacked, 18–44 each | 11 muted, short form | date 11; Hijri 10 muted, up to 3 lines |
| 2×2, 2×3 | 13 SemiBold | 34 × 0.5–1.5 by spare height | 12 (×1.5 on 2×3) | labels 9.5; date 12 Medium; chip 10; footer 11 |
| 3×2, 3×3 | 13 SemiBold | 34 / 44 | 13 | labels 9.5; crumb 12; chip 10; scale 9 (names 8.5 on 3×3) |
| 4–5×2, 4–5×3 | 13 SemiBold | 40 / 52 | 15 / 17 | as 3×; scale names on 5× and on 3-row sizes; footer 11 |

On 3-row Card sizes, spare height scales the time by up to 2×, and the countdown by half that growth.

### Named Rules
**The Two Voices Rule.** Values are set in Google Sans Flex and legends in letter-spaced mono. A legend is never sans and a value is never mono, except the scale's times, which are the instrument's own markings.

**The No Ellipsis on the Answer Rule.** Times, prayer names, the countdown, the Hijri chip and footer lines shrink to fit and never end in "…". Only dates and the place may shorten. `WidgetFitTest` enforces this for every size, both languages, 12- and 24-hour clocks, through the day, and at large system fonts.

## Layout

The widget is a grid of size classes. Columns break at widths of 110, 195, 290 and 400dp. Rows break at heights of 150dp and then 250dp (215dp for 4 or more columns). Both are measured after dividing by the font scale. Each class has its own layout built from the same parts: OneByOne, TwoByOne, WideOne (3 to 5 columns × 1 row), Narrow (1 column), Square (2 columns), and Card (3 to 5 columns × 2 to 3 rows, the reference layout).

Spacing is one shared object, so edges and gaps line up across sizes:
- **Inset** (`inset`, or `inset-small` on one-row and one-column widgets): the sky margin round the card.
- **Pad** (`pad` on Card and WideOne, `l` on Square, `pad-short` vertical on 2×1, `pad-small` on 1×1 and Narrow): the text's padding inside the card. On Card layouts the top padding is `l` and the bottom padding is `pad`.
- **Steps** `xs`, `s`, `m`, `l`: the gaps between parts. Label to value gaps use `s`. Name to time gaps use `xs`. Header to divider uses `m`. Gaps around the next-prayer block are `l + s` on tall cards and flexible with a minimum of `m` on 2-row cards.
- **Bar height**: `bar` on Square and Card layouts, `bar-thin` elsewhere.

Spare height goes to the figures first and to gaps last. Tall cards keep one rhythm from header to footer and centre the group in the card. Under pressure, parts drop out in this order: the footer, then the scale's names, then the scale, and only then does type shrink.

### Named Rules
**The Figures Take the Room Rule.** Spare space goes to the time and the countdown, never to wider padding or bigger gaps.

## Elevation & Depth

There are no shadows. Depth comes from translucency: the sky behind, the smoked card in front, and the text on top. A 1dp hairline marks the card's edge, and white brackets in the margin frame the card from outside so they never crowd its text. A soft rim on the sky's own edge separates the widget from the wallpaper.

### Named Rules
**The Smoke Not Shadow Rule.** Depth comes from the card's translucency and its hairline. No drop shadows and no lifted layers.

## Shapes

The sky's corners follow the launcher's widget radius (`system_app_widget_background_radius` on Android 12+, otherwise `sky-fallback`). The card's radius is the sky's radius minus the inset, with a 2dp minimum, so the two curves stay concentric. Brackets trace each corner's curve halfway into the margin and extend 4dp straight on both sides. The chip is nearly square-cornered (`chip`). The bar's filled run has a 1dp radius, and its track is 2dp dashes with 2dp gaps. The header divider is a full-width 1dp rule.

## Components

### Card (signature)
- **Corner Style:** concentric with the sky (see Shapes).
- **Background:** `smoked-card` with a `card-hairline` stroke, and bracket corners in `bracket-white` drawn in the inset margin.
- **Shadow Strategy:** none (see Elevation & Depth).
- **Internal Padding:** `pad` at the sides and bottom, `l` at the top on Card layouts.
- The card, brackets, sky and bar are painted into bitmaps (sky at up to 1.5× density). Only text and the chip are views.

### Header
- **Crumb:** the place in `muted-white`, then " / ", then the date in `on-sky`, set in Body at 12sp. It takes the remaining width and may shorten with "…".
- **Chip:** the Hijri date on the right. The crumb shortens before the chip does. On Card layouts a `header-divider` rule sits `m` below the row.

### Chips
- **Style:** mono text in `accent-signal` on `chip-fill`, padding 3dp × 6dp, `chip` radius. No border.
- **State:** one variant only. It holds the Hijri date: "12 Jumadilawal" short, or "12 Jumadilawal 1448 H" on 5× and 3-row sizes.

### NextLabel
- "BERIKUTNYA" in mono `muted-white`, a 6dp gap, then the prayer name in SemiBold `accent-signal`. On Square and Card layouts it sits above the time. Smaller sizes drop the legend and keep only the accent name.

### Countdown block
- "TIBA DALAM" in mono `muted-white` above the countdown in Medium `on-sky`, right-aligned and bottom-aligned with the time on Card layouts. Square puts it under the time (inline on 2×2, stacked and enlarged on 2×3). Small sizes use a short muted form ("3j 10m").
- **Behaviour:** the text updates every minute through `WidgetTicker`, an exact non-waking alarm on the minute that waits while the screen is off. Without the exact-alarm permission the tick runs in a 10-minute window, and the countdown is rounded to ten minutes and prefixed "±". Minutes round up, so it reads "1 menit" until the prayer comes in.

### Bar
- Filled in `accent-signal` up to now, with `track-dash` dashes after. With a scale, the fill is interpolated between the stops, which sit under the centre of each scale time. Without one, the stops are evenly spaced.
- **Day and night:** by day the stops are the five prayers. From Isya until Subuh, the bar runs from Isya to Subuh with two stops. The header keeps today's date after Isya, even though the next prayer is tomorrow.

### Scale
- The bar's stop times in mono `muted-white`, with the next one in `accent-signal`. Prayer names sit above at 8.5sp where there is room. The first stop is start-aligned, the last end-aligned, and the rest centred. The type shrinks to keep 6dp between items, and the scale drops out once it would shrink below 0.75× (with more than two stops).

### Footer (3-row sizes)
- On the left, a 12dp Material icon tinted `muted-white` with the "now" line in Body `on-sky` ("Dhuha hingga 11.30"). On the right, tomorrow's sunnah fast in `accent-signal`. It shows only what fits whole: both lines, else the fast, else the "now" line, else nothing.

### Empty and handover states
- With no location, or after the move to the new app, the widget shows the Night sky without a card and one centred 13sp Body message (up to 4 lines, 16dp side padding).

## Do's and Don'ts

### Do:
- **Do** give the accent only to what is next: the name, its scale stop, the bar's fill, the chip, tomorrow's fast.
- **Do** set values in Google Sans Flex with tabular figures and legends in letter-spaced mono caps.
- **Do** shrink type through the fit factor before letting a time, name, countdown, chip or footer line ellipsize, and keep `WidgetFitTest` green.
- **Do** spend spare height on the time (up to 2× on 3-row Card sizes) and the countdown, not on gaps.
- **Do** keep every sky look dark enough for white text at 4.5:1 (`SkyContrastTest`).
- **Do** take every gap and padding from the shared spacing steps so edges line up between sizes.

### Don't:
- **Don't** list the five times as a table or list. They appear only as the bar's scale.
- **Don't** let the countdown reach the size of the time.
- **Don't** draw a sun, moon or clouds in the sky. Text sits anywhere on it.
- **Don't** make the card opaque or add drop shadows. Depth comes from smoke and the hairline.
- **Don't** apply the app's Material 3 palettes or `docs/design-tokens.md` colours to the widget, or the widget's tokens to app screens.
