# Adwam promo, batch 1: brief

Owner's rules (5 Nov 2026): free channels only (Instagram, LinkedIn); no music; no faces or awrah;
no IslamHouse or HadeethEnc material, so no dua, hadith, verse or recitation in any frame, and no
readable dua card; never write religious text; claims the app proves only; no streak or points
language. The owner posts; nothing is posted from a session.

## Idea

**Hafal sedikit, tapi rutin.** Adwam means "the most constant". The promo shows the one thing other
dzikir apps don't: a four-step memorization flow (Dengar, Bangun, Ingat, Cek) with reviews scheduled
for you. The second idea is that the app is for every eye: offline, large text, tablet, light and
dark, WCAG AA.

## Look (chosen: direction A, round 2 on the decision page)

- Parchment `#FBF8F1` ground, ink type, teal `#006a65` emphasis. Gold `#C9A227` only as the mark's
  dot and focus rings, never on text.
- Plus Jakarta Sans 800 headlines with −3.5% tracking; words rise out of per-word masks.
- Real CI screenshots in a plain bezel. Readable Arabic is removed: every word becomes an empty
  hidden-word box (the app's own 100 % state); blurred words appear only for a few frames while
  they turn into boxes (`tools/prep_screens.py`).
- Light to dark: the whole frame wipes top to bottom; the wipe edge is the Wasl line with the gold
  dot leading.
- End card: the mark draws along its real centreline, the dot lands on a spring, then the outlined
  wordmark slides out of the mark (`tools/prep_mark.py`; the wordmark is never retyped).
- Motion: strong ease-out `cubic-bezier(.23,1,.32,1)` for entrances, ease-in-out
  `cubic-bezier(.77,0,.175,1)` for camera moves and wipes, springs with bounce 0.15–0.25 for pops.
  Rendered at 60 fps and blended to 30 fps for natural motion blur; light film grain.
- Sound: none. Burned-in on-screen text carries everything (feeds autoplay muted) plus `.srt`
  files in Indonesian and English. If a voiceover is ever added, use a male voice (owner).

## Audience and channels

| Channel | Who | Language | Formats |
|---|---|---|---|
| Instagram | Indonesian Muslims, 18–40, Android | Indonesian | Reels 9:16, feed 4:5, carousels 4:5 |
| LinkedIn | Developers, designers, product people; Indonesian diaspora | English | Video 4:5, images 4:5, carousels as PDF |

## Batch 1 deliverables

- Video 1 "Hafal sedikit, tapi rutin" (27.5 s): memorization steps, review, light/dark, end card.
- Video 2 "Untuk semua mata" (19.2 s): offline prayer times, 200 % text, tablet, light/dark.
- Each video: `id-9x16` (Reels), `id-4x5` (IG feed), `en-4x5` (LinkedIn), `en-9x16`; `.srt` beside each.
- 8 single images and 2 carousels (6 and 5 slides), Indonesian and English.
- Copy, alt text and a four-week calendar: `copy.md`, `calendar.csv`.

## Claims and where the app shows them

| Claim | Proof in the app |
|---|---|
| Four steps: Dengar, Bangun, Ingat, Cek | Memorize screen tabs |
| Play phrase by phrase, slowed down | Player, 0,85× |
| Add one phrase per round, repeat aloud | Build screen info text |
| Hide 30, 60, 100 % of words | Recall "Tingkat sembunyi" chips |
| Tap missed words, rate honestly | Check screen text |
| Next review scheduled for you | "Muncul lagi 1 hari lagi", "Berikutnya Rabu…" |
| Prayer times without internet; location never sent | Onboarding: "Lokasi tidak pernah dikirim ke mana pun" |
| Large text (200 %) | `font200` screenshot set |
| Tablet two columns | `102-today-tablet` |
| WCAG AA contrast | `docs/accessibility.md`, `docs/design-tokens.md` |
| Free, no ads, no tracking, open source | GitHub repo, privacy policy |
