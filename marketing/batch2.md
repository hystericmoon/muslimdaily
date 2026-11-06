# Adwam promo, batch 2: plan

Same rules and look as `brief.md` (owner, 5 Nov 2026): no religious text, no faces, claims the app
proves only, no streak or points language, silent video, Arabic only as empty hidden-word boxes.
The owner posts; nothing is posted from a session. English media uses the English-UI shots
(`PromoEnglishScreenshotTest`, 150-157); more EN shots get added the same way.

Step 1 (done 6 Nov): English-UI media for batch 1 (stills, carousel PDFs, 4:5 and 9:16 videos).

## Topics, claims and proof

| # | Topic | Claim | Proof in the app | Screens |
|---|---|---|---|---|
| W | Widgets | Next prayer, countdown and day bar on the home screen; Hijri date; 15 sizes (1x1 to 5x3); EN 12-hour | Widget shots 80-89, 125 | none have Arabic; ready as is |
| R | Reminders | Per prayer: Off, Sound or Silent. Honest limit: without the exact-alarm permission, reminders and the widget can be a few minutes late | `43-settings-reminders`, `117-today-widget-alarm-note`; strings `reminders_exact_permission`, `today_widget_alarm_note` | no Arabic |
| N | Routines (Rangkaian) | Build your own sequence of duas and run them one per screen; counts only resume today's session, no history or statistics | `111`-`113`, `124`, `31`-`33`; string `run_private_note` | 111, 113, 31 show Arabic: need `prep_screens.py` box regions; 112 shows placeholder text "[Teks contoh...]", so re-record or erase it before use |
| H | How it's built (LinkedIn) | Kotlin, Compose, Hilt, Room, DataStore, Glance; no network; screenshot tests on CI; the promo engine is in the repo | `CLAUDE.md`, `docs/architecture.md`, `marketing/README.md` | one image: the content-to-UI flow from `docs/architecture.md`, redrawn |

## Assets (4:5 unless noted; ID for Instagram, EN for LinkedIn)

- W: Video 3 "Di layar utama" (~15 s: widget through the day, moon to sun, sizes), carousel C
  (5 slides: glance, sizes, day and night, English 12 h, exact-alarm note), 2 images.
- R: carousel D (4 slides: per-prayer choice, the exact-alarm limit said plainly, silent on Friday
  note, end card), 1 image.
- N: carousel E (5 slides) after the Arabic regions are set and checked by eye; 1 image.
- H: one LinkedIn text post plus the flow image; no Instagram version.

## Draft calendar (same cadence as batch 1: Instagram Mon/Thu 19:30 and Sat 09:00, LinkedIn Tue 08:30)

| Week | Mon | Tue | Thu | Sat |
|---|---|---|---|---|
| 5 (from 7 Dec) | Video 3 Reel (W) | LinkedIn: Video 3 EN | Carousel C (W) | Image W1 |
| 6 | Carousel D (R) | LinkedIn: Carousel C EN | Image R1 | Image W2 |
| 7 | Carousel E (N) | LinkedIn: Carousel D EN | Image N1 | Carousel C repeat or owner's pick |
| 8 | Owner's pick | LinkedIn: "how it's built" (H) | Owner's pick | Owner's pick |

## Open for the owner

- Topic order and whether routines (N) goes in; it needs the most prep work.
- Caption text: drafted after the order is chosen (Indonesian for Instagram, English for LinkedIn).
