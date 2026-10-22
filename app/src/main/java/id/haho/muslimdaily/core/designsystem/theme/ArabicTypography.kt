package id.haho.muslimdaily.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import id.haho.muslimdaily.R

val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

/**
 * Qur'anic text. Amiri Quran (SIL OFL) stands in for KFGQPC Uthmanic Script Hafs, whose licence
 * forbids redistribution without written approval from the King Fahd Complex. See docs/fonts.md.
 */
val AmiriQuran = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

@OptIn(ExperimentalTextApi::class)
private fun notoArabic(weight: FontWeight) = Font(
    R.font.noto_sans_arabic,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val NotoSansArabic = FontFamily(notoArabic(FontWeight.Normal), notoArabic(FontWeight.SemiBold))

/** Line height for vocalised text: tashkeel above and below needs ~1.9x the font size. */
private const val TASHKEEL_LINE_HEIGHT = 1.9f

private fun arabicStyle(family: FontFamily, size: TextUnit, lineHeightFactor: Float = TASHKEEL_LINE_HEIGHT) = TextStyle(
    fontFamily = family,
    fontSize = size,
    lineHeight = size * lineHeightFactor,
    textDirection = TextDirection.Rtl,
    localeList = LocaleList("ar"),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/** Arabic type scale, separate from the Latin M3 scale. Exposed through [LocalArabicTypography]. */
@Immutable
data class ArabicTypography(
    val arabicDisplay: TextStyle = arabicStyle(Amiri, 34.sp),
    val arabicLarge: TextStyle = arabicStyle(Amiri, 28.sp),
    val arabicBody: TextStyle = arabicStyle(Amiri, 24.sp),
    val arabicSmall: TextStyle = arabicStyle(Amiri, 20.sp),
    val quranLarge: TextStyle = arabicStyle(AmiriQuran, 28.sp, 2.1f),
    val quranBody: TextStyle = arabicStyle(AmiriQuran, 24.sp, 2.1f),
    /** Arabic UI labels (e.g. a prayer name shown in Arabic). Not vocalised, normal leading. */
    val arabicLabel: TextStyle = arabicStyle(NotoSansArabic, 14.sp, 1.5f),
) {
    /** Qur'anic text uses the Qur'an face; everything else uses Amiri. */
    fun large(isQuran: Boolean) = if (isQuran) quranLarge else arabicLarge
    fun body(isQuran: Boolean) = if (isQuran) quranBody else arabicBody

    fun scaled(factor: Float): ArabicTypography {
        if (factor == 1f) return this
        fun TextStyle.s() = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
        return ArabicTypography(
            arabicDisplay.s(), arabicLarge.s(), arabicBody.s(), arabicSmall.s(),
            quranLarge.s(), quranBody.s(), arabicLabel.s(),
        )
    }
}

val LocalArabicTypography = staticCompositionLocalOf { ArabicTypography() }
