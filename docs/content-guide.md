# Content guide

All religious content (Arabic, translations, sources, gradings, counts, virtues, audio) comes
from **two spreadsheets**. It is taken from published sources (see "Imported content" below);
the app never contains text written by a developer or an AI.

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
bumps the content version when anything changed. Every row ships to users, so if a row is
incomplete it prints the problem and writes nothing. App builds run the same gate again.

## duas.csv columns

| Column | Required | What to enter |
|---|---|---|
| `id` | yes | Short unique code, e.g. `wake_01`, `morning_07`. Never reuse or rename once shipped (users' sets and memorisation refer to it). |
| `category` | yes | One of: `daily`, `prayer`, `adhkar`, `situational`, `quran` |
| `event` | yes | Event code from the table below |
| `order` | yes | Position within the event (1, 2, 3 …). Canonical order of the full set. |
| `variant_group` | no | Same code on rows that are alternative authentic forms of one dhikr (e.g. several istiftah forms). The app shows them as one item with a choice of form. |
| `arabic` | yes | Full text with complete tashkeel. **Mark phrase boundaries with `\|`.** Each phrase must have one audio segment. Phrases are used for highlighting and for memorisation chunks. Keep the book's quotation marks: the build drops one `((…))` or `(…)` pair (and a full stop after it) when it encloses the whole text, and keeps `[…]`, notes in brackets inside the text and `﴿…﴾`. |
| `is_quran` | yes | `ya` for Qur'anic text (shown in the Qur'an typeface), otherwise `tidak` |
| `translation_id` | yes | Indonesian translation |
| `translation_en` | no | English translation |
| `transliteration` | no | Latin transliteration (off by default in the app). Leave it empty: the build derives Indonesian-style Latin from `arabic` with `tools/transliterate.py` (SKB Menag–Mendikbud 1987: ts, dz, sy, sh, dh, th, zh, ' for ع; ā ī ū; ai/au; sun letters; pausal forms). Rows whose Arabic has unvowelled words get none. A value here overrides it, and must come from a source. |
| `count` | no | Number of repetitions **only if the Sunnah specifies it**. Leave blank otherwise; the app then shows no count. |
| `source_1_book`, `source_1_no` … `source_3_*` | at least one book | e.g. book `Sahih Muslim`, no `2723`. Up to three sources. |
| `grade` | no | `sahih` or `hasan` only, where the source states it. Anything else is rejected. |
| `grader` | with `grade` | Who graded it, e.g. `al-Albani` |
| `alt_gradings` | no | Other gradings: `grader: grade; grader: grade` |
| `virtue_text`, `virtue_source` | no (source required if text given) | The virtue and where it is narrated |
| `audio_file` | no | File name in `content/audio/`, e.g. `wake_01.opus`. Without it the dua has no player. |
| `audio_segments` | no | Start-end seconds per phrase, in order: `0.0-1.8; 1.8-3.9; 3.9-6.2`. Count must equal the number of `\|`-separated phrases; otherwise highlighting is off. |
| `reference` | no | Takhrij as printed in the source (e.g. the footnote), shown under the sources in the app |
| `notes` | no | Free notes for the team (importer warnings); not shipped |
| `arabic_full` | no | The book's whole entry, verbatim, when `arabic` holds only the words to recite (see "Dua and context" below). Every word of `arabic` must come from it, in order. Shown in the app under "Lihat teks lengkap di kitab". |
| `translation_id_full` | no | The same for the Indonesian: the whole translation when `translation_id` holds only the dua's meaning. |
| `translation_en_full` | no | The same for the English. Shown instead of the Indonesian when the reader chose English. |
| `audio_range` | no | Start-end seconds of the dua in the audio file, e.g. `3.20-9.80`, when the recording also reads the context. Playback and memorisation cover only this part; the file is not edited. Segments must lie inside it. |

### Event codes

| Category | Events |
|---|---|
| `daily` | `waking_up`, `toilet`, `wudu`, `dressing`, `leaving_home`, `market`, `entering_home`, `going_to_masjid`, `entering_masjid`, `leaving_masjid`, `adhan`, `eating`, `drinking`, `sneezing`, `sleeping`, `night` |
| `prayer` | `istiftah`, `ruku`, `itidal`, `sujud`, `between_sujud`, `tashahhud`, `after_salah`, `witr`, `tilawah` |
| `adhkar` | `morning`, `evening` |
| `dhikr` | `shalawat`, `istighfar`, `tasbih`, `protection` |
| `situational` | `travel`, `rain`, `thunder`, `wind`, `new_moon`, `illness`, `distress`, `anger`, `fear`, `waswas`, `debt`, `news`, `animals`, `istikharah` |
| `fasting` | `iftar`, `fasting_manners` |
| `family` | `marriage`, `children` |
| `social` | `salam`, `gathering`, `manners` |
| `death` | `funeral`, `condolence`, `grave` |
| `hajj` | `hajj`, `qurban` |
| `quran` | `quran_forgiveness`, `quran_goodness`, `quran_ease`, `quran_family` |
| `sunnah` | `sunnah_heart`, `sunnah_goodness`, `sunnah_refuge` |

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
| `evidence_1_book`, `evidence_1_no`, `evidence_2_*` | Evidence, shown with the entry when given |
| `notes` | Short practical note shown in the app, e.g. how to pray it |
| `hidden_when_travelling` | `ya` to hide in travel mode. Per spec only Fajr sunnah stays visible (witr is handled separately). |

The structure (which rawatib exist, rak'ah counts, tiers, no ba'diyah after Fajr or Asr) came
from the product spec. Evidence columns are still empty.

## Audio

* Clear recitation only: no music, melodic intro or sound effects.
* Opus (`.opus`) mono, 48 kHz, ~24–32 kbps is plenty for speech.
* Segment times can be read off in Audacity: select each phrase, note start and end.
* Hisnul Muslim rows use the recitation by Hamad ad-Duraihim (one file per dhikr, published by
  IslamHouse.com, whose FAQ allows republishing unchanged with the source credited). Run
  `python3 -I tools/hisnul_muslim/fetch_audio.py` after importing new Hisn rows; it downloads the
  matching MP3 from hisnmuslim.com, transcodes it to `content/audio/hisn_NNN.opus` without editing,
  and fills `audio_file`. These files have no phrase segments, so playback has no highlighting.
* Duas without `audio_file` simply have no player.

### Phrase segments helper

Without phrases, a dua is one block: no moving highlight, and memorisation works on the whole
text. `tools/segment_audio.py` proposes phrases for rows that have audio but no `|` yet. It only
breaks where the source text already has punctuation (، . : ؛ ۝ and Qur'anic brackets) and
pairs each break with a pause in the recitation; it never changes a word. A person approves
every row.

```bash
python3 -I tools/segment_audio.py suggest --event morning --event evening   # or --id morning_03
```

1. Open `build/segments/review.html` in a browser. Tap each time range: the recitation should
   say exactly the phrase next to it.
2. In `build/segments/review.csv`, set `approved` to `ya` for rows that sound right. To fix a
   row, edit `audio_segments` or move/remove `|` in `arabic_marked` (keep the counts equal).
   Rows marked `match` or `close` usually need no edits; `partial` rows need the closest look.
3. `python3 -I tools/segment_audio.py apply`, then `python3 tools/build_content.py`.

`apply` skips a row if its Arabic changed since the suggestion, or if `arabic_marked` differs
from the source by anything other than `|`. Some long Qur'anic passages (e.g. Ayat al-Kursi)
have no punctuation inside, so they stay one or two phrases; adding waqf breaks there is a
content decision. Run this after the final Hisn import, because `import_hisn.py --merge`
replaces imported rows.

### Dua and context helper

Hisnul Muslim prints many entries as the narration: the hadith wording around the dua ("when one
of you eats, let him say: …"), notes in [ ] (another form, or when it is said) and repetition
notes ("three times"). The recitation reads the same text. `tools/split_context.py` proposes the
words to recite and where they are in the recording; it only leaves words out, never adds or
changes one (checked for every row, again by `build_content.py`).

```bash
python3 -I tools/split_context.py suggest        # or --id eating_01
```

1. Open `build/split/review.html`. Each entry shows the book's text with the proposed dua in
   green and the context in grey. Tap "Play dua part" to check the recording starts and ends on
   it. Entries marked ⚑ need a decision: most are hadiths about a dhikr's virtue, where only a
   person can say which words are the dhikr, or that the entry should stay whole.
2. In `build/split/review.csv`, set `approved` to `ya` for rows that are right. Fix `arabic`,
   `audio_range` or `translation_id` there first if needed (words may only be left out).
3. `python3 -I tools/split_context.py apply`, then `python3 tools/build_content.py`.

Do this before the phrase segments helper: `segment_audio.py` then marks phrases inside the
dua's audio range only.

## Imported content: Hisnul Muslim

The rows in `duas.csv` (24 events) and the full sheet `content/source/import/hisnul_muslim.csv`
(all 267 entries, including chapters without an app event yet) come from
`tools/hisnul_muslim/import_hisn.py`:

* **Indonesian translation and takhrij**: the Indonesian Hisnul Muslim PDF supplied by the owner
  ("Hisnul Muslim Kumpulan Doa dan Dzikir Shahih Dari AlQuran dan AsSunnah").
* **Arabic text and repetition counts**: hisnmuslim.com, joined by the book's entry number.
* `source_N` holds the books cited in the footnote (or "Hisnul Muslim no. N" when it cites
  none), `reference` the footnote verbatim plus the entry number, and grade/grader only where
  the footnote states al-Albani's verdict.
* The Arabic gets a narrow typographic cleanup (`tidy_arabic`): `*` between verses becomes
  the end-of-ayah sign ۝, an inline repetition note such as «(ثلاث مرات)» is dropped when the
  count badge shows the same number (notes with a condition, e.g. «إذا أصبح», stay), and
  «﴾.» becomes «﴾». The words themselves are never changed.
* Entries the PDF's appendix reports al-Albani graded weak are left out of `duas.csv` and
  marked `LEMAH` in the full sheet. A count he judged unauthentic is left blank.
* An event with no rows is hidden from the library, routines and memorisation. Two are empty
  on purpose: `entering_home` (Hisn no. 18 is graded weak) and `drinking` (no Hisn chapter).
  Filling them needs another sahih or hasan source.

Imported rows ship as they are; there is no review step.

## Imported content: HadeethEnc (duas from the Sunnah)

Category `sunnah` holds sahih and hasan duas that Hisnul Muslim does not carry, from
HadeethEnc.com (Encyclopedia of Translated Prophetic Hadiths, the IslamHouse family). The
snapshot `content/source/import/hadeethenc.json` keeps each hadith as downloaded (Arabic, grade,
takhrij, Indonesian and English); `tools/hadeethenc/import_hadeethenc.py` turns it into the
`sunnah_NN` rows (`--fetch` downloads a new snapshot).

* **Licence**: republish "without modification, addition, or deletion", publisher and source
  credited (About screen and `reference`). So the whole hadith stays verbatim in `arabic_full`,
  `translation_id_full` and `translation_en_full`; `arabic` and the translations hold only the
  dua, a span cut from that text. The Arabic span gets `|` after each comma: no word changes.
* **Grade**: HadeethEnc's own (grader `HadeethEnc`); anything but sahih or hasan stops the import.
* **What is imported** is listed in the tool's `SELECTED`, and what was left out, with the
  reason, in `EXCLUDED`.

## Checking imported content

The owner has no reviewer, so every imported row is checked when it is imported, and the
checks are recorded in the importer:

1. The grade is sahih or hasan, as stated by the source.
2. The dua is not already in the app (compare the Arabic with `arabic` and `arabic_full`).
3. It is a dua the reader says, not an instruction about one.
4. The Indonesian and the English translate the same Arabic, completely.

A row that fails any check is left out, with the reason in the importer. It is never
corrected or completed by hand. To re-run:
`python3 tools/hisnul_muslim/import_hisn.py --pdf <file> --merge`. It replaces the previously
imported rows of the mapped events, so make hand edits after the final import.
