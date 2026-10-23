# TalkBack checklist

Some semantics are covered by Compose UI tests (for example `RunModeUiTest`) and every screen is
screenshot at 200 % font, but focus order, announcement noise
and whether a task is actually doable can only be judged by using the app with TalkBack. Do
each task with the screen curtain on (TalkBack menu → "Hide screen") so you cannot rely on
sight. A task passes when it can be finished without the curtain coming off.

Settings: TalkBack on, speech rate default, font size 200 % for a second pass.

## Tasks

| # | Task | Watch for |
|---|---|---|
| T1 | First launch: finish setup choosing a city by hand | Each step's heading is announced; switches say "on/off" and their label once. |
| T2 | Today: find out when the next prayer is and how long until it | The hero reads name, time and countdown in one focus stop; the countdown does not re-announce every minute. |
| T3 | Today: tick the qabliyah of Dhuhr | Rows read "<name>, <rakaat>, checkbox, not checked"; expanded/collapsed is announced. |
| T4 | Start the morning adhkar and advance three items | Counter announces remaining count; "next" works with a double tap; no focus jump to the top after each item. |
| T5 | Duas: find "Doa masuk masjid" by browsing and by search | Category headings are headings (swipe by headings works). Arabic is read by the Arabic voice, translation by Indonesian. |
| T6 | Open a dua's source and grading | The source chip says what it opens; the sheet's close action is reachable. |
| T7 | Qibla: turn until aligned | Guidance is announced in 15° steps at most, plus once when aligned. Not a stream of numbers. |
| T8 | Fasting: find the next sunnah fast | Calendar days read the full date and the kind of fast; forbidden days say so. |
| T9 | Settings → Pengingat: turn on Fajr adhan | Permission prompts are announced; the exact-alarm row explains itself. |
| T10 | Isbat card (29th after Maghrib): answer it | The question and both answers are distinct; the card's disappearance does not strand focus. |
| T11 | English UI: repeat T2 and T5 | English strings read naturally; Arabic still uses the Arabic voice. |
| T12 | Tablet: switch tabs with the rail, open an occasion in the detail pane | Rail items read as tabs with selected state; focus moves into the detail pane predictably. |

## Record

For each failure note: screen, what TalkBack said, what it should have said, and whether it
blocks the task (blocker) or only slows it (friction). Blockers hold the release.
