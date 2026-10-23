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

@OptIn(ExperimentalTextApi::class)
private fun notoNaskh(weight: FontWeight) = Font(
    R.font.noto_naskh_arabic,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Noto Naskh Arabic: a Naskh drawn for screens, with open counters and clear, separated marks. */
val NotoNaskhArabic = FontFamily(notoNaskh(FontWeight.Normal), notoNaskh(FontWeight.Bold))

/** Scheherazade New (SIL): made for fully vocalised text, with large, well-spaced harakat. */
val ScheherazadeNew = FontFamily(Font(R.font.scheherazade_new, FontWeight.Normal))

/**
 * The reader's choice of Arabic typeface for duas and Qur'anic text. Each has its own size and
 * line height so the three read at a similar visual size; the user's scale applies on top.
 */
enum class ArabicFont(val family: FontFamily, val quranFamily: FontFamily, val sizeFactor: Float, val lineHeight: Float) {
    /** Default: the most even on phone screens at reading sizes. */
    NASKH(NotoNaskhArabic, NotoNaskhArabic, 1f, 2.0f),
    SCHEHERAZADE(ScheherazadeNew, ScheherazadeNew, 1.08f, 1.9f),
    /** The classical book face; the Qur'anic text keeps Amiri Quran. */
    AMIRI(Amiri, AmiriQuran, 1.04f, 1.9f),
}

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
    val arabicDisplay: TextStyle = arabicStyle(NotoNaskhArabic, 32.sp, 2.0f),
    val arabicLarge: TextStyle = arabicStyle(NotoNaskhArabic, 26.sp, 2.0f),
    val arabicBody: TextStyle = arabicStyle(NotoNaskhArabic, 23.sp, 2.0f),
    val arabicSmall: TextStyle = arabicStyle(NotoNaskhArabic, 19.sp, 2.0f),
    val quranLarge: TextStyle = arabicStyle(NotoNaskhArabic, 26.sp, 2.1f),
    val quranBody: TextStyle = arabicStyle(NotoNaskhArabic, 23.sp, 2.1f),
    /** Arabic UI labels (e.g. a prayer name shown in Arabic). Not vocalised, normal leading. */
    val arabicLabel: TextStyle = arabicStyle(NotoSansArabic, 14.sp, 1.5f),
) {
    /** Qur'anic text uses the Qur'an face; everything else uses Amiri. */
    fun large(isQuran: Boolean) = if (isQuran) quranLarge else arabicLarge
    fun body(isQuran: Boolean) = if (isQuran) quranBody else arabicBody

    companion object {
        /** The scale for [font]: base sizes 32/26/23/19sp, adjusted per typeface. */
        fun of(font: ArabicFont): ArabicTypography {
            fun size(base: Int) = (base * font.sizeFactor).sp
            return ArabicTypography(
                arabicDisplay = arabicStyle(font.family, size(32), font.lineHeight),
                arabicLarge = arabicStyle(font.family, size(26), font.lineHeight),
                arabicBody = arabicStyle(font.family, size(23), font.lineHeight),
                arabicSmall = arabicStyle(font.family, size(19), font.lineHeight),
                quranLarge = arabicStyle(font.quranFamily, size(26), font.lineHeight + 0.1f),
                quranBody = arabicStyle(font.quranFamily, size(23), font.lineHeight + 0.1f),
            )
        }
    }

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
