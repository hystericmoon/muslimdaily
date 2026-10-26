---
name: Adwam home-screen widget
description: Translucent black glass over the wallpaper; the time now, the time left until the next prayer, the sun or the moon, and how far the day has gone.
colors:
  accent-signal: "#EEF26B"
  on-glass: "#FFFFFF"
  muted-white: "#FFFFFFA6"
  glass: "#000000BD"
  glass-sheen: "#FFFFFF0F"
  glass-edge: "#FFFFFF66"
  header-divider: "#FFFFFF1A"
  track-dash: "#FFFFFF38"
typography:
  clock:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "38sp"
    fontWeight: 700
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
    fontSize: "11sp"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  body:
    fontFamily: "google-sans-flex, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: 1
    fontFeature: "tnum"
  scale:
    fontFamily: "monospace"
    fontSize: "9sp"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0.06em"
rounded:
  bar: "1dp"
  widget-fallback: "24dp"
spacing:
  xs: "2dp"
  s: "4dp"
  m: "8dp"
  l: "12dp"
  pad: "14dp"
  pad-short: "10dp"
  pad-small: "8dp"
  bar: "5dp"
  bar-thin: "4dp"
components:
  glass:
    backgroundColor: "{colors.glass}"
    padding: "12dp 14dp 14dp"
  clock:
    textColor: "{colors.on-glass}"
    typography: "{typography.clock}"
  countdown-line:
    textColor: "{colors.on-glass}"
    typography: "{typography.countdown}"
  countdown-target:
    textColor: "{colors.accent-signal}"
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

**Scope.** This file governs one surface: the home-screen and lock-screen prayer widget (`app/src/main/java/id/haho/muslimdaily/widget/`, the `widget_text*` and `widget_clock` layouts, and the `widget_sun` and `widget_moon_map` images). The app's own screens keep their established Material 3 system: palettes, surfaces, gold and contrast tables live in `docs/design-tokens.md`, which this file neither restates nor replaces. Nothing here applies to app screens, and nothing in `docs/design-tokens.md` applies to the widget.

## Overview

**Creative North Star: "A Clock Under Glass"**

The widget is a pane of translucent black glass over the wallpaper. The largest thing on it is the time now, a live clock in bold. Under it one line says how long until the next prayer and names that prayer and its time in yellow: "2 jam 47 menit menuju **Maghrib 17.51**". On the right, the sun by day or the moon at its real phase by night. A bar under them shows how far the day (or the night) has gone, with the prayers' names and times as its scale.

The large figure used to be the next prayer's time, which people read as the current time. Now the clock is the clock, and a prayer time only ever appears with its name beside it.

**Key Characteristics:**
- Translucent black glass (74%), a faint sheen from the top left and a lit top edge. The wallpaper shows through; launchers cannot blur it.
- One accent (`accent-signal`), used only for what is next: the prayer and its time in the countdown line, its stop on the scale, the bar's filled part, and tomorrow's sunnah fast.
- The current time is a TextClock the launcher keeps, so it is right every minute between widget updates.
- The sun and the moon are drawn from NASA imagery, the moon lit for tonight's phase.
- Text never ellipsizes where it carries the answer; it shrinks first.

## Colors

The colour comes from the wallpaper behind the glass, the sun or moon, and one yellow-green signal accent. The text is white at two strengths.

### Primary
- **Signal Yellow** (`accent-signal`): marks the next thing and nothing else. The next prayer and its time in the countdown line (and the name on 1- and 2-column sizes), its name and time on the bar's scale, the bar's filled run up to now, and the footer's "Besok: …" fast line.

### Neutral
- **On-Glass White** (`on-glass`): the clock, the countdown, the dates (Gregorian and Hijri, the same style), the footer's "now" line.
- **Muted White** (`muted-white`, 65% white): the place in the crumb, scale stops that are not next, the short countdown on small sizes, the 12-hour marker on narrow sizes, and the footer icon's tint.
- **Glass** (`glass`, black at 74%): the widget's ground. Set so muted text still passes AA where the wallpaper behind is pure white, sheen included (`GlassContrastTest`).
- **Glass Sheen** (`glass-sheen`): a diagonal gradient from 6% white at the top left to 2% across the middle and 3% at the bottom right.
- **Glass Edge** (`glass-edge`): a 1dp stroke round the edge, 40% white at the top fading to 12% by a third of the way down and 8% at the bottom.
- **Header Divider** (`header-divider`): a 1dp rule under the header on Card layouts.
- **Track Dash** (`track-dash`): the dashed part of the bar, the time still to come.

### Named Rules
**The One Signal Rule.** The accent marks what is next and nothing else. A new element gets the accent only if it points forward in time.

**The Glass Not Paint Rule.** The glass is translucent and never opaque; the wallpaper must show through it. Contrast is held by its darkness, measured over a white wallpaper, not by making it solid.

## Typography

**Content Font:** Google Sans Flex (system font name `google-sans-flex`; on phones without it the launcher falls back to the system sans), variable weight set through `wght`, tabular figures (`tnum`).
**Scale Font:** the system `monospace`, letter-spaced 0.06em.

The pairing is forced by RemoteViews, which allows system fonts only. The app's own Plus Jakarta Sans cannot load in a widget.

All sizes are sp, so the system font size still applies. Each layout multiplies them by the grid scale k (1 to 1.35 on large cells) and then by fit factors (at most 1) that shrink the type until every line fits, re-measured at each step because Android 14 scales small and large type differently.

### Hierarchy
- **Clock** (Bold 700, optical size 144 where the font has the axis; `textStyle="bold"` for the fallback font): the time now, always the largest thing. A 12-hour "AM/PM" sits beside it at 0.45×, 4dp away. Fitted to its widest possible value (every figure the widest figure), so no time it ticks to can outgrow its room.
- **Countdown line** (Medium 500): "2 jam 47 menit menuju Maghrib 17.51", the prayer and its time in the accent. One line where it fits at 90% or more, else two ("2 jam 47 menit" / "menuju Maghrib 17.51").
- **Prayer name** (SemiBold 600, accent): on 1- and 2-column sizes, the next prayer (with its time on 2×1).
- **Body** (Regular 400): the crumb ("Jakarta / Sen, 26 Okt", the place muted), the Hijri date in the same style, the footer.
- **Scale** (mono): the bar's stop times, with the prayer names above them at 8.5sp where there is room.

There are no legend labels: "BERIKUTNYA" and "TIBA DALAM" were removed with the chip.

### Sizes per size class (base sp before k and fit)
| Size class | Clock | Countdown | Other |
|---|---|---|---|
| 1×1 | 22 | name 11 SemiBold accent; short form 10 muted | bar where it fits |
| 2×1 | 28 | "Maghrib 17.51" 11 accent; short form 10 muted | date and Hijri 10 muted |
| 3–5×1 | 28 (22 when tight, under 70dp tall) | 14 Medium over "menuju …" 12; tight: target and short form 11 | 4+: date over Hijri 11 |
| 1×2, 1×3 | hours over minutes, 18–44 each | name 12; short form 11 muted | 3 rows: date 11, Hijri 10 muted, up to 3 lines |
| 2×2, 2×3 | 34 × 0.5–1.5 by spare height | 12 Medium over "menuju …" 12 | date and Hijri 12 Medium; footer on 2×3 |
| 3×2, 3×3 | 34 / 44 | 13 | crumb and Hijri 12; scale 9, names 8.5 where they fit |
| 4–5×2, 4–5×3 | 38 / 52 | 15 / 17 | as 3×; long Hijri date on 5× and 3-row sizes |

On 3-row Card sizes, spare height scales the clock by up to 2×, and the countdown by half that growth.

### Named Rules
**The Clock Is the Clock Rule.** The large figure is always the current time. A prayer time is never shown large or alone: it always sits beside its prayer's name.

**The No Ellipsis on the Answer Rule.** The clock, times, prayer names, the countdown line, the Hijri date and footer lines shrink to fit and never end in "…". Only the Gregorian date and the place may shorten. `WidgetFitTest` enforces this for every size, both languages, 12- and 24-hour clocks, through the day, and at large system fonts.

## Layout

The widget is a grid of size classes. Columns break at widths of 110, 195, 290 and 400dp. Rows break at heights of 150dp and then 250dp (215dp for 4 or more columns). Both are measured after dividing by the font scale. Each class has its own layout built from the same parts: OneByOne, TwoByOne, WideOne (3 to 5 columns × 1 row), Narrow (1 column), Square (2 columns), and Card (3 to 5 columns × 2 to 3 rows, the reference layout).

Spacing is one shared object, so edges and gaps line up across sizes:
- **Pad** (`pad` on Card and WideOne, `l` on Square, `pad-short` vertical on 2×1, `pad-small` on 1×1 and Narrow): the text's padding inside the glass. On Card layouts the top padding is `l` and the bottom padding is `pad`.
- **Steps** `xs`, `s`, `m`, `l`: the gaps between parts. Clock to countdown is `xs`. Header to divider is `m`. The gaps around the clock block are `l + s` on tall cards and flexible with a minimum of `m` on 2-row cards.
- **Bar height**: `bar` on Square and Card layouts, `bar-thin` elsewhere.

On Card layouts the scale's names and the footer are decided by the height there is, not the size class (Samsung's 2-row cells, about 350×200dp, are taller than Pixel's 340×164dp). Short cards give the names priority: the countdown line takes one line if it can (with "Jumat" rather than "Shalat Jumat" if that is what fits), and the clock gives up to 15% of its size, before the names drop out. Under pressure, parts drop out in this order: the footer, then the scale's names, then the scale, and only then does type shrink.

### Named Rules
**The Figures Take the Room Rule.** Spare space goes to the clock and the countdown, never to wider padding or bigger gaps.

## Elevation & Depth

There are no shadows. Depth comes from translucency: the wallpaper behind, the glass over it, the text and the sun or moon on top. The glass's lit top edge and its sheen are the only signs of a surface.

### Named Rules
**The Glass Not Shadow Rule.** Depth comes from the glass's tint, sheen and edge. No drop shadows and no lifted layers.

## Shapes

The glass's corners follow the launcher's widget radius (`system_app_widget_background_radius` on Android 12+, otherwise `widget-fallback`), and it fills the whole widget. The bar's filled run has a 1dp radius, and its track is 2dp dashes with 2dp gaps. The header divider is a full-width 1dp rule.

## Components

### Glass (signature)
- **Background:** `glass`, `glass-sheen`, `glass-edge`, painted into one bitmap at up to 2× density.
- **Shadow Strategy:** none.
- **Internal Padding:** `pad` at the sides and bottom, `l` at the top on Card layouts.

### Header (Card layouts)
- **Crumb:** the place in `muted-white`, then " / ", then the date in `on-glass`, set in Body at 12sp. It takes the remaining width and may shorten with "…".
- **Hijri date:** on the right, in the same Body style as the date: "15 Jumadilawal" short, or "15 Jumadilawal 1448 H" on 5× and 3-row sizes. The crumb shortens before it does. A `header-divider` rule sits `m` below the row.

### Clock
- A TextClock (`widget_clock`) with both formats set to the app's pattern ("HH.mm" in Indonesian, "HH:mm" in English, "h.mm"/"h:mm" with a separate smaller "a" for 12-hour), in the location's time zone. Tests and the picker preview draw it once, from the moment rendered (`WidgetData.liveClock = false`).

### Countdown line
- See Typography. It updates every minute through `WidgetTicker`, an exact non-waking alarm on the minute that waits while the screen is off. Without the exact-alarm permission the tick runs in a 10-minute window, and the countdown is rounded to ten minutes and prefixed "±". Minutes round up, so it reads "1 menit" until the prayer comes in.

### Sun and moon (Card layouts)
- Shown from sunrise to Maghrib (the sun) and from Maghrib to sunrise (the moon), on the right of the clock block, centred on it. The disc is about the clock's line height, at most 28% of the card's inner width; its square, with the glow, is the disc over 0.6, and its right edge lines up with the text's right edge. The text keeps `l` clear of it.
- **Sun:** NASA/SDO's visible-light (HMI continuum) photo of the disc, which shows the granulation. That product has limb darkening removed, so it is put back (I = 1 − 0.32(1 − μ)), warmer at the centre and redder at the limb, with a soft orange glow that starts no brighter than the limb.
- **Moon:** NASA's CGI Moon Kit colour map (LRO LROC) wrapped on a sphere with the near side facing us, mostly greyscale with a little of the map's colour, its contrast raised (gamma 1.8) to look as the moon does at night. Lit from the sun's direction for the phase with Lommel–Seeliger shading, so it is nearly flat at full and falls off at the terminator, with a 3% earthshine on the unlit part. The phase comes from the mean synodic month (`Moon`), not the Hijri date, so a calendar adjustment never changes it. The glow grows with how much is lit.
- Credits are in Settings → About.

### Bar
- Filled in `accent-signal` up to now, with `track-dash` dashes after. With a scale, the fill is interpolated between the stops, which sit under the centre of each scale time. Without one, the stops are evenly spaced.
- **Day and night:** by day the stops are the five prayers. From Isya until Subuh, the bar runs from Isya to Subuh with two stops. The header keeps today's date after Isya, even though the next prayer is tomorrow.

### Scale
- The bar's stop times in mono `muted-white`, with the next one in `accent-signal`, and the prayer names above them at 8.5sp where the card's height has room. The first stop is start-aligned, the last end-aligned, and the rest centred. The type shrinks to keep 6dp between items, and the scale drops out once it would shrink below 0.75× (with more than two stops).

### Footer (Card and 2×3 sizes, where the height has room)
- On the left, a 12dp Material icon tinted `muted-white` with the "now" line in Body `on-glass` ("Dhuha hingga 11.30"). On the right, tomorrow's sunnah fast in `accent-signal`. It shows only what fits whole: both lines, else the fast, else the "now" line, else nothing.

### Empty and handover states
- With no location, or after the move to the new app, the widget shows the glass alone and one centred 13sp Body message (up to 4 lines, 16dp side padding).

## Do's and Don'ts

### Do:
- **Do** make the large figure the current time, and put every prayer time beside its name.
- **Do** give the accent only to what is next: the prayer and its time, its scale stop, the bar's fill, tomorrow's fast.
- **Do** shrink type through the fit factors before letting the clock, a time, name, countdown, Hijri date or footer line ellipsize, and keep `WidgetFitTest` green.
- **Do** keep the glass dark enough for muted text at 4.5:1 over a white wallpaper (`GlassContrastTest`).
- **Do** take every gap and padding from the shared spacing steps so edges line up between sizes.

### Don't:
- **Don't** list the five times as a table or list. They appear only as the bar's scale.
- **Don't** let the countdown reach the size of the clock.
- **Don't** make the glass opaque or add drop shadows.
- **Don't** let text run under the sun or the moon.
- **Don't** apply the app's Material 3 palettes or `docs/design-tokens.md` colours to the widget, or the widget's tokens to app screens.
