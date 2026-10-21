# Reviewer checklist

Defaults and wording that touch fiqh. None of them are religious *content* (no dua text,
sources or gradings), but the reviewing ustadz should confirm each before release. Each is a
setting or a string resource, so changing it needs no code change unless noted.

## Prayer times

| Item | Current value | Where |
|---|---|---|
| Default method | Kemenag RI: Fajr 20°, Isha 18° | `PrayerCalculator.kt` |
| Kemenag ihtiyat | +2 min Fajr/Asr/Isha, +3 min Dhuhr/Maghrib, −3 min sunrise; rounded up (sunrise down) | `PrayerCalculator.kt` |
| Accuracy | Within ±1 minute of the official Kemenag schedule for Jakarta, Surabaya, Medan, Makassar, Jayapura across four dates. Fixture: `app/src/test/resources/kemenag_reference.tsv` | `KemenagReferenceTest` |
| Asr default | Standard (shadow 1×), Hanafi optional | Settings |
| Imsak | Not shown, by design | — |
| Hijri date | Umm al-Qura calendar with −1/0/+1 day adjustment | `HijriDate.kt` |

Note: the official schedule shows a minute earlier than ours for a few Dhuhr/Asr/Fajr points
(coordinate differences). Users can add per-prayer offsets; the reviewer may prefer adding +1
minute to those prayers for caution.

## Windows

| Window | Rule used | Default to confirm |
|---|---|---|
| Morning adhkar | Fajr → sunrise | Note shown: some scholars allow a wider window |
| Evening adhkar | Asr → Maghrib | Same note |
| Prohibited after Fajr | After praying Fajr → sunrise + **15 min** | 15 min (Kemenag's own tables start Dhuha ~28 min after sunrise) |
| Prohibited at zawal | Dhuhr − **10 min** → Dhuhr | 10 min |
| Prohibited after Asr | After praying Asr → Maghrib | — |
| Duha | End of the sunrise prohibition → start of zawal | Note: best when the heat rises |
| Witr | Isha → next Fajr | — |
| Prayers with a cause | Note: scholars differ (e.g. tahiyyatul masjid) | `note_prohibited_cause` |

Both minute values are adjustable under Settings → Prayer → "Waktu terlarang dan Dhuha".

## Rawatib display rules (structure from the spec)

* Mu'akkadah 12: 2 before Fajr, 4 before Dhuhr (2 as the alternative narration), 2 after
  Dhuhr, 2 after Maghrib, 2 after Isha.
* Ghairu mu'akkadah: 4 before Asr, 2 before Maghrib, the prayer between adhan and iqamah.
  The last is listed only for prayers without a specific qabliyah (currently Isha), so it is
  never shown twice; its rak'ah count is left blank on purpose.
* No ba'diyah after Fajr or Asr.
* Friday: Jumu'ah row replaces Dhuhr, no qabliyah Jumu'ah row, ba'diyah "2 at home or 4 in
  the masjid", note that those who do not attend pray Dhuhr.
* Travel mode hides everything except Fajr sunnah (and witr, which is separate).
* Fard rak'ah counts are in `rawatib.csv` as `FARD` rows (status draft).

## Wording to confirm (strings.xml)

`note_adhkar_window`, `note_prohibited_cause`, `note_duha_best`, `note_jumuah`,
`window_prohibited_*`, `settings_dhuhr_qabliyah_summary`, `settings_travel_mode_summary`,
the Hijri month spellings in `hijri_months`, and the Methodology page text.
