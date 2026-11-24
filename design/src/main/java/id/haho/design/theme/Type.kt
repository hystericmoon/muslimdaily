package id.haho.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import id.haho.design.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    R.font.haho_plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Plus Jakarta Sans (variable, 400 to 700; SIL OFL 1.1). */
val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
)

private val base = Typography()

/** Returns [this] style set in [family]. */
private fun TextStyle.inFamily(family: FontFamily) = copy(fontFamily = family)

/**
 * The M3 type scale mapped onto [family]. Sizes and line heights stay at M3 defaults; headlines,
 * titles and labels are SemiBold.
 */
fun hahoTypography(family: FontFamily = PlusJakartaSans): Typography = Typography(
    displayLarge = base.displayLarge.inFamily(family),
    displayMedium = base.displayMedium.inFamily(family),
    displaySmall = base.displaySmall.inFamily(family),
    headlineLarge = base.headlineLarge.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.inFamily(family),
    bodyMedium = base.bodyMedium.inFamily(family),
    bodySmall = base.bodySmall.inFamily(family),
    labelLarge = base.labelLarge.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.inFamily(family).copy(fontWeight = FontWeight.SemiBold),
)

/** The default type scale: M3 sizes in Plus Jakarta Sans. */
val HahoTypography: Typography = hahoTypography()

/** Scales every style in the type scale; used for an in-app reading-size setting. */
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
