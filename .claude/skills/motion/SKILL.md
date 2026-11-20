---
name: motion
description: Add or change animation in Adwam (Compose screens, sheets, the island, transitions, counters, check marks). Use before touching any animation, transition, spring or haptic. Sonnet is enough.
---

# Motion in Adwam

Motion that feels alive without pulling attention from reading or reciting. Decided with the
owner on 18 Nov 2026 (design page: https://claude.ai/artifact/1tcpHAeEbosZC7pgrSG768, private to
the owner). At most **one signature motion per screen**.

## Speeds and curves

| Name | Duration | Use |
|---|---|---|
| Instant | 90 ms | Touch feedback: press, toggle, row highlight |
| Quick | 160 ms | Things that appear: menus, the island pill, hints |
| Standard | 260 ms | Cards expanding, sheets, tab changes |
| Slow | 420 ms | Rare one-off moments: a ring filling, first launch |

Curves (Compose `CubicBezierEasing`): arrive `(0.05, 0.7, 0.1, 1.0)`, leave `(0.3, 0.0, 0.8, 0.15)`
(and shorter than the arrival), move across the screen `(0.2, 0.0, 0.0, 1.0)`. Springs only for
physical things (sheet, island, Qibla needle): `spring(dampingRatio = 0.8f, stiffness =
Spring.StiffnessMediumLow)`. Keep these as shared constants in one file under
`core/designsystem/` (create it with the first motion change); don't repeat numbers per screen.

## Rules

1. Animate `graphicsLayer` transform and alpha only, never layout size. Resizing a window or a long
   list per frame is what made the island crawl (fixed in #298: size snaps, content fades and scales).
   For overlay windows also set `FLAG_HARDWARE_ACCELERATED`.
2. Use Compose animation APIs (`animate*AsState`, `tween`, `spring`, `AnimatedContent`): they follow
   the system animator scale, so Reduce motion (`ANIMATOR_DURATION_SCALE == 0`) snaps them, which is
   already the rule in `docs/accessibility.md`. State must still change when motion is off; never
   hide information behind an animation. No `Handler` timers or hand-rolled frame loops.
3. No idle or looping animation, except a countdown tick. People read and recite here.
   One owner-approved exception (20 Nov 2026): the island's travelling light (`travellingLight` in
   `island/IslandOverlay.kt`). It is an overlay on top of other apps, not a screen where people read; it
   runs only while a card is open, draws in the draw phase only, is still when animations are off, and
   has a Glow switch. Don't copy it into app screens.
4. Arabic text never moves, scales or fades while it is being read. Highlights may glide; text may not.
5. Pair motion with a haptic only for confirmations (mark as read, Qibla aligned), and respect the
   existing `haptics` setting.
6. No confetti, flames, bounce for reward, streak effects: Adwam has no gamification.
7. Touch targets stay 48 dp while animating; don't shrink a target as part of an effect.
8. Check every new motion in a screenshot at 200% font, in light and dark, and with Reduce motion on
   (screenshots show the end state; say what the motion is in the PR).

## Signature moments (the plan, in order; one per release is fine)

1. **Today**: a thin ring or line fills between the previous and next prayer; the "Now" highlight
   slides between prayer rows instead of jumping (one shared highlight, transform only).
2. **Mark as read**: the check draws itself, one soft scale (1.0, 1.08, 1.0), one haptic tap.
3. **Qibla**: damped needle; pulses once and gives one haptic when aligned, then stays still.
4. **Screens**: tab and screen changes use a short fade plus an 8 dp slide (Standard).
5. **Empty states, onboarding**: staggered fade-in, 40 ms apart, top to bottom.
6. **Listen and Routine**: the gold phrase highlight glides to the next phrase (scroll already follows).

Status of each: `docs/handover.md`. Related decisions: `docs/roadmap-2026-11.md`.
