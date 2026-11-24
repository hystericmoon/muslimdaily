package id.haho.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.paletteTokens
import id.haho.design.theme.seedColorScheme
import id.haho.design.theme.seedTokens
import id.haho.design.theme.toColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class SeedSchemeTest {

    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary, "onPrimary" to onPrimary, "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer, "inversePrimary" to inversePrimary,
        "secondary" to secondary, "onSecondary" to onSecondary, "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer, "tertiary" to tertiary, "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer, "onTertiaryContainer" to onTertiaryContainer,
        "error" to error, "onError" to onError, "errorContainer" to errorContainer, "onErrorContainer" to onErrorContainer,
        "background" to background, "onBackground" to onBackground, "surface" to surface, "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant, "onSurfaceVariant" to onSurfaceVariant, "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface, "inverseOnSurface" to inverseOnSurface, "outline" to outline,
        "outlineVariant" to outlineVariant, "scrim" to scrim, "surfaceBright" to surfaceBright, "surfaceDim" to surfaceDim,
        "surfaceContainerLowest" to surfaceContainerLowest, "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer, "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    private fun channelDistance(a: Color, b: Color): Int {
        val x = a.toArgb()
        val y = b.toArgb()
        return (0..16 step 8).maxOf { abs(((x shr it) and 0xFF) - ((y shr it) and 0xFF)) }
    }

    /**
     * The runtime seed pipeline reproduces the generated palettes (from the JS tool), so a custom
     * seed gets exactly the treatment the built-in palettes get. One step of rounding is allowed
     * between the JS and Kotlin ports of Material Color Utilities.
     */
    @Test fun seedPipelineReproducesBuiltInPalettes() {
        val teal = HahoPalette.TEAL
        for (p in HahoPalette.entries) for (dark in listOf(false, true)) {
            val surface = if (p == teal) (if (dark) 0xFF121412 else 0xFFFBF8F1).toInt() else null
            val generated = seedTokens(p.seed.toArgb(), dark, neutral = p == HahoPalette.SLATE, greySurfaces = false, surfaceOverride = surface)
                .toColorScheme(dark).roles()
            val baked = paletteTokens(p, dark).toColorScheme(dark).roles()
            for ((role, c) in baked) {
                val d = channelDistance(c, generated.getValue(role))
                assertTrue("${p.name} dark=$dark $role: baked $c vs seed ${generated[role]}", d <= 1)
            }
        }
    }

    @Test fun greySeedPicksNeutralVariant() {
        // Slate's seed is grey enough to be detected as neutral by default.
        for (dark in listOf(false, true)) {
            val auto = seedColorScheme(HahoPalette.SLATE.seed, dark).roles()
            val baked = colorSchemeFor(HahoPalette.SLATE, dark).roles()
            for (role in listOf("primary", "secondary", "tertiary", "primaryContainer")) {
                assertTrue(role, channelDistance(auto.getValue(role), baked.getValue(role)) <= 1)
            }
        }
    }

    @Test fun greySurfacesHaveNoHue() {
        for (p in HahoPalette.entries) for (dark in listOf(false, true)) {
            val s = colorSchemeFor(p, dark, greySurfaces = true)
            for (c in listOf(s.surface, s.surfaceContainerLow, s.surfaceContainer, s.surfaceContainerHighest, s.outline, s.onSurfaceVariant)) {
                val argb = c.toArgb()
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                assertTrue("${p.name} $c is not grey", max(abs(r - g), max(abs(g - b), abs(r - b))) <= 1)
            }
            // The accent keeps its colour.
            assertEquals(colorSchemeFor(p, dark).primary, s.primary)
        }
    }
}
