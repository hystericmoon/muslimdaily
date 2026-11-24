# UX audit, 24 Nov 2026 (input to the redesign)

Method: a heuristic read of the navigation map (`navigation/Routes.kt`) and the recorded screenshots
(Today, Duas library, dua detail, Memorize home, Settings home, onboarding). No usage data exists
(the app is offline, no analytics), so everything below is a hypothesis to test on a phone, not a
measurement. Test fixtures ("0 doa", sample Arabic text) are not counted as defects.

## What works (keep)

- Four tabs with plain names (Today, Duas, Memorize, Settings); the bar is stable.
- Onboarding is one short page with a skip path ("Bisa juga diatur nanti").
- Memorize home: one "Lanjutkan" action, then a list whose state is written out, not only coloured.
- Settings is grouped (Shalat, Bacaan, Data, Sumber, Aplikasi) with a summary under each row.
- Dua detail puts the Arabic first and the personal note last.

## Findings, most costly first

| # | Where | Problem | Why it matters | Direction to test |
|---|---|---|---|---|
| 1 | Today | About eleven tappable things above the prayer list (place chip, Travel mode chip, Puasa, Jadwal, Penghitung, Kiblat, Masjid terdekat, forbidden-time info, dzikir card and its two buttons). All use similar outlined shapes, so nothing says "do this first". | Hick's law and weak visual hierarchy: the user's one question ("when is the next prayer, what do I do now?") shares weight with rarely used tools. | Make the Next prayer the single dominant element; move tools (Puasa, Jadwal, Penghitung, Kiblat, Masjid) into one quieter row or a "Tools" sheet. |
| 2 | Today | Kiblat and Masjid terdekat sit inside the Next prayer card. They are tools, not part of the prayer. | Wrong grouping: the card's meaning is blurred (recognition over recall). | Group tools apart from the countdown. |
| 3 | Today | "Waktu terlarang" (a passive state) and "Dzikir pagi" (an action) are cards of equal weight, stacked. | Status and action compete. | Show the forbidden time as a slim banner, keep the card for the action. |
| 4 | Navigation | Qibla, Mosques, Fasting, Timetable and the Dhikr counter are only reachable from Today. Search, Routines and Memorize sub-flows are one level down in their tabs. | Discoverability: features with no tab depend on the user noticing the tiles. | Decide per feature: tab, Today tool row, or search. Check with a "find the timetable" task. |
| 5 | Duas | Terms overlap: Rangkaian saya (my sets), Rutinitas (routines), momen (occasions), Hafalan (memorised). The library shows "Rutinitas harian, 6 momen" next to a tab called "Rangkaian saya". | Match with the real world: the same idea has several names. | One vocabulary, written down before the redesign (a short glossary). |
| 6 | Dua detail | The row under the Arabic mixes a repeat chip, a source chip and two icon-only buttons (listen/memorise, add to set). | Icon-only actions force recall; touch targets and TalkBack labels need re-checking. | Label the two actions, or move them to a labelled bar. |
| 7 | Settings | Pengingat and Island (beta) share the same bell icon. "Kirim masukan atau cek pengingat" joins two unrelated jobs in one row. | Same icon, different meaning; one row, two tasks. | Distinct icons; split the row. |
| 8 | Visual system | Glass cards over a gradient: card edges, chips, tiles and outlined buttons are all low-contrast rounded outlines. | Affordance: it is hard to tell a button from a container. WCAG 1.4.11 (non-text contrast 3:1) must be re-tested on any new surface. | Test a solid-surface variant against glass on one screen before committing. |

## Not looked at yet

Run (counting) screens, the Qibla and Mosques screens, the Timetable, the island overlay, widgets,
and every 200% text render. They need the same pass.

## Test plan (needs the owner, or 3 to 5 people)

Ask each person to do these on a phone, without help, and note hesitation, wrong taps and time:
1. Find when Isya starts and whether it is time for dzikir now.
2. Open the timetable for the month.
3. Find the dua for entering the mosque and start repeating it.
4. Turn off the adhan sound for Subuh only.
5. Find where the app asks for feedback.
