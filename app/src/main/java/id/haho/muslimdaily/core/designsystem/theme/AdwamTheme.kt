package id.haho.muslimdaily.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.HahoTheme
import id.haho.design.theme.ThemeMode
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.isDark

/**
 * Adwam's theme: the shared haho-design system ([HahoTheme]: palettes, glass, Plus Jakarta Sans,
 * spacing, motion) plus what only Adwam needs, the Arabic type scale and the gold highlight and
 * count-complete colours.
 */
@Composable
fun AdwamTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    palette: HahoPalette = HahoPalette.Default,
    arabicScale: Float = 1f,
    translationScale: Float = 1f,
    glass: Boolean = true,
    arabicFont: ArabicFont = ArabicFont.NASKH,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    HahoTheme(
        palette = palette,
        themeMode = themeMode,
        glass = glass,
        dynamicColor = dynamicColor,
        readingScale = translationScale,
    ) {
        val arabic = remember(arabicFont, arabicScale) { ArabicTypography.of(arabicFont).scaled(arabicScale) }
        CompositionLocalProvider(
            LocalArabicTypography provides arabic,
            LocalExtendedColors provides remember(palette, dark) { extendedColors(palette, dark) },
            content = content,
        )
    }
}

/** Adwam's theme values beyond [HahoTheme]: Arabic typography and the gold roles. */
object AdwamTheme {
    val arabic: ArabicTypography
        @Composable @ReadOnlyComposable get() = LocalArabicTypography.current
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}

/** Sizes only Adwam's screens use; the shared ones are in [HahoTheme.dimens]. */
object AdwamDimens {
    val playerMainButton: Dp = 72.dp
    val completeDisc: Dp = 56.dp
    val highlightUnderline: Dp = 2.dp
    val timelineDot: Dp = 10.dp
    val placeholderWordHeight: Dp = 28.dp
}

/** Gold, used only as a fill or a non-text indicator. */
internal val Gold = Color(0xFFC9A227)

/** Gold tones 90 / 30 / 10 (the active-phrase fill and the text on it). */
private val GoldTone90 = Color(0xFFFFE08E)
private val GoldTone30 = Color(0xFF584400)
private val GoldTone10 = Color(0xFF241A00)

/** Colour roles M3 does not cover. Gold appears only as fill or non-text indicator. */
@Immutable
data class ExtendedColors(
    val highlightFill: Color,
    val onHighlightFill: Color,
    val highlightIndicator: Color,
    val completeFill: Color,
    val onCompleteFill: Color,
)

/**
 * The gold roles for [palette]. They are the same in every palette except the light-mode
 * underline, which uses the palette's primary because raw gold on a light surface is below 3:1.
 * Dynamic colour does not change them.
 */
fun extendedColors(palette: HahoPalette, dark: Boolean) = ExtendedColors(
    highlightFill = if (dark) GoldTone30 else GoldTone90,
    onHighlightFill = if (dark) GoldTone90 else GoldTone10,
    highlightIndicator = if (dark) Gold else colorSchemeFor(palette, dark = false).primary,
    completeFill = Gold,
    onCompleteFill = GoldTone10,
)

val LocalExtendedColors = staticCompositionLocalOf { extendedColors(HahoPalette.Default, dark = false) }
