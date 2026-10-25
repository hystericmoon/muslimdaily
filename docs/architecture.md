# Architecture

Single module, single activity, MVVM with unidirectional data flow. ViewModels expose one
`StateFlow` of immutable UI state; screens are stateless composables (`…Content` /
`…Screen`) wrapped by a `…Route` that wires the ViewModel. That split is what the previews and
screenshot tests render.

```
id.haho.muslimdaily   (Kotlin package; the app installs as id.haho.adwam)
├── core
│   ├── designsystem   theme (generated colours, both type scales, tokens), components
│   ├── model          Prayer, Dua, Phrase, Rawatib …
│   ├── arabic         ArabicNormalizer (search)
│   └── ui             previews, formatting, report-an-error
├── data
│   ├── content        JSON schema, ContentSource, importer, validator, repository
│   ├── db             ContentDatabase (rebuildable) and UserDatabase (backed up)
│   ├── settings       DataStore-backed AppSettings
│   ├── location       one-shot GPS, offline city search
│   ├── myset, run, review, checklist
├── domain             pure, unit-tested rules
│   ├── prayer         PrayerCalculator (Kemenag preset), PrayerWindows, HijriCalendar
│   ├── rawatib        RawatibPlanner (Friday, travel, variant groups)
│   ├── myset          ActiveSet (canonical order, variants, rotation)
│   └── memorize       Leitner, MemorizePlan (audio sequences), Recall (word fade)
├── audio              AudioEngine (shared ExoPlayer), PlaybackService (MediaSession)
├── notifications      ReminderPlanner, ReminderScheduler, receivers, Notifier
├── feature            today, duas (library/event/detail/search/customize), run, memorize, settings
└── navigation         type-safe routes, 4-tab shell, notification deep links
```

## Content pipeline

```
content/source/*.csv ──tools/build_content.py──▶ content/*.json ──ContentAssetsTask──▶ APK assets
                         validate, version bump        (in git)     build fails on incomplete
                                                                    entries
APK assets ──ContentImporter (checksum)──▶ Room content.db (+ FTS4) ──ContentRepository──▶ UI
```

* Three gates: the Python validator (authoring), the Gradle `ContentAssetsTask` (release
  build), and `ContentValidator` at import (defence in depth).
* Re-import happens when the files' checksum changes (content version bump or new build).
* `content.db` is disposable and excluded from backup; `user.db` (My set, sessions, checklist,
  review boxes) and settings are backed up. User data references content by stable ids.

### Content packs (future)

`ContentSource` is the seam. A signed pack would add `PackContentSource`:

1. Download only on explicit user action (this needs the `INTERNET` permission, which the app
   does not request today).
2. Pack = zip of `duas.json`, `rawatib.json`, `taxonomy.json`, `audio/*` plus `manifest.json`
   (content version, file SHA-256s) and an Ed25519 signature over the manifest.
3. Verify the signature with a public key compiled into the app, then every file hash, before
   returning bytes. Reject packs whose version is not newer.
4. The importer is unchanged; malformed entries are still rejected.

## Audio

`AudioEngine` owns one ExoPlayer (speech attributes, audio focus, becoming-noisy handling,
wake lock). Playback is a list of `SequenceItem`s: `Clip` (a dua file clipped to segment
timestamps) or `Pause` (a clipped silent asset), so memorisation runs as one continuous
queue that keeps playing with the screen off. `PlaybackService` exposes the player through a
`MediaSession` (notification, lock screen, headphone buttons). The active phrase is derived
from the playback position and the dua's segments.

## Reminders

`ReminderPlanner` (pure) computes the next time for each enabled reminder from settings and
prayer times. `ReminderScheduler` keeps one alarm per type (exact when allowed, otherwise
inexact) and is re-run after each delivery, on boot / time / time-zone / package-replaced /
exact-alarm-permission broadcasts, whenever relevant settings change (`ReminderSync`), and
by a 12-hour WorkManager safety net.

## Privacy and trust rules in code

* No `INTERNET` permission. Location is coarse, one-shot, stored locally.
* Worship counts (`run_sessions`) are per day and pruned; memorisation loops live only in
  `MemorizePlan`. The checklist is pruned daily unless the user enables history.
* No streaks, badges, leaderboards or sharing of worship statistics anywhere.
