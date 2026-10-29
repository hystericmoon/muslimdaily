# Licence request to IslamHouse

**Status: sent 2026-10-23 through the contact form, awaiting a reply.** The form at
https://islamhouse.com → "Contact us" allows only 500 characters, so the short message below
went instead of the full letter (the site's FAQ is https://d1.islamhouse.com/html/faq.htm). If
they ask for details, email them the full letter. Keep their reply in this folder as
`islamhouse-reply-<date>.pdf` and link it from About.

**2026-10-29:** with no reply, the owner chose to ship the English translations (point 1) under
IslamHouse's published terms, credited to hisnmuslim.com (IslamHouse.com) in About and on the
site. If IslamHouse or the translator's publisher objects, remove `translation_en` from the Hisn
rows. The Play Store release (point 2) stays on hold. The English text translates the book's
full entry, so a row later split with `tools/split_context.py` needs its English checked too.

Why: the app bundles the Hisn al-Muslim recitation by Hamad ad-Duraihim (about 26 MB, Opus) on
the strength of IslamHouse's FAQ, which allows republishing without changes and with credit. The
site's disclaimer is less clear, and two further uses need a yes before they ship:

1. The English translations served at hisnmuslim.com/api/en (no translator or licence is named
   there). `tools/hisnul_muslim/fetch_english.py` is ready but not applied.
2. A Play Store release, which is a distribution channel beyond GitHub.

## Message sent (contact form)

> Assalamu'alaikum. I maintain Adwam (formerly Muslim Daily), a free, offline, ad-free Android app
> (github.com/hystericmoon/muslimdaily). May we include, unchanged and credited to IslamHouse:
> 1. Hamad ad-Duraihim's Hisn al-Muslim audio, converted to Opus without edits
> 2. The English Hisn al-Muslim text from hisnmuslim.com (who is the translator?)
> 3. Distributed via GitHub and Google Play
>
> Your FAQ seems to allow this; we'd like written confirmation. Jazakumullahu khairan.

## Full letter (for email follow-up)

**Subject:** Permission to include Hisn al-Muslim audio and translation in a free, offline app

Assalamu'alaikum warahmatullahi wabarakatuh,

I maintain Adwam (formerly Muslim Daily), a free and open-source Android app for daily prayer times and adhkar,
mainly for Indonesian Muslims. It has no ads, accounts or tracking, and works fully offline.
Source: https://github.com/hystericmoon/muslimdaily

The app includes material published by IslamHouse, and I would like to confirm that this use is
permitted:

1. **Audio.** The Hisn al-Muslim recitation by Sheikh Hamad ad-Duraihim, one file per dhikr as
   published on hisnmuslim.com. The recordings are converted to a smaller audio format (Opus) for
   the app's size, without cutting, editing or adding anything. Each file is credited in the app
   to the reciter and IslamHouse.com.
2. **English translation.** The English text of each dhikr as served by hisnmuslim.com
   (api/en). We would show it unchanged, credited to IslamHouse. Could you tell us who the
   translator is, so we can credit them correctly?
3. **Distribution.** The app is distributed free of charge through GitHub and possibly Google
   Play.

Your FAQ says IslamHouse materials may be republished by any means, without changes and with the
source mentioned. I want to be certain the uses above, including the audio format conversion,
are within that permission. If you need any change in how the credit is shown, or if any of this
is not allowed, please tell me and I will follow it.

Jazakumullahu khairan,

[Name]
Adwam maintainer
[Email]
