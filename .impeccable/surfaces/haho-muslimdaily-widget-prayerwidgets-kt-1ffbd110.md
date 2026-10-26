---
version: 1
slug: "haho-muslimdaily-widget-prayerwidgets-kt-1ffbd110"
primary_target: "app/src/main/java/id/haho/muslimdaily/widget/PrayerWidgets.kt"
related_targets: []
---

# Surface: home-screen prayer widget

Mode: Operate (a glance surface). Audience: Indonesian Muslims checking the next prayer from the home or lock screen.
Job: the time now, which prayer is next and its time, how long until it, and today's Gregorian and Hijri date. Every size 1×1 to 5×3; lock-screen capable. The time-of-day sky was replaced by translucent black glass with a sun or moon (owner, 2026-10-26).
Constraints: RemoteViews (system fonts only, no free drawing; glass, sun, moon and bar are painted bitmaps; the clock is a TextClock), WCAG AA, 200% font without clipping, Indonesian first.

## Direction contract

THESIS: A clock under translucent black glass, owner's revision of the instrument card: the large figure is the current time, because people read the next prayer's time as the clock. Refuses the category default of a gradient card with a list or table of times.
OWN-WORLD: Black glass at 74% over the wallpaper, faint sheen, lit top edge; text white, muted text at 65% white; one accent #EEF26B (the next prayer and its time, its scale stop, the bar's fill, tomorrow's fast). Google Sans Flex Bold for the clock, Medium for the countdown line, system monospace for the scale. Sun and moon from NASA imagery (SDO photo; LROC map lit for the phase).
STORY: The visitor sees the time now, large; under it "2 jam 47 menit menuju Maghrib 17.51" with the prayer in the accent; the sun or tonight's moon on the right; how far the day has gone on the bar, with the prayers' names and times as its scale.
FIRST VIEWPORT: 4×2: header "Jakarta / Sen, 26 Okt" left, "15 Jumadilawal" right in the same style, hairline; clock at 38sp left; countdown line at 15sp under it; sun or moon right, centred on the clock block; bar (accent fill, dashed rest) with names over the five times, next one in accent. At night the bar runs Isya to Subuh.
FORM: Owner's mockup (2026-10-26), two rounds: no "Waktu sekarang" label, Hijri date styled like the date, translucent glass, bolder clock, names on the scale.
Signature interaction: the clock ticks on its own (TextClock); the countdown updates each minute (non-waking exact tick) and the bar fills through the day.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance (widget_sun.jpg: NASA/SDO HMI continuum, 2026-10-26; widget_moon_map.jpg: NASA SVS CGI Moon Kit lroc_color_2k).
