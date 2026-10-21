package id.haho.muslimdaily.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import id.haho.muslimdaily.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.jakarta() = copy(fontFamily = PlusJakartaSans)

/** The M3 type scale mapped onto Plus Jakarta Sans. Sizes and line heights stay at M3 defaults. */
val AppTypography = Typography(
    displayLarge = base.displayLarge.jakarta(),
    displayMedium = base.displayMedium.jakarta(),
    displaySmall = base.displaySmall.jakarta(),
    headlineLarge = base.headlineLarge.jakarta().copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.jakarta().copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.jakarta().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.jakarta().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.jakarta().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.jakarta().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.jakarta(),
    bodyMedium = base.bodyMedium.jakarta(),
    bodySmall = base.bodySmall.jakarta(),
    labelLarge = base.labelLarge.jakarta().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.jakarta().copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.jakarta().copy(fontWeight = FontWeight.SemiBold),
)

/** Scales every style in the M3 type scale; used for the translation-size setting. */
fun Typography.scaled(factor: Float): Typography {
    if (factor == 1f) return this
    fun TextStyle.s() = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
    return Typography(
        displayLarge.s(), displayMedium.s(), displaySmall.s(),
        headlineLarge.s(), headlineMedium.s(), headlineSmall.s(),
        titleLarge.s(), titleMedium.s(), titleSmall.s(),
        bodyLarge.s(), bodyMedium.s(), bodySmall.s(),
        labelLarge.s(), labelMedium.s(), labelSmall.s(),
    )
}
