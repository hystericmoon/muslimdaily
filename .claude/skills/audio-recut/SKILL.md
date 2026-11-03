---
name: audio-recut
description: Check or re-cut Adwam's dua audio (clip ranges, phrase segments, Whisper checks) with tools/verify_audio.py and tools/recut_audio.py. Use for any audio cut, timing or "audio plays the wrong words" task. Opus only.
---

# Audio checks and re-cuts

Needs Opus. The audio files are never edited (IslamHouse licence): every cut is a playback range
or segment in `content/duas.json`, built from the CSV.

## Tools

- `tools/verify_audio.py [--id ID ...] [--resume]`: Whisper check of each phrase; results in
  `build/align/verify.csv` (all rows ~2.5 h on CPU).
- `tools/recut_audio.py`: commands `words` (Whisper, needs the venv), `propose`, `start`,
  `apply`; read its docstring first. `start` runs the fixes in `tools/align/start_fixes.csv`,
  whose rows also carry modes (`full`, `break`, `unheard`, `join`, `end`). Whisper word times:
  `tools/align/whisper_words.json`.
- `tools/align_audio.py`: wav2vec2 forced alignment (`tools/align/word_timings.json`).

## Gotchas

- `recut_audio.py start` re-runs every fix in the CSV, and `join` joins again on a second run:
  always `apply --id <new rows>` and check the CSV diff.
- Whisper venv is not in the setup script (~2 GB):
  `python3 -m venv /tmp/wvenv && /tmp/wvenv/bin/pip install --index-url https://download.pytorch.org/whl/cpu torch && /tmp/wvenv/bin/pip install transformers numpy`.
  The tools pass `dtype=torch.float32` (float16 is ~6x slower on CPU).
- `words` ran out of memory on long clips (quran_13, 30 s); `end` reads loudness instead.
- Whisper invents text on short or silent clips ("اشتركوا في القناة", a word repeated over
  silence) and mishears first words: read such a flag as mishearing, then listen to the cut.
- Re-check every re-cut row with `verify_audio.py --id` before the PR.

## Finish

Rebuild content and run the tools tests (see the `content-import` skill, "Finish"). It changes
shipped content: ask the owner before merging. Past cuts and reasons: `docs/history.md`
(search the row id).
