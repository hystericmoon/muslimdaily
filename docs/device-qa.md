# Device QA script

Robolectric covers layout, strings and logic. What it cannot cover is real hardware: sensors,
alarms under Doze, OEM battery managers, launchers, TalkBack and the watch. Run this pass before
each minor release (0.x.0) on at least:

- one Pixel or Android One phone on the latest Android,
- one phone with an aggressive battery manager (Xiaomi/HyperOS, Oppo/ColorOS, Vivo/Funtouch or
  Samsung One UI with "Put unused apps to sleep" on),
- if available, a tablet or unfolded foldable (≥ 600dp wide).

Record results in the release PR as a table: step, device, pass/fail, note.

## 1. First launch (fresh install, clear data first)

| # | Step | Expected |
|---|---|---|
| 1.1 | Open the app | Setup screen, not Today. No system dialog before you tap anything. |
| 1.2 | Tap "Gunakan lokasi saya", allow | "Lokasi: <town>" replaces the button within ~10 s. |
| 1.3 | Repeat on a second install, deny | Explanation that a city can be picked by hand; no crash, no second dialog. |
| 1.4 | Turn on prayer reminders | Android 13+: the notification dialog appears now, not earlier. |
| 1.5 | Deny notifications | The note "Notifikasi belum diizinkan…" appears; the switch stays on. |
| 1.6 | Android 12–13 without exact alarms | "Izinkan alarm tepat waktu" opens the system page; returning updates the note. |
| 1.7 | Tap "Mulai" | Today opens. Relaunching the app does not show the setup again. |
| 1.8 | Upgrade install over 0.4.x with a saved city | Setup is skipped; Today shows the old city. |

## 2. Prayer times

| # | Step | Expected |
|---|---|---|
| 2.1 | Pick Jakarta, compare with bimasislam.kemenag.go.id/jadwalshalat for today | Every time within 1 minute. |
| 2.2 | Pick Bandung, compare the same way | Terbit and Maghrib within 1 minute (elevation correction). |
| 2.3 | Settings → Waktu shalat → change method to MWL and back | Times change and return. |
| 2.4 | Change the phone's time zone while the app is open | Times follow the saved location's zone, not the phone's. |

## 3. Reminders under real conditions

Leave the phone idle (screen off, unplugged) for at least one full prayer cycle.

| # | Step | Expected |
|---|---|---|
| 3.1 | Prayer reminder with exact alarms allowed | Arrives within 1 minute of the time, also after 2+ hours of Doze. |
| 3.2 | Same with exact alarms denied | Arrives within 10 minutes. |
| 3.3 | Reboot the phone | Next reminder still arrives (boot receiver). |
| 3.4 | Aggressive OEM phone, app not whitelisted | Note whether reminders are late or missing; if missing, check the OEM's battery setting and record the exact menu path for the docs. |
| 3.5 | Nearby-mosque notification 10 min before | Arrives; tapping it opens Masjid terdekat, and a mosque's directions button opens the maps app. |
| 3.6 | Lock-screen next-prayer notification | Countdown ticks on the lock screen; on Android 16 it shows as a Live Update. |
| 3.7 | Fasting reminder the evening before a Monday | Arrives at the set time, only when tomorrow is a sunnah fast. |

## 4. Compass (Qibla)

| # | Step | Expected |
|---|---|---|
| 4.1 | Open Kiblat outdoors, away from metal | Dial settles within 2 s; haptic once when aligned. |
| 4.2 | Compare with a known qibla (a local mosque's mihrab or a second app) | Within ~5°. |
| 4.3 | Rotate to landscape | Pointer stays on the same real-world direction. |
| 4.4 | Hold near a laptop or speaker | Calibration note appears; figure-8 motion clears it. |
| 4.5 | Phone without a magnetometer (if available) | "Ponsel ini tidak punya kompas…" note; the bearing in degrees is still shown. |

## 5. Widgets and tiles

| # | Step | Expected |
|---|---|---|
| 5.1 | Add the "Jadwal shalat" widget on the stock launcher and one third-party launcher | One entry in the picker; the preview looks like the real widget; sky matches time of day. |
| 5.2 | Resize it through 1×1, 2×1, 4×1, 2×2, 4×2 and 4×3 (and 5 wide where the launcher allows) | Each size shows its layout (`docs/screenshots/80-widget-*`); no clipped or "…" times, names or countdown; numbers in Google Sans where the phone has it, labels in the system monospace. |
| 5.3 | Wait across a prayer time with the screen on | Widget moves to the next prayer within 1 minute. |
| 5.3b | With exact alarms allowed, watch the countdown for two minutes with the screen on | "Tiba dalam" counts down a minute at a time, with no "±". Without the permission it shows "±" and moves in steps of about ten minutes. |
| 5.4 | Upgrade over a build that had a "Jadwal shalat hari ini" widget placed | It keeps working and shows the new widget; the picker lists only "Jadwal shalat" (Android 12+). |
| 5.4b | Android 8–11 phone | The card and chips have round corners; the widget's own corners are round. |
| 5.4c | Pixel on Android 16 QPR2+: add the widget to the lock-screen widget page | It shows and stays legible; tapping asks to unlock, then opens Today. |
| 5.5 | Add the Quick Settings tile | Shows the next prayer and time; tap opens Today. |

## 6. Watch

| # | Step | Expected |
|---|---|---|
| 6.1 | Install the phone and watch APKs from the same release | Tile shows the next prayer within a minute of opening the phone app. |
| 6.2 | Put the phone in another room for an evening | Tile keeps advancing through the prayers it already has; after the last known one it asks for the phone. |
| 6.3 | Midnight with the phone away | Header changes from "Besok" to "Berikutnya" at midnight. |

## 7. Language, tablet, backup

| # | Step | Expected |
|---|---|---|
| 7.1 | Phone set to English, fresh install | Setup and app are in Indonesian; the setup offers "English". |
| 7.1b | Settings → Tampilan → Bahasa aplikasi → English (Android 12 and 14) | App switches at once; dates read "Wednesday, 21 October"; times use 04:32; notifications follow after the next reminder. |
| 7.2 | English, open a Hisn dua | Indonesian translation is shown (no English yet); Qur'anic duas show English. |
| 7.3 | Tablet or unfolded foldable | Navigation rail at the side; Today in two columns at ≥ 840dp; dua library list-detail. |
| 7.4 | Fold/unfold while on a dua occasion | No crash; state kept. |
| 7.5 | Export a backup, clear data, restore | Settings, My set, memorization and checklist return. |
| 7.6 | 29 Sya'ban after Maghrib (or set the phone's date) | Isbat card appears; answering adjusts the Hijri date and the card does not return. |

## 8. TalkBack

Run [the TalkBack checklist](talkback-checklist.md) on one phone each release.
