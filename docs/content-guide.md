# Content guide: filling the spreadsheet

All religious content (Arabic, translations, sources, gradings, counts, virtues, audio) comes
from **two spreadsheets** that the owner fills and an ustadz reviews. The app never contains
text written by a developer or an AI. Every row starts as `[PLACEHOLDER]`.

```
content/source/duas.csv      one row per dua or dhikr
content/source/rawatib.csv   fard rak'ah counts and sunnah rawatib
content/audio/               one .opus recitation per dua
```

Easiest workflow: import both CSV files into Google Sheets (File → Import → Upload), fill
them in, then **File → Download → CSV** and replace the files in `content/source/`. Then run:

```bash
python3 tools/build_content.py
```

It converts the sheets to `content/duas.json` / `content/rawatib.json`, checks every row, and
bumps the content version when anything changed. If a published row is incomplete it prints
the problem and writes nothing. Release builds run the same gate again.

## Workflow per row

| `status` | Meaning | Appears in |
|---|---|---|
| `draft` | Being prepared. Anything can be missing. | Debug builds only, with a "Draft" label |
| `review` | Filled in; waiting for the ustadz. | Debug builds only |
| `published` | Approved by the ustadz. Must be complete. | Debug and release builds |

Only `published` rows ship to users.

## duas.csv columns

| Column | Required for published | What to enter |
|---|---|---|
| `id` | yes | Short unique code, e.g. `wake_01`, `morning_07`. Never reuse or rename once published (users' sets and memorisation refer to it). |
| `category` | yes | One of: `daily`, `prayer`, `adhkar`, `situational`, `quran` |
| `event` | yes | Event code from the table below |
| `order` | yes | Position within the event (1, 2, 3 …). Canonical order of the full set. |
| `variant_group` | no | Same code on rows that are alternative authentic forms of one dhikr (e.g. several istiftah forms). The app shows them as one item with a choice of form. |
| `arabic` | yes | Full text with complete tashkeel. **Mark phrase boundaries with `\|`.** Each phrase must have one audio segment. Phrases are used for highlighting and for memorisation chunks. |
| `is_quran` | yes | `ya` for Qur'anic text (shown in the Qur'an typeface), otherwise `tidak` |
| `translation_id` | yes | Indonesian translation |
| `translation_en` | no | English translation |
| `transliteration` | no | Latin transliteration (off by default in the app) |
| `count` | no | Number of repetitions **only if the Sunnah specifies it**. Leave blank otherwise; the app then shows no count. |
| `source_1_book`, `source_1_no` … `source_3_*` | at least one book | e.g. book `Sahih Muslim`, no `2723`. Up to three sources. |
| `grade` | yes | `sahih` or `hasan` only. Anything else is rejected. |
| `grader` | yes | Who graded it, e.g. `al-Albani` |
| `alt_gradings` | no | Other gradings: `grader: grade; grader: grade` |
| `virtue_text`, `virtue_source` | no (source required if text given) | The virtue and where it is narrated |
| `audio_file` | yes | File name in `content/audio/`, e.g. `wake_01.opus` |
| `audio_segments` | yes | Start-end seconds per phrase, in order: `0.0-1.8; 1.8-3.9; 3.9-6.2`. Count must equal the number of `\|`-separated phrases. |
| `status` | yes | `draft`, `review` or `published` |
| `reviewer_notes` | no | Free notes for the team; not shipped |

### Event codes

| Category | Events |
|---|---|
| `daily` | `waking_up`, `toilet`, `wudu`, `dressing`, `leaving_home`, `entering_home`, `going_to_masjid`, `entering_masjid`, `leaving_masjid`, `adhan`, `eating`, `drinking`, `sleeping` |
| `prayer` | `istiftah`, `ruku`, `sujud`, `tashahhud`, `after_salah` |
| `adhkar` | `morning`, `evening` |
| `situational` | `travel`, `rain`, `thunder`, `illness`, `distress`, `istikharah` |
| `quran` | `quranic_duas` |

New events are added in `content/taxonomy.json` (ask the developer).

## rawatib.csv columns

| Column | What to enter |
|---|---|
| `id` | Unique code |
| `prayer` | `FAJR`, `DHUHR`, `ASR`, `MAGHRIB`, `ISHA`, `JUMUAH`, or `ANY` (the prayer between adhan and iqamah) |
| `position` | `BEFORE`, `AFTER`, or `FARD` (the obligatory prayer itself, used for the rak'ah count) |
| `rakah` | Number of rak'ahs. May be blank only for `ANY`. |
| `tier` | `MUAKKADAH` or `GHAIRU_MUAKKADAH` (blank for `FARD`) |
| `variant_group`, `variant_label`, `default_variant` | Alternative narrations. One default in a group = the user picks one form (e.g. 4 or 2 before Dhuhr). Several defaults = shown together (e.g. Jumu'ah ba'diyah "2 at home / 4 in the masjid"). |
| `evidence_1_book`, `evidence_1_no`, `evidence_2_*` | Evidence. At least one book is required to publish. |
| `notes` | Short practical note shown in the app, e.g. how to pray it |
| `hidden_when_travelling` | `ya` to hide in travel mode. Per spec only Fajr sunnah stays visible (witr is handled separately). |
| `status`, `reviewer_notes` | As above |

The structure (which rawatib exist, rak'ah counts, tiers, no ba'diyah after Fajr or Asr) came
from the product spec and is pre-filled as `draft`. The reviewer confirms it and adds evidence.

## Audio

* Clear recitation only: no music, melodic intro or sound effects.
* Opus (`.opus`) mono, 48 kHz, ~24–32 kbps is plenty for speech.
* Segment times can be read off in Audacity: select each phrase, note start and end.
* `tools/gen_placeholder_audio.py` creates silent placeholders for unpublished rows so the app
  can be tested before recordings exist. Replace them with real files before publishing.

## Other settings the reviewer should confirm

These are app defaults, not content, but they touch fiqh. They are listed in
[review-checklist.md](review-checklist.md).
