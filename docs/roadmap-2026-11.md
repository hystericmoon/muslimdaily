# Decisions of 18 Nov 2026 (free features, motion, third accent, island)

What the owner decided in the 16-18 Nov session, with the design pages (private to the owner,
so the facts are repeated here). Status lives in `docs/handover.md`; this file is the record.

## The owner's blanket yes (18 Nov)

"I approve everything, free features, motion, third accent." Taken as: yes to everything on the
pages below, including the new permission for silence-during-prayer and the version numbers
proposed. Still ask at merge, as CLAUDE.md requires, for anything that changes shipped religious
content, `UserDatabase` or `BackupRepository`, or adds a dependency: that covers the zakat calculator
and the missed-prayers log. State the version before each release.

## Island (done)

Asked for "like the Dynamic Island" (a status-bar chip was not what they meant). Shipped: 1.20.0
(#295, #296) a pill at the camera cutout with the next prayer, a big card 10 minutes before each
prayer; shipped after it in 1.21.0 (#298): Island settings (height, inner padding, text size, camera
gap adjust, X/Y offset, opacity, right side countdown or time, big card on/off and lead 5-30,
hide in landscape, live preview), the hardware-acceleration fix (the overlay drew in software and
expanded slowly), the animation that no longer resizes the window, and equal-width sides so the
camera sits mid-pill. Design pages: https://claude.ai/artifact/FTKfGfXmvb8i7fLCN4qLwW,
https://claude.ai/artifact/HEW2akJz6amou8pXG4vYXC. Phone checks are the owner's (handover).
Island ideas not built: Listen controls, after-salah shortcut, Build position, travel prompt.

## Free features to build (page: https://claude.ai/artifact/W8R711diCHQKFguwuNZVcs)

Features other prayer apps charge for, filtered by Adwam's rules (offline, no gamification, no
religious text written by a model). Already in the app, so not repeated: Qibla, fasting calendar,
Ramadan suhur and iftar countdowns, Hijri calendar, widgets, memorisation, island.

| # | Feature | Notes | Model |
|---|---|---|---|
| 1 | Monthly timetable and Imsakiyah (Ramadan), share as image or PDF | Computed on the phone, no data, no permission. Most shared feature in Indonesia. Share through the existing FileProvider. | Sonnet |
| 2 | Silence the phone during prayer | Starts at the adhan, lasts N minutes, then restores. Needs Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`, special access the user grants once): approved by the owner. | Sonnet |
| 3 | Saved places (home, work, other) with one-tap switch or follow GPS | Builds on location and travel mode. | Sonnet |
| 4 | Dhikr counter | A neutral tally with haptic taps and a target the user sets. No streaks, points or wording of its own. | Sonnet |
| 5 | Last third of the night | Time arithmetic on today's times; any explanatory text needs a source. | Sonnet |
| 6 | Islamic dates with reminders | Names and dates only, calculated like the fasting calendar. | Sonnet |
| 7 | Zakat calculator | The nisab and the rate are rulings: they must come from a cited source (content rules), prices typed by the user. Needs a source first. | Opus work (done on Sonnet while no Opus budget) |
| 8 | Missed-prayers log | User data: `UserDatabase` auto-migration, exported schema, `BackupRepository` VERSION bump. Counts down, never up. | Opus work |

Left out: premium adhan voices (needs licensed audio, licence first), a full Qur'an reader
(content and licensing, a separate project), anything that needs internet, streaks and badges.

## Release plan (versions are stated before each release)

- **1.21.0** (shipped 18 Nov, the lightest push: it was already on main): Island settings, fast
  expand, centred camera gap. The third accent and the first motion pieces moved to 1.22.0.
- **1.22.0**: the third accent on Today (colours only first) and the first motion pieces.
- **1.23.0**: monthly timetable and Imsakiyah. **1.24.0**: saved places and the dhikr counter.
  **1.25.0**: silence during prayer. Then 5 and 6; 7 and 8 when sourced or designed.

## Motion (skill: `.claude/skills/motion/`)

Four speeds (90, 160, 260, 420 ms), three curves, springs only for physical things, transform and
alpha only, Reduce motion honoured, no idle animation, Arabic text never moves, no gamified
effects, at most one signature motion per screen. Order: Today ring and sliding highlight, mark as
read, Qibla, screen transitions, staggered empty states, Listen highlight glide. Page:
https://claude.ai/artifact/1tcpHAeEbosZC7pgrSG768.

## Third accent (details: `docs/accent.md`, generator `tools/accent_palette.py`)

Per-palette accent hues: Teal 295 (iris violet), Navy 130, Forest 325, Slate 200, Olive 350, Rose 235,
Lavender 165, Peach 265. Used on Today for the countdown, a thin progress line, the "Now" chip and
the next-row countdown. Method: the 60:30:10 rule (surface 60, palette 30, accent 10).

## Still open

- **Prayer-time accuracy**: the owner never said where they saw "province based" (asked twice; they
  answered "no preference"). Options and the recommendation (show "GPS, exact" on Today plus a dev-only
  comparison script against eQuran; kecamatan picker and coordinates entry only if the script shows
  manual picks off by more than 2 minutes): https://claude.ai/artifact/G4ousEcKajSi1rQgexYDay.
- **LinkedIn post for 1.19**: waits for the owner's device screenshots of Build, Routine and Recall;
  the Arabic is to be unblurred. The old scratchpad assets are gone.
- Owner phone checks: island (pill at the camera, expand, the 10-minute card, battery killer), the
  faster expand, the camera gap, each Island settings slider moving the real island.
