# Adding kajian schedules (Ihrish): assessment

**Question:** can we build what [ihrish.app](https://ihrish.app/) does right into the app?

## What Ihrish is

* A web app (Astro, static pages on Cloudflare, installable as a PWA) listing Islamic study
  sessions (*kajian*) in Indonesia, grouped by masjid, with filters by day, prayer time and
  topic, directories of ustadz / masjid / city, a map of about 60 mosques, and "nearest first"
  using the device location.
* Run by one person (Aditio Pangestu). Data comes from monitoring mosque and organiser
  Instagram accounts: a system reads the posters, extracts date, time, speaker, theme and
  place, then a person checks each entry before it is shown. Their own page says the checks
  reduce errors but do not guarantee accuracy.
* No public API, feed or data licence. The schedule data is embedded in the pages; the About
  page says nothing about reuse, licensing or partnerships, and points to a contact page.

## Constraints on our side

1. **Offline rule (spec §0.7):** "No network calls except optional content-pack updates." A live
   kajian list is network data that changes daily, so any real integration means changing this
   rule and adding the `INTERNET` permission.
2. **Trust and manhaj:** the app's credibility rests on ustadz-reviewed content. A kajian
   directory recommends *speakers*, and Ihrish's selection is not reviewed by our ustadz. The
   app would be implicitly endorsing every listed speaker.
3. **Ownership:** copying or scraping their data without permission is not acceptable
   (their curation work, no licence, and it would break whenever their pages change).
4. **Operations:** schedules go stale within days. Building our own directory means a
   collection pipeline, human verification, corrections and moderation: a second product.

## Options

| Option | What users get | Cost | Conflicts |
|---|---|---|---|
| **A. Link out** (recommended now) | A "Jadwal kajian" row (e.g. in Settings or at the end of Today) that opens ihrish.app in the browser, with a one-line note that it is an external, independently run site | Hours | None: no network code, no permission, no endorsement of individual speakers |
| **B. Partnership feed** | A native "Kajian" list, filtered by city and nearest first using the location the app already has, cached for offline viewing | Weeks, plus an agreement with the Ihrish owner on a JSON feed, attribution and update rules | Requires amending the offline rule; needs a policy on which speakers/masjid are shown (e.g. an allow-list reviewed by our ustadz) |
| **C. Build our own** | Same as B, but our data | Ongoing team and backend | Contradicts offline / no-account design; large operational load |

## Recommendation

Do **A** for MVP. It respects the offline rule, costs almost nothing and gives users the
value today. If usage shows demand, approach Ihrish about **B**. Agree on a feed format,
attribution and a filter policy reviewed by the ustadz first, and decide explicitly to relax
the offline rule for that one feature (opt-in, clearly labelled, cached).

Not built yet. Decisions needed: (1) do you accept linking to an external site, and
(2) is the offline rule open to change for an opt-in kajian feature?
