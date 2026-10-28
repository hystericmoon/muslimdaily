# Fonts

| Use | Typeface | Licence | File |
|---|---|---|---|
| Latin UI (M3 type scale) | Plus Jakarta Sans (variable, 400–700) | SIL OFL 1.1 | haho-design (`haho_plus_jakarta_sans.ttf`) |
| Arabic duas and Qur'anic text (default) | **Noto Naskh Arabic** (variable) | SIL OFL 1.1 | `res/font/noto_naskh_arabic.ttf` |
| Arabic, reader's choice | Scheherazade New Regular | SIL OFL 1.1 | `res/font/scheherazade_new.ttf` |
| Arabic, reader's choice | Amiri Regular / Bold | SIL OFL 1.1 | `res/font/amiri_*.ttf` |
| Qur'anic text when Amiri is chosen | Amiri Quran | SIL OFL 1.1 | `res/font/amiri_quran.ttf` |
| Arabic UI labels | Noto Sans Arabic (variable) | SIL OFL 1.1 | `res/font/noto_sans_arabic.ttf` |

All come from the Google Fonts repository; licence texts are in `assets/licenses/` and shown in
Settings → About.

## Choosing the Arabic typeface (0.5.3)

Readers found Amiri hard to read on a phone. The app now offers three OFL Naskh faces in
Settings → Tampilan dan teks → Huruf Arab, each previewed in its own face, and the share image
follows the choice.

| Face | Why it is offered | Trade-off |
|---|---|---|
| **Noto Naskh Arabic** (default) | Drawn for screens: open counters, larger letter bodies, and harakat that sit clearly above and below their letters without stacking ligatures. | Plainer than a book face. |
| **Scheherazade New** | SIL designed it for fully vocalised text; its marks are the largest and most widely spaced of the three, which helps slow or beginner readers. | Wider and taller, so more line breaks. |
| **Amiri** | The classical book (Bulaq) style many readers know from printed Hisn al-Muslim. | Stacked ligatures (e.g. «الحمد») and small marks are hard to read at phone sizes. |

How the default was chosen: there is no controlled readability study comparing these faces on
phones, so the choice rests on rendering the app's own longest Hisn dua and a Qur'anic dua in
each face at reading size (`docs/screenshots/67-real-font-*.png`) and comparing how clearly each
mark can be read and attached to its letter. Published comparisons agree on the ranking in
general terms: Scheherazade New and Amiri for heavily vocalised text, Noto Naskh as the reliable
screen face, and Arabic set at least ~16–17px because it reads smaller than Latin at the same
size ([The Arabic Keyboard: Arabic fonts compared](https://www.thearabickeyboard.com/arabic-fonts/),
[Arabic fonts for the Qur'an](https://arabic-calligraphy-generator.com/fonts/for-quran),
[Quranic fonts usage guide](https://www.urdu-nigaar.com/blog/quranic-fonts-usage-guide/)).
All three faces cover the Qur'anic annotation marks used in the Tanzil text; with Amiri the
Qur'anic text keeps Amiri Quran.

Sizes: the base dua size is 23sp (Display 32, Large 26, Small 19) with a line height of 2.0×
for Naskh, so tashkeel above and below never collides. Scheherazade is set 8% larger and Amiri
4% larger so the three read at a similar visual size. The reader's text-size setting applies on
top.

## Why not KFGQPC Uthmanic Script Hafs

The spec asked for KFGQPC Uthmanic Script Hafs for Qur'anic text. Its licence reads:

> This Font is the property of King Fahd Glorious Quran Printing Complex, and may not be
> reproduced, modified without the express written approval of King Fahd Glorious Quran
> Printing Complex.

([ScanCode LicenseDB](https://scancode-licensedb.aboutcode.org/kfgqpc-uthmanic-script-hafs.html))

Bundling a font in an APK is reproduction and distribution, so shipping it needs written
approval from the Complex. Third-party listings describe it as free for personal or
non-commercial use only. The font is also designed around the Complex's own encoding of the
mushaf text, so it renders best with the Complex's Qur'an text data rather than generic
Unicode input.

**Amiri Quran** is an OFL typeface made for Qur'anic text (full Uthmani marks), works with
standard Unicode, and can be bundled without permission.

### Switching to KFGQPC later

If written approval is obtained: add the TTF as `res/font/kfgqpc_hafs.ttf` and change
`AmiriQuran` in `core/designsystem/theme/ArabicTypography.kt` to point at it. Nothing else
references the Qur'an face directly (`AppTheme.arabic.large(isQuran)` / `body(isQuran)`).
