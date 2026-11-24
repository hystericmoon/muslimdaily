package id.haho.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot
import kotlin.math.max

/**
 * The Material colour scheme for [palette] in light or dark.
 *
 * With [greySurfaces] the palette keeps its accent roles but every surface, container and
 * outline is a pure neutral grey (chroma 0), for apps whose surfaces must not take on a hue.
 */
fun colorSchemeFor(palette: HahoPalette, dark: Boolean, greySurfaces: Boolean = false): ColorScheme =
    if (greySurfaces) {
        seedColorScheme(palette.seed, dark, greySurfaces = true, neutral = palette == HahoPalette.SLATE)
    } else {
        paletteTokens(palette, dark).toColorScheme(dark)
    }

/**
 * Chroma below which a seed is treated as grey and gets the neutral scheme variant (as Slate
 * does); above it, the tonal-spot variant.
 */
const val NeutralSeedChroma: Double = 16.0

/**
 * Builds a full scheme from any seed colour, the same way the eight built-in palettes are
 * generated (tools/color/generate_scheme.mjs): Material tonal spot (neutral for grey seeds),
 * surfaces tinted faintly toward the seed's hue (chroma 4), container steps from a low-chroma
 * palette on that hue. Use it to give an app its own accent.
 *
 * @param neutral force the neutral variant; by default chosen from the seed's chroma.
 * @param greySurfaces make surfaces, containers and outlines pure grey (chroma 0).
 */
fun seedColorScheme(
    seed: Color,
    dark: Boolean,
    greySurfaces: Boolean = false,
    neutral: Boolean = Hct.fromInt(seed.toArgb()).chroma < NeutralSeedChroma,
): ColorScheme = seedTokens(seed.toArgb(), dark, neutral, greySurfaces, surfaceOverride = null).toColorScheme(dark)

/** Kotlin twin of build() in tools/color/generate_scheme.mjs. */
internal fun seedTokens(
    seedArgb: Int,
    dark: Boolean,
    neutral: Boolean,
    greySurfaces: Boolean,
    surfaceOverride: Int?,
): ColorTokens {
    val hct = Hct.fromInt(seedArgb)
    val scheme: DynamicScheme = if (neutral) {
        SchemeNeutral(hct, dark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
    } else {
        SchemeTonalSpot(hct, dark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
    }
    val m = MaterialDynamicColors()
    fun DynamicColor.c() = Color(getArgb(scheme))

    val surfaceArgb = surfaceOverride
        ?: TonalPalette.fromHueAndChroma(hct.hue, if (greySurfaces) 0.0 else 4.0).tone(if (dark) 7 else 98)
    val sHct = Hct.fromInt(surfaceArgb)
    val n = TonalPalette.fromHueAndChroma(sHct.hue, if (greySurfaces) 0.0 else max(sHct.chroma, 2.0))
    fun t(tone: Int) = Color(n.tone(tone))
    val surface = Color(surfaceArgb)
    val onSurface = t(if (dark) 90 else 10)

    // Outlines follow the surfaces when they are grey, so no hue leaks into boundaries.
    val outline = if (greySurfaces) t(if (dark) 60 else 50) else m.outline().c()
    val outlineVariant = if (greySurfaces) t(if (dark) 30 else 80) else m.outlineVariant().c()
    val onSurfaceVariant = if (greySurfaces) t(if (dark) 80 else 30) else m.onSurfaceVariant().c()

    return ColorTokens(
        primary = m.primary().c(),
        onPrimary = m.onPrimary().c(),
        primaryContainer = m.primaryContainer().c(),
        onPrimaryContainer = m.onPrimaryContainer().c(),
        inversePrimary = m.inversePrimary().c(),
        secondary = m.secondary().c(),
        onSecondary = m.onSecondary().c(),
        secondaryContainer = m.secondaryContainer().c(),
        onSecondaryContainer = m.onSecondaryContainer().c(),
        tertiary = m.tertiary().c(),
        onTertiary = m.onTertiary().c(),
        tertiaryContainer = m.tertiaryContainer().c(),
        onTertiaryContainer = m.onTertiaryContainer().c(),
        error = m.error().c(),
        onError = m.onError().c(),
        errorContainer = m.errorContainer().c(),
        onErrorContainer = m.onErrorContainer().c(),
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = t(if (dark) 22 else 90),
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = m.surfaceTint().c(),
        inverseSurface = t(if (dark) 90 else 20),
        inverseOnSurface = t(if (dark) 20 else 95),
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = m.scrim().c(),
        surfaceBright = if (dark) t(24) else surface,
        surfaceDim = if (dark) surface else t(87),
        surfaceContainerLowest = t(if (dark) 4 else 100),
        surfaceContainerLow = t(if (dark) 10 else 96),
        surfaceContainer = t(if (dark) 12 else 94),
        surfaceContainerHigh = t(if (dark) 17 else 92),
        surfaceContainerHighest = t(if (dark) 22 else 90),
    )
}

internal fun ColorTokens.toColorScheme(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        error = error, onError = onError,
        errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        outline = outline, outlineVariant = outlineVariant, scrim = scrim,
        surfaceBright = surfaceBright, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh, surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow, surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        error = error, onError = onError,
        errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        outline = outline, outlineVariant = outlineVariant, scrim = scrim,
        surfaceBright = surfaceBright, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh, surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow, surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )
}
