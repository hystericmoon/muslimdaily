# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Indonesian Muslims who follow the manhaj salaf and want a calm daily guide on their phone.
Most read the app in Indonesian (the default, also on English phones); English is available.
They check prayer times many times a day, often in a glance at the home screen between other
tasks.

## Product Purpose

Adwam (أدوم, formerly Muslim Daily) is a native Android daily Muslim guide: prayer times, sunnah
rawatib, morning and evening adhkar, and authentic duas from waking to sleeping, with Arabic,
translation, audio and a memorisation mode. Success is a user who keeps small deeds constant,
as the name says ("the most constant").

The home-screen widget's job: answer "which prayer is next, and how long until it?" at a glance,
with today's Gregorian and Hijri date as context.

## Positioning

Prayer times computed on the device with the Kemenag RI method (elevation-corrected, within ±1
minute of the official schedule in tested cities), sourced religious content only (sahih/hasan,
every dua with its source), fully offline, with no account, ads, analytics or gamification.

## Operating Context

- Sideloaded through GitHub Releases (`id.haho.muslimdaily.debug`, moving to `id.haho.adwam`);
  an in-app updater checks once a day.
- The widget is one Glance widget that resizes from 1×1 to 5×3 on any launcher, and can sit on
  the lock screen where the phone allows it (Pixel-style lock-screen hubs; Samsung's One UI lock
  screen does not list third-party widgets).
- Widgets are drawn by the launcher from RemoteViews: no custom fonts from the app, no live
  animation except a system Chronometer, updates only when the app pushes one (an alarm at each
  prayer time and on settings changes, plus a 30-minute period). Without the exact-alarm
  permission, alarms can land up to 10 minutes late.

## Capabilities and Constraints

- Indonesian is the default language; every string exists in Indonesian and English.
- On Fridays Dhuhr is shown as Jumu'ah ("Jumat" in Indonesian, which is also the word for Friday).
- The widget must keep: the time-of-day sky, every size from 1×1 to 5×3, and lock-screen support
  (owner, 2026-10-25).

## Brand Commitments

- Name: Adwam (أدوم), "the most constant". The mark is Wasl (a line that loops once and carries
  on, with a small gold dot); rules in `docs/brand/README.md`.
- Voice: calm, plain, never hype. No streaks, points or badges.
- Never write or generate religious content; it comes only from `content/source/*.csv`.

## Evidence on Hand

- Screenshot set of every screen and every widget size: `docs/screenshots/` (80–89 are the widget).
- Brand files: `docs/brand/`.
- No user research, testimonials or usage data exist; none may be invented.

## Product Principles

1. A glance is enough: the next prayer and the time left come first, everything else is quiet.
2. Calm over busy: fewer words, no ornament, nothing that competes with the answer.
3. Truthful and sourced: times are computed honestly, religious text only from sources.
4. Offline and private: nothing leaves the device.

## Accessibility & Inclusion

WCAG 2.2 AA: text contrast ≥ 4.5:1, state never by colour alone, ≥ 48dp targets, TalkBack labels,
200% font without clipping (see `docs/accessibility.md`).
