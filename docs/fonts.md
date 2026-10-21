# Fonts

| Use | Typeface | Licence | File |
|---|---|---|---|
| Latin UI (M3 type scale) | Plus Jakarta Sans (variable, 400–700) | SIL OFL 1.1 | `res/font/plus_jakarta_sans.ttf` |
| Arabic duas | Amiri Regular / Bold | SIL OFL 1.1 | `res/font/amiri_*.ttf` |
| Qur'anic text | **Amiri Quran** | SIL OFL 1.1 | `res/font/amiri_quran.ttf` |
| Arabic UI labels | Noto Sans Arabic (variable) | SIL OFL 1.1 | `res/font/noto_sans_arabic.ttf` |

All come from the Google Fonts repository; licence texts are in `assets/licenses/` and shown in
Settings → About.

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
