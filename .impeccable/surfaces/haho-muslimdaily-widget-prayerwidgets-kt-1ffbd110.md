---
version: 1
slug: "haho-muslimdaily-widget-prayerwidgets-kt-1ffbd110"
primary_target: "app/src/main/java/id/haho/muslimdaily/widget/PrayerWidgets.kt"
related_targets: []
---

# Surface: home-screen prayer widget

Mode: Operate (a glance surface). Audience: Indonesian Muslims checking the next prayer from the home or lock screen.
Job: which prayer is next, its time, how long until it, and today's Gregorian and Hijri date. Every size 1×1 to 5×3; lock-screen capable; the time-of-day sky stays (owner).
Constraints: RemoteViews (system fonts only, no free drawing; card and bar are painted bitmaps), WCAG AA, 200% font without clipping, Indonesian first.

## Direction contract

THESIS: A smoked-glass instrument card laid over the living sky, owner's reference (dark card on a blurred photo, mono caps labels, yellow progress bar). Refuses the category default of a gradient card with a list or table of times.
OWN-WORLD: Card #0E1218 at 66% over the time-of-day sky, inset a hairline (1.5dp), hairline edge; text white, labels mono caps at 65% white; one accent #EEF26B (next prayer's name, its scale label, the bar's fill, the Hijri chip on a 15% accent fill). Google Sans Medium for numbers, system monospace for labels.
STORY: The visitor sees the next prayer's name in the accent, its time large, "TIBA DALAM" and the time left small beside it, and how far the day has gone on the bar; the dates sit quietly in the header.
FIRST VIEWPORT: 4×2: header row "Jakarta / Sen, 26 Okt" left, Hijri chip right, hairline; "BERIKUTNYA" + name in accent; time at 40sp left, "TIBA DALAM" over "6 jam 25 menit" at 15sp right-aligned on the time's baseline; bar (accent fill, dashed rest) with the five times as its scale, next one in accent. At night the bar runs Isya to Subuh.
FORM: Owner-pinned reference after two rolls and two sketches were declined (form: their "signal strength" card, adapted). seed:486a7d628f3723ad
Signature interaction: the countdown in words updates each minute (non-waking exact tick) and the bar fills through the day.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
