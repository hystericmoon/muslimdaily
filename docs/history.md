# History

The archive behind `docs/handover.md`: finished work, release notes with their reasons, closed
items and facts that took time to find. Moved here verbatim on 3 Nov 2026 to keep the handover
short. Read a section when a task touches it (search by row id, PR number or tool name);
don't read it end to end at session start. Newest first within each section.

## Promo campaign (moved from handover, 14 Nov)

Batches 1 and 2 are done: 34 posts, Mon 9 Nov to Tue 5 Jan, every slot has media, copy and alt text.
The owner posts; nothing has been posted. Details of batch 2: history, "Promo batch 2".
- Plan, results and calendar live in Notion (the Drive Doc and Sheets were trashed 6 Nov, restorable
  from Drive's trash): plan https://app.notion.com/p/40feb9fc21558177b761e849159cd6f3, calendar database
  https://app.notion.com/p/06ba9cf0b4664b45b94264815533ad04. `marketing/calendar.csv` is the source of truth.
  Calendar columns added 6 Nov (Thumbnail 7 Nov): Media link (GitHub raw link on `promo-assets`, folder link for
  Instagram carousels), Copy, Alt text, Extra files (.srt, slide names); filled for all 34 rows from
  `copy.md`/`alt-text.md` verbatim. Update them in Notion if copy changes (property key is `Media link`,
  no `userDefined:` prefix). Links open only while signed in to GitHub (private repo).
- Media: orphan branch `promo-assets` (never merge it; head 15d6316) and the review page
  https://claude.ai/artifact/MTfXg3ptJcNMLLqRhVSMsj (private; the owner shares it).
- Source: `marketing/` (`README.md` how to render, `brief.md`, `batch2.md`, `copy.md`, `alt-text.md`,
  scenes, engine). Last four slots: imgP1, imgP2, imgT1, imgD1 (26, 28, 31 Dec, 2 Jan; ID only).
- LinkedIn video thumbnails (7 Nov, #185): v1 `images/tV1-en.png`, v2 `images/tV2-en.png` (scene ids
  `tV1`/`tV2` in `stills.html`: covers without swipe line and counter), v3 existing `imgW1-en.png`.
  Linked in the Notion `Thumbnail` column (URL) for weeks 1, 3, 5.
- Decisions (owner, 5 Nov): direction A (parchment, product first); memorization first; light and
  dark both shown; Arabic only as empty hidden-word boxes; silent; any voiceover is a male voice.
- Rules: no IslamHouse/HadeethEnc material, no religious text, no faces, no streaks. Ask the owner
  before pushing to `promo-assets` (the owner said "always allow" on 6 Nov for pushes, review page,
  Notion and Drive updates of this campaign).
- Pitfall: the Screenshots workflow does a plain `git push`: push your own commits after it ends.
- Possible next: a LinkedIn slot after 5 Jan, English versions of the four open-slot stills, a batch 3.

## Shipped

1.13.0 (versionCode 56, 11 Nov WIB, release PR #193; features #188, #191, #192): Memorize toggle (occasion card, dua
source row, Undo), compact player bar with previous/next phrase, Isha end countdown, Today timeline rail check, Today
heading without year, Start reading only on morning/evening adhkar (`sequential` flag, ContentDatabase v8). No
content rows changed.

1.12.6 (versionCode 55, 3 Nov 21:23 WIB, #159, content #157): English for eating_01 and waswas_04, funeral_01
audio (hisn_153), Latin for 16 rows, 5 Latin corrections (adhan_02, adhan_04, istiftah_03, news_02,
funeral_03). Unvowelled «رضي الله عنه» left out of the Latin (owner, 3 Nov). content_version 55.

1.12.5 (versionCode 54, 3 Nov 14:28 WIB, #152, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.12.5): About credits IslamHouse
as their reply suggested: "Audio: Hamad ad-Duraihim" and "Teks: Hisnul Muslim" (Mahrus Ali for the
two eating duas), from #150. No content change; content_version stays 54.

1.12.4 (versionCode 53, 3 Nov 13:16 WIB, #147, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.12.4): eating_09 has Indonesian; eating_08/09 from IslamHouse's
Indonesian PDF (Mahrus Ali), translator named in the reference; `tools/check_sources.py`.
content_version 54.

1.12.3 (versionCode 52, 3 Nov 11:51 WIB, #144, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.12.3):
eating_08 has Indonesian from hisnmuslim.com (details under In flight). content_version 53.

1.12.2 (versionCode 51, 3 Nov 09:58 WIB, #142): end-of-clip scan of all 312 recordings (Whisper
on each clip's last 10 s and the 4 s after it, plus the loudness at the cut; decision page
https://claude.ai/artifact/EoRzFgYXsZcgYJLBoLRuXn). No clip plays a word after the dua. Seven
Qur'an clips (quran_08/13/17/18/19/24/26) ended 0.14-0.51 s before the held last vowel died
away: `recut_audio.py` mode `end` moved them into the silence, each re-checked by
`verify_audio.py`. tasbih_01 tried and dropped (the reciter runs on into «في يوم»). 16 other
flags checked, nothing to fix (list on the page; sleep_02/07's source files end on the last
word). content_version 52.

1.12.1 (versionCode 50, 3 Nov, #139): six clips (dress_01/02, grave_03, travel_10,
distress_01, travel_01) no longer open on the reciter's chapter title; travel_01's first two
phrases joined (details under In flight).

1.12.0 (versionCode 49, 2 Nov, #136; owner chose B and C on the decision pages
https://claude.ai/artifact/NJi4iMwytByy5Nj1WVcwNF and https://claude.ai/artifact/8DvtMWsQgHHa7yRJdbrxVf):
the book's ، in the palette's primary (`withPauseCommas`, also on the 7 Qur'anic rows that carry
it; muted inside book notes and muted text); morning_19 / evening_19 left out as weak
(`clean_entries.WEAK`: al-Albani hasan in Sahih al-Jami' 5357, then weak in Silsilah ad-Da'ifah
5788 per islamqa 130213; ad-Da'ifah itself not read from a cloud session). Saved reviews and
routines keep the ids; `ReviewRepository` never counts a missing id as due and routine cards skip
it. `hisn_098.opus` stays unused in `content/audio`. content_version 49.
Note: `clean_entries.py` is not idempotent on main's CSV (it re-touched 45 rows); the two rows were
removed from `duas.csv` directly and the decision kept in `WEAK`.

1.11.0 (versionCode 48, 2 Nov, #133): istiftah_05's lines the recording leaves out are marked.

1.10.3 (versionCode 47, 1 Nov, #131): #129 (two re-timed recordings, travel_12/13 second source).

1.10.2 (versionCode 46, 1 Nov, #128): hadith numbers from islamweb.net, 86 duas re-timed after the Whisper check, tasbih_09 skips its narration, #122 ("Hafal?" below the Arabic).

1.10.1 (versionCode 45, merged 1 Nov, #120): #116, the Android 8–11 widget no longer shows
"Problem loading widget" (no location set, "moved to Adwam" notice).

1.10.0 (versionCode 44, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.10.0):
#104 (Darussalam under "Penilaian lain"), #109 (city list during first import), #112 (one widget
entry on Android 8–11, #107), #113 (content cleanup, reading cards, footnotes, timed audio).

1.9.0 (versionCode 43, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.9.0):
#94, #96, #97, #98, #99, #102 (details below).

#97 (31 Oct): the PDF's footnote markers (NUL bytes plus stray dots) removed from 7 rows'
Indonesian (morning_04/06/13, evening_04/06/13, distress_05; morning_04/evening_04 and
distress_05 showed them in the main translation). `build_content.strip_markers` does it, used by
`parse_pdf.py`; the build now fails on any control character.

#96 (31 Oct): morning_13 / evening_13 drop the trailing أَوْ, the "or" that
introduced the book's note "(or once, when lazy)"; the clip ends at 11.71 s (the pause after
قَدِيرٌ) instead of 14.07, which ran on into the note. `split_context.py` now drops an
"or" left in front of a dropped note. The owner approved the change; the new clip end comes
from the pauses, not from listening, so it is in the listening review's next round.

In 1.9.0 with #96, #97 above: #94 Memorise Listen/Build in the focus
layout; practising missed words clears the hint level and always shows the reveal button; the
focus layout's Back/Next row no longer breaks at 200% text (it also broke on the dua page in
1.8.0).


1.8.0 (versionCode 42, released 31 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.8.0): #89 Memorise Cek marks missed
words (suggested rating, practise only those); #90 dua layouts A/B/C (`PhraseLayout`); #91 the
build drops `(( ))` before bracketed notes (8 rows, content_version 30); #92 phrase fills
(`PhraseFill`, hide while playing) and the Aa reading sheet on the dua page (`ReadingSheet`,
shared with Display settings).

1.7.0 (versionCode 41, released 30 Oct, https://github.com/hystericmoon/muslimdaily/releases/tag/v1.7.0):
#82 Today without a location, onboarding city picker and text size, 200% fixes, audio error
message, search ranking (content DB v6); #83 six Hisnul Muslim rows reduced to the recited
words; #84 reading view (one phrase per line, tap to play from a phrase, muted book notes);
#85 ATF checks in `AccessibilityChecksTest` (add new screens to it); #86 this file; #87 release.

## Finished work, details (was "In flight")

- **Promo batch 2** (6 Nov, Sonnet, #165-#181; routines #174-#175, four open-slot stills `imgP1`, `imgP2`,
  `imgT1`, `imgD1` from screens 41, 100, 10 in #179; calendar and plan moved from Drive to Notion, #178, #181): English-UI shots 150-164 (`PromoEnglishScreenshotTest`,
  `WidgetScreenshotTest`) and routine shots 170-174 (`PromoRoutineScreenshotTest`, light only via
  `captureLight`); `prep_screens.py` builds `-en` screens and handles light-only entries; scenes
  `stills.html` (carousels C, D, E, images, `imgH`) and `v3-widgets.html` read them for `lang=en`.
  Media: orphan branch `promo-assets`; review page updated each step. Pitfalls found: a worktree's
  own FETCH_HEAD is stale (fetch inside it); the Screenshots workflow commits with a plain `git
  push`; `.scrim` is opaque only to 62% of its height and `fade-b` hides the bottom 250 px of
  stills; a stills `phone()` needs `fx,fy` as the screen point that lands at `cx,cy`; `blank` fills
  with the column median (fails when the object fills the region: use `bridge`); render ~15-25 min
  per video, 2-4 in parallel on 4 cores. The gold completion mark on the run summary is blanked in
  promo shots (no reward look).
- **Promo batch 1** (#163, 5-6 Nov, Opus director with Sonnet agents): `marketing/` render engine
  (HTML scenes, Playwright frames at 60 fps blended to 30 fps by ffmpeg `tmix`), 2 videos x 4
  formats, 8 images, 2 carousels, copy, alt text, calendar. Media on orphan branch `promo-assets`.
  Two fresh-eyes QA passes (images, video frames) found no content blockers; craft fixes applied.
  A 28.6 s 1080x1920 render takes ~38 min with three renders sharing 4 cores.
- **1.12.1** (versionCode 50, #139, merged 3 Nov 07:13 WIB; see Shipped): the owner heard the end of
  the chapter title («يقول») at the start of dress_01 and dress_02 (2 Nov). An intro scan of all
  312 recorded rows (Whisper on each clip's first 8 s, words heard before one of the dua's first
  four words; script kept only in the session's scratchpad: re-create from this description)
  found three more: grave_03, travel_10, distress_01, and travel_01 (below). All five re-cut by `recut_audio.py start`
  (rows in `tools/align/start_fixes.csv`), re-checked by `verify_audio.py`: all start on the
  dua (dress_02 phrase 1 "check" is Whisper fusing «أنت كسوتنيه»). Only `audio_range` and the
  first segment changed. The 1 Nov check missed them because a single extra word passed its
  "more than one word" rule. Flags that were not intros: misheard first words (ruku_03,
  morning_10/evening_10, children_02, quran_18/21/23 «وقر» for «وقل») and rows whose text is
  itself the book's instruction (night_03, waswas_01, waswas_06, sleep_12). PR #139; the owner
  approved the merge and the release (2 Nov, Jakarta 3 Nov).
- **travel_01** (owner chose B, 2 Nov, decision page https://claude.ai/artifact/Xo4WGqb1JzSdxZPe2Gm9nH):
  phrase 1 (2.27-6.87) played only the title «دعاء الركوب … أقول»; phrase 2 (6.87-11.97) held
  «بسم الله والحمد لله» with no pause quiet enough to cut between them (noisy track; Whisper's
  whole-file words for hisn_206 miss the opening). New `recut_audio.py` mode `join` (row in
  `start_fixes.csv`): the two phrases are one, playing 6.87-11.97; the clip starts at 6.87. Text
  unchanged (one "|" removed). verify_audio: all 11 phrases ok. In 1.12.1.
- **1.11.0** (versionCode 48, PR #133, owner's choice B on 2 Nov, decision page
  https://claude.ai/artifact/1SzPRAj9bpBXBfP45f3Na4): istiftah_05's recording leaves out two of
  the book's bracketed lines («وَمَا أَنْتَ أَعْلَمُ بِهِ مِنِّي», and the closing «وَلاَ حَوْلَ وَلاَ قُوَّةَ
  إِلاَّ بِاللَّهِ»: the file ends on «أنت»). The text is unchanged; those phrases have segment `-`
  (`null` in duas.json) and show muted with a dotted underline, a note under the Arabic, TalkBack
  "Not in the recording"; never highlighted or played; Memorise chunks and A–B loops skip them.
  Pipeline: `recut_audio.py start`, mode `unheard` (row in `tools/align/start_fixes.csv`).
  App: `Phrase.unheard`, `Dua.isSegmented`. Screenshot 144. Owner kept the display as is (no
  extra label, no lighter grey); note that the book's bracketed lines are already muted by
  `withBookNotes`, so the dotted underline is what tells unheard lines apart.

- **haho-design 0.2.0** (1 Nov, shipped in 1.10.2): pin bumped from 0.1.0. The six
  `OutlinedTextField`s (search, location, routine name/notes, memorize picker, dua note) are now
  the library's `HahoTextField` (16dp corners instead of Material's 4dp), so their screenshots were
  re-recorded. `DockedBar` content now clears side insets (landscape only). New library parts Adwam
  could use later: `HahoOptionChip`, `HahoSegmentTrack`, `CenterZeroSlider`, `HahoTopBar`,
  `GlassOutlinedIconButton`, `HahoTheme.status`, `OpenSourceNoticesDialog` (see haho-design
  README).
- **Content cleanup** (#113, shipped in 1.10.0): every row only what the reader says. Tools:
  `tools/clean_entries.py` (every text decision, with reasons; re-run only on a fresh main CSV,
  it is not idempotent on its own output) and `tools/align_audio.py` (wav2vec2 Arabic forced
  alignment; timings in `tools/align/word_timings.json`; the `timings` step needs a venv with
  torch, see its docstring; ~5 s a row on CPU). Review page (record):
  https://claude.ai/artifact/2Pm8Bu3GsYdhqG4YNWUZd1. It replaced the listening review
  (GzK5…). Left as they were: tasbih_09 (a #83 cut joining two runs, so no clip); 16 rows the
  aligner was unsure of keep their old audio; 87 rows that had phrases keep them.
  Update 1 Nov (PR #128, branch `claude/hadith-numbers-islamweb`): tasbih_09 has two phrases
  and the app skips the narration between them (`Dua.playableParts` in `AudioEngine`).
  **Full Whisper check of all 314 rows** (`tools/verify_audio.py`, results in
  `build/align/verify.csv`, ~2.5 h): 88 rows flagged; about 30 were real mis-cuts from the
  hisnmuslim.com recordings: the clip started on the reciter's chapter title ("دعاء ... يقول"),
  every phrase played one segment late (morning_06/evening_06, fear_06, tilawah_01,
  leave_home_01, night_01, after_salah_01 …), or a count note was read at the end ("ثلاث مرات",
  morning_02/09, evening_02/09, salam_03, witr_04). The rest were Whisper mishearing.
  `tools/recut_audio.py` re-timed **73 rows** from Whisper's word times (stored in
  `tools/align/whisper_words.json`) and the loudness; every one was re-checked by Whisper and
  passes or shows only mishearing inside the right cut. Eight more had only a wrong clip start
(on the title; iftar_01 started after its first word, cutting it off): fixed by
`recut_audio.py start` from `tools/align/start_fixes.csv` and re-checked. After 1.10.2 (branch
`claude/audio-last-three`): waswas_05 re-cut from after its title (mode `full`) and wake_04's
break moved after "وبئس المهاد" (mode `break`), both in `start_fixes.csv`. istiftah_05's cuts are
right (Whisper's segment check drops its repeated lines; the full-file transcript has them all);
but **the recording does not say "وَمَا أَنْتَ أَعْلَمُ بِهِ مِنِّي"** (nothing between "أعلنت" 83.5 s and
"أنت المقدم" 85.5 s): the text shows words the audio lacks. Owner's call; the text is not edited
by hand. Resolved in 1.11.0 (above).
quran_16/18, dress_04, travel_01, sujud_03 are fine
  (Whisper mishearing). Whisper invents text on short or silent clips ("اشتركوا في القناة",
  "ترجمة نانسي قنقر", a word repeated over the silence): read such a flag as a mishearing.
- **1.10.2** (versionCode 46, PR #128, owner approved the merge and release on 1 Nov): #128 and #122.
- **Da'if grades found by #102**: the owner chose B (31 Oct, decision page
  https://claude.ai/artifact/JV9nAhPh1S4QmUJ5AegmLN): the five rows (between_sujud_02,
  leave_home_02, sleep_12, illness_05, illness_06) stay on al-Albani's grade and the Sources
  sheet lists Darussalam's grade per chain under "Penilaian lain". Done by
  `tools/alt_gradings.py` (copies from `tools/hadith_numbers_sunnah.csv`, refuses anything not
  in it), content_version 35, merged in #104, shipped in 1.10.0.
- **Plan B for Play** (open work 1): #118 merged 1 Nov with option A (owner's choice): the 5
  English-only Hisn rows are left out of the Play build. #124 (1 Nov): the release workflow
  builds the AAB (artifact `play-bundle`, unsigned until the `PLAY_UPLOAD_*` secrets exist, see
  `docs/release.md`); screenshot `143-play-hisn-english`; the old What's New Hisn-English items
  can't show in the Play build, so they stay. Checked locally: the AAB's `content/duas.json` is
  byte-identical to `content/play/duas.json`. See `docs/play-store.md`, "Plan B".
- **Play build = GitHub content** (owner chose A, 3 Nov, decision page
  https://claude.ai/artifact/4QQtVz85R8KrGWNZHBU4XM; branch `claude/play-same-content`): Plan B's
  separate copy removed (`content/play/`, `ContentAssetsTask.duasFile`, `src/release` strings,
  screenshot 143, `play_duas`). Play gets the hisnmuslim.com English and the 5 rows back. Nothing
  ships to users (Play on hold); no version bump. `docs/play-store.md`, "Content".
- **1.12.4** (versionCode 53, #147, released 3 Nov 13:16 WIB; owner: "do all the Opus
  work", 3 Nov): IslamHouse's own Indonesian Hisn PDF
  (https://d1.islamhouse.com/data/id/ih_books/single/id_hisn_almuslim.pdf, 156 pp., «Kumpulan Doa
  dalam Alquran & Hadits», terj. Mahrus Ali, koreksi Abdullah Khaidhir, Fir'adi Nasruddin, Erwandi
  Tarmizi; Maktab Dakwah dan Bimbingan Jaliyat Rabwah 1430/2010) is the edition hisnmuslim.com/i/id
  shows, with its title page. Found via web search; the old islamhouse.com/39092 page is 404 and
  dev.islamiccontent.org's IslamHouse API (api3.islamhouse.com/v3) was not called (the auto-mode
  classifier refused a call with the key from IslamHouse's SDK). It gives eating_09 Indonesian
  (entry 179, «Ya Allah! Berilah kami berkah padanya dan tambahkanlah kepada kami (berkah)
  darinya», matches «بارك لنا فيه وزدنا منه») and names eating_08's translator. Source kind renamed
  `hisnmuslim:<no>` → `islamhouse:<no>` (snapshot `content/source/import/islamhouse_hisn_id.json`
  with the PDF's sha256, by `tools/hisnul_muslim/fetch_islamhouse_id.py 69:178 69:179`); the
  reference note now names translator, publisher and edition. tasbih_03 (256), tasbih_06 (259),
  salam_04 (227) give only a Latin reading in it too: still English only. content_version 54.
  Same PR: `tools/check_sources.py` (see open work 0).
- **1.12.3** (versionCode 52, #144, released 3 Nov 11:51 WIB; owner chose A on 3 Nov):
  eating_08's Indonesian «Dengan menyebut nama Allah, pada awalnya dan akhirnya.» cut verbatim
  from hisnmuslim.com/i/id/69 (entry 178). New source kind `translation_id_from = hisnmuslim:<no>`
  (must be the row's own "Hisnul Muslim no. N", cut verbatim from the snapshot
  `content/source/import/hisnmuslim_id.json`, written by `tools/hisnul_muslim/fetch_indonesian.py
  <chapter>`). The build appends "Terjemah doa: hisnmuslim.com (IslamHouse.com), tanpa nama
  penerjemah." to the reference (shown in the Sources sheet) and leaves the row out of the Play
  build like the hisnmuslim.com English. Methodology text (About) names it (screenshot 46
  re-recorded). content_version 53.
- **English-only Hisn rows** (eating_08 until 1.12.3, eating_09 until 1.12.4; left: tasbih_03, tasbih_06, salam_04): the owner
  asked (1 Nov) to find their Indonesian online. Nothing importable: HadeethEnc 5507
  (tasbih_03) has the two phrases in the other order, 4201 (tasbih_06) leaves out "Yang Maha
  Agung", 65997 / 66011 / 65345 (eating_08, eating_09, salam_04; 65345 is a different wording)
  are Arabic only so far. IslamHouse's Indonesian Hisn page (39092) is gone (404). Re-check
  HadeethEnc later, or import another licensed Indonesian edition if the owner supplies one.
- **Mac session** "Sunnah.com hadith lookups" (session_01W2ZFMuts1anPa7PAqpiUjD, on the
  owner's Mac via `claude remote-control`): did #102, the widget pass the evening of 31 Oct
  (results in `docs/device-qa.md` §5; issues #106, #107) and #116. Stopped 1 Nov. TalkBack was
  skipped at the owner's word (31 Oct). It can message cloud sessions; cloud sessions can't
  message it (the platform refuses cloud-to-Mac messages without a trust grant).
- **Hadith numbers**: 42 of the 58 empty rows filled (#99 dataset, #102 sunnah.com), 10 more on
  1 Nov from the islamweb.net library (`tools/hadith_numbers_islamweb.csv`, URL per row; it
  answers cloud sessions, sunnah.com and dorar.net don't): al-Hakim (morning_10, evening_10,
  night_01, travel_03, funeral_06), an-Nasa'i al-Kubra (wudu_04, manners_04), al-Baihaqi
  (witr_03), Ahmad 19109 (protection_02, plural wording), Abu Dawud 466 (enter_masjid_01, the
  a'udzu sentence). 5 without a first-source number (7 before morning_19/evening_19 left out): ruku_01, sujud_01, tashahhud_13 (no single hadith, #102),
  morning_19/evening_19 (left out in 1.12.0 as weak), travel_12/13 (Ibn as-Sunni not on islamweb; their second source,
  al-Hakim 1883, is filled since 1.10.2 via `slot` 2 in the islamweb CSV). al-Mu'jam al-Kabir's
  section for the companion runs ~7000 pages with no search; a crawl that size was refused. islamweb book ids: Mustadrak 74, Musnad Ahmad 6, Baihaqi al-Kubra 71,
  Nasa'i al-Kubra 406 (its 'Amal al-Yawm is content ids 10054-11232), Tabarani Kabir 84, Awsat
  85, Saghir 86. Pages: `/ar/library/content/<book>/<id>/`; chapter id ranges are the
  `data-idfrom`/`data-idto` attributes of `index.php?page=bookcontents&bk_no=<book>`. The
  auto-mode classifier refused one bulk crawl of ~9000 pages; fetch a chapter at a time.
- This session (3 Nov, after a clear, Opus): session_01W2ZFMu64SGUJb7ZB37ryAj again: 1.12.3
  (#144), #145, Play = GitHub content (#146), 1.12.4 + check_sources (#147). Next: Sonnet.
- Before it: the same session id (1.12.1 released; #140, #141 Opus/Sonnet rule; 1.12.2 = #142;
  eating_08 decision page).
- Before it: session_01W2ZFMtXqwh55Bi2zuR56rM (#138 setup script; 1.12.1 = #139).
- Before that: session_01W2ZFMtK31PRVNJYbAMjRnn (closed #127; #135; 1.12.0 = #136).
- Before that: session_01W2ZFMtjcNVqpoYdcT8Jicx (#132, #133 = 1.11.0). Before it:
  session_01W2ZFMuv8AfZt8voGDUM455 (#128 = 1.10.2, #129 merged). Before it:
  session_01W2ZFMscuG3LNhNF4PPQ8Gn (#124). Before it: session_01W2ZFMtyVHfVpmCd9Vz2uc7 (1.10.1, #118, #122).
- Branch deletes: the cloud git proxy refuses `git push --delete`; the owner can turn on
  "Automatically delete head branches" in the repo settings.

## Closed open-work items

0. **Done (3 Nov, Sonnet, #150, released in 1.12.5): IslamHouse credits.** About's licence list now has "Audio:
   Hamad ad-Duraihim" ("Sumber: IslamHouse.com …") and "Teks: Hisnul Muslim" (Arabic and English
   from hisnmuslim.com, no translator named; Indonesian of the two eating duas: Mahrus Ali; source
   IslamHouse.com). `methodology_*` and content untouched. Screenshot 47 re-recorded by the
   Screenshots workflow. Shipped as 1.12.5 (#152). Also done: `tools/check_sources.py` (1.12.4;
   release gate in `docs/release.md`), Play content (#146). Open follow-up: none.
2. Done in 1.12.0 (accent-coloured ،). Decided
   31 Oct: no ۝ in non-Qur'anic duas (the owner checked: sunnah.com and dorar.net don't use it);
   the text keeps the book's punctuation.
3. Done in 1.10.0 (#113), 1 Nov (#128: tasbih_09; full Whisper check; 83 rows re-timed) and
   1.11.0 (#133: istiftah_05). Nothing left from the audio check.
6. Empty `source_1_no`: 5 rows left (see In flight). Closed 3 Nov: ruku_01, sujud_01,
   tashahhud_13 have no single hadith (#102); travel_12/13 cite Ibn as-Sunni, not reachable from
   cloud sessions. Reopen only if the owner can look them up on the Mac. sunnah.com and dorar.net answer 403 to
   cloud sessions (also through the web-fetch tool); islamweb.net answers.
7. Widget pass for 1.9.0 done (31 Oct, `docs/device-qa.md` §5). Left: 5.4c (lock-screen
   widget: the Redmi's PixelOS has none, so it needs a real Pixel), 5.4's legacy-widget half, and a third-party
   launcher. All three can run on emulators on the Mac (an Android 16 QPR2+ Pixel image for
   5.4c; install 1.5.0, place the old widget, upgrade, for 5.4; Lawnchair from its GitHub
   releases for a third-party launcher), told to the owner 31 Oct. TalkBack T1–T12: skipped
   by the owner (31 Oct); a later pass belongs on `qa_api37_profile`.
   Filed from it: #106 (fixed by #109) and #107 (fixed by #112: the old receiver is disabled
   below Android 12, owner's choice); both shipped in 1.10.0. #112 verified on Android 8 (one
   picker entry, the placed widget kept working). #116 (found on `qa_api26_widget`) merged
   1 Nov. Cloud sessions can't do it: the container has no /dev/kvm (no emulator) and
   `list_computer_folders` is empty until the owner runs `claude remote-control` in the clone
   on the Mac; then a cloud session can attach it with `request_computer_folder` (checked 1 Nov).
   What the owner's Redmi can still cover (told to the owner 1 Nov): 5.1 on a third-party
   launcher (install Lawnchair). Not 5.4's legacy half (placing the old widget needs an older
   build installed over 1.10.x, i.e. a downgrade that wipes app data: emulator only) and not
   5.4c (no lock-screen widgets on its PixelOS: a Pixel or a Pixel emulator image).
8. Merged branches: `claude/handover-docs-ntkjix` is deleted (owner, 3 Nov), but ~50 other merged
   `claude/*` branches are still on origin (checked 3 Nov), so auto-delete of head branches looks
   off. Earlier note: only `claude/handover-docs-ntkjix` was left. The owner said go ahead
   (31 Oct), but the cloud git proxy refuses `git push --delete`; suggested to the owner:
   turn on "Automatically delete head branches" in the repo settings and delete the leftover
   once by hand.

## Decided earlier

Decided 31 Oct (1.10.0): listening review, virtue hadiths (reading cards), evening wording
(footnote), source errors (fixed from the book's own data; HadeethEnc 5491/6163 were never
imported), microphone in Memorise (no), #107 (disable the old widget).

- **IslamHouse licence**: request sent 23 Oct (`docs/islamhouse-licence-request.md`); answered,
  see "IslamHouse licence reply" above.
  IslamHouse publishes no email address (only the 500-character contact form). Owner's choice
  (31 Oct): plan B, a Play build without the hisnmuslim.com English; see `docs/play-store.md`,
  "Plan B". Merged in #118 (option A, 1 Nov); the rest is open work item 1.
- **Cloud environment**: decided (owner, 3 Nov): stay on Default. Each session attaches
  haho-design (`add_repo`) and runs `tools/cloud_setup.sh` (~2 min). The public docs show no
  Repositories field on any environment.
- **Oppo user's widget feedback** via Diagnostics (shipped in 1.5.0): no reply recorded.


## Facts that cost time to find

- Promo (5 Nov): CI screenshots use the font-test string "هٰذَا نَصٌّ تَجْرِيبِيٌّ..." (reads "this is test
  text"), not a dua; `marketing/tools/prep_screens.py` turns it into empty boxes. The Google Drive
  connector takes file content inline only, so multi-MB media can't go to Drive from a session.
  Notion: settings show all tools "Always", but only setup/meeting/sidebar tools reached sessions.
- Content (main after 1.7.0): 323 rows; 87 with phrase breaks; 17 without audio
  (sunnah_01–16, funeral_01); 52 with a grade; rows without Latin are mostly the ones whose
  `arabic` still carries unvowelled narration (صلى الله عليه وسلم) or basmalah.
- The English "( 1 )" / "[Soorah …]" are mostly verse numbers and surah headings in the Qur'an
  translations: information, not junk.
- sunnah.com, api.sunnah.com and dorar.net return 403 from cloud sessions (checked 31 Oct);
  hadeethenc.com and cdn.jsdelivr.net (fawazahmed0/hadith-api, Arabic of the six books with
  numbers and Al-Albani grades, public domain) answer 200.
- Screenshots workflow race: it commits to the branch it checked out, so a push to the branch
  while it runs makes its push fail. Push first, then start it (or cancel and restart it).
- Phrase rendering is all in `feature/duas/components/HighlightedArabic.kt` (`PhraseLines`:
  column, flow, focus; fills and the gold active phrase). Reading settings composables are in
  `feature/settings/ReadingSettings.kt`, used by both Display settings and the dua page sheet.
- Connecting the owner's Mac: the desktop-app path "Settings → Claude Code → Remote Control"
  does not exist in the owner's app (31 Oct); `claude remote-control` in a terminal in the
  clone is the way.
- `split_context.py` proposals are often wrong (it only drops the narrator); check every row.
  Values are cut from the source between anchors, never typed: see #83's approach.
- islamweb: Majma' az-Zawa'id is book 87, Fayd al-Qadir 304. Library search: POST
  `https://www.islamweb.net/ar/SearchEngine/explibtab.php` with `bookid=-1&collection=4&txt=…&R1=1&R2=0`
  (capped ranked list: a miss proves nothing). Page of a printed volume:
  `pageno_redirect.php?part=<vol>&bk_no=<book>&pageno=<page>`.
- hisnmuslim.com's API gives entry 153's audio as `audio/ar/.mp3` (no number; 3 Nov); the
  file is at `audio/ar/153.mp3` like every other. Check a missing recording there before
  concluding the site has none.
- hisnmuslim.com has Indonesian as HTML only (`/i/id/<chapter>`); its API has ar and en.
- Empty occasions (entering_home, drinking) are already hidden in the library.
- HadeethEnc (#81, from the fitrahive/dua-dhikr analysis): its grades name no grader, 4 entries
  have no hadith numbers; 8 candidates were left out (e.g. no. 2932 is an instruction).
- Routine recall: a correct recital that is not due leaves the schedule alone, so daily adhkar
  don't climb a box every day (#79).

## Devices

- The owner's phone: Redmi Note 10 Pro on PixelOS 17 (AOSP, not MIUI); runs the 1.9.0 debug
  build with exact alarms allowed (1 Nov). It has no lock-screen widgets (no switch under Lock
  screen, no hub mode under Screen saver, SystemUI's widget editor isn't exported), so 5.4c
  needs a real Pixel. Device QA needs a desktop
  or Remote Control session on the owner's MacBook (cloud sessions can't reach USB adb).
- Xiaomi battery manager / launcher and Samsung font checks need stock phones; Android 8–11
  checks (5.4b) need an emulator.
- Emulators on the Mac: set both `ANDROID_HOME` and `ANDROID_SDK_ROOT` to
  `~/Library/Android/sdk` (the Homebrew SDK has no system images). `qa_api26_widget` is
  Android 8; `adb root`, `settings put global auto_time 0` and `date MMDDhhmmYYYY.ss` set its
  clock to just before a prayer (put `auto_time` back to 1 after). Never clear data or run
  connected tests on the owner's phone.
- On the Mac: `handoff.md` in the local clone is git-excluded, and an older handover is in
  `~/Library/Application Support/Claude/side-session-notes/`; this file supersedes both.

## Sessions (archive)

  owner says they're clearing: update this file, merge it, then reply "Safe to clear" with the
  next model, a one-line reason and the prompt ("Continue from docs/handover.md." + task).

- Standing instruction from the owner (31 Oct): work on Adwam only. Don't read, archive or
  change sessions, repos or artifacts of other projects; the archiving notes below were
  one-off requests.
- 31 Oct (after #94): the owner asked to archive the other sessions. Archived Inverse
  Construction S3, Whittle S3, Drive Folder Sync S2, Speed Meter S2 and Smol; kept F052 Harbour
  (Crust, working) and OpenRef S3 (a long re-take job). Their open items are in the doc
  https://claude.ai/artifact/TnBH5bNS1iAFZNm1BtQHSP ("Pending across projects").
- 31 Oct: the owner asked for idle sessions in other projects to be archived. Archived the
  ones a later session in the same project had replaced (cRust 1.2, OpenRef S1/S2, Whittle
  S1/S2, Drive Folder Sync S1, Speed Meter S1, Inverse Construction S1/S2) and two stale
  one-offs (skills setup, installed skills list). Kept the latest session of each project.

## Cloud setup, details

- `tools/cloud_setup.sh` is the environment's whole setup script (Android SDK 37, Maven Central
  mirror for Gradle, haho-design at the version `gradle/libs.versions.toml` pins, published to
  `~/.m2`). 2 Nov: it is the Default environment's script plus the pinned haho-design version (Default's
  copy still pins 0.1.0 unless the owner pasted the new one). The **Default environment has no Repositories section**,
  so its script can't clone the private haho-design (its step fails quietly). The public docs
  (code.claude.com/docs/en/cloud-environments) list no Repositories field for any environment. A session's own repo list
  (side panel) is not the environment's: a repo attached with `add_repo` shows there too.
  Check at start: `ls ~/.m2/repository/id/haho/haho-design/`. If the version is missing:
  `add_repo hystericmoon/haho-design`, then `bash tools/cloud_setup.sh` (safe to re-run).
  `create_session` takes one repo and inherits the caller's environment.
- `export ANDROID_HOME=/opt/android-sdk`; the first Gradle build takes several minutes.
- Maven Central and sunnah.com may refuse direct `curl` (429 / 403); Gradle resolves fine.
- Whisper with transformers 5 loads in float16 by default: ~6x slower on CPU (72 s against 12 s
  for a 10 s clip); both tools now pass `dtype=torch.float32`. `recut_audio.py words` ran out of
  memory (16 GB) on quran_13 (30 s, word timestamps); mode `end` reads the loudness from the file
  instead. `recut_audio.py start` re-runs every fix in the file and travel_01's `join` joins
  again on a second run: always `apply --id <new rows>` and check the CSV diff.
- Run the content tests from `tools/` (`cd tools && python3 -m unittest discover -s .`); from the
  repo root `python3 -m unittest tools.test_x` fails to import.
- Whisper venv for `verify_audio.py` / `recut_audio.py` (not in the setup script: ~2 GB):
  `python3 -m venv /tmp/wvenv && /tmp/wvenv/bin/pip install --index-url
  https://download.pytorch.org/whl/cpu torch && /tmp/wvenv/bin/pip install transformers numpy`.
