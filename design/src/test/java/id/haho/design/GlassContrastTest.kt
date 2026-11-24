package id.haho.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import id.haho.design.glass.GlassColors
import id.haho.design.glass.glassColors
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.seedColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 AA (4.5:1) for text on every glass role, composited over the worst-case point of the
 * backdrop (plain base, each glow at its centre, and all glows stacked). Covers the eight
 * palettes, the grey-surface variant and a spread of custom seeds.
 */
class GlassContrastTest {

    /** Text sits at about 30% of a button's height; the sheen there is partly faded. */
    private fun sheenAtText(sheen: Color, stop: Float = 0.38f, at: Float = 0.3f): Color =
        sheen.copy(alpha = sheen.alpha * (1f - at / stop).coerceAtLeast(0f))

    private fun check(name: String, text: Color, fill: Color, g: GlassColors, extra: Color = Color.Transparent) {
        for (bg in g.backdropExtremes()) {
            val surface = extra.compositeOver(fill.compositeOver(bg))
            val r = ratio(text.compositeOver(surface), surface)
            assertTrue("$name: %.2f:1 over $bg".format(r), r >= 4.5)
        }
    }

    private fun verify(scheme: ColorScheme, dark: Boolean, label: String, glass: Boolean = true) {
        val g = glassColors(scheme, dark, enabled = glass)
        val mode = "$label " + (if (dark) "dark" else "light") + (if (glass) "" else " glass-off")
        for ((name, text) in listOf("onSurface" to scheme.onSurface, "onSurfaceVariant" to scheme.onSurfaceVariant, "primary" to scheme.primary)) {
            check("$mode card/$name", text, g.card, g, g.sheen)
            check("$mode chrome/$name", text, g.chrome, g, g.sheen)
            check("$mode backdrop/$name", text, Color.Transparent, g)
        }
        check("$mode tinted/onTinted", g.onTinted, g.tinted, g, g.sheen)
        check("$mode selected row", scheme.onSurface, g.card, g, g.tinted)
        // An alert banner inside a tinted hero card, and an alert badge on a plain card.
        check("$mode alert in hero", g.onAlert, g.tinted, g, g.alert)
        check("$mode alert badge", g.onAlert, g.card, g, g.alert)
        check("$mode tertiary note", scheme.onTertiaryContainer, scheme.tertiaryContainer.copy(alpha = g.tinted.alpha), g, g.sheen)
        check("$mode primary button", g.onPrimary, g.primary, g, sheenAtText(g.sheen.copy(alpha = g.sheen.alpha * 0.6f)))
        check("$mode tonal button", g.onTonal, g.tonal, g, sheenAtText(g.sheen))
        check("$mode outlined button", scheme.primary, g.clear, g, sheenAtText(g.sheen))
        // Sheets cover arbitrary content: check against black and white behind them.
        for (behind in listOf(Color.Black, Color.White)) {
            val s = g.sheet.compositeOver(behind)
            for (text in listOf(scheme.onSurface, scheme.onSurfaceVariant, scheme.primary)) {
                val r = ratio(text.compositeOver(s), s)
                assertTrue("$mode sheet over $behind: %.2f:1".format(r), r >= 4.5)
            }
        }
    }

    @Test fun lightGlassMeetsAa() = HahoPalette.entries.forEach { verify(colorSchemeFor(it, dark = false), dark = false, it.name) }

    @Test fun darkGlassMeetsAa() = HahoPalette.entries.forEach { verify(colorSchemeFor(it, dark = true), dark = true, it.name) }

    @Test fun glassOffMeetsAa() = HahoPalette.entries.forEach { p ->
        for (dark in listOf(false, true)) verify(colorSchemeFor(p, dark), dark, p.name, glass = false)
    }

    @Test fun greySurfacesMeetAa() = HahoPalette.entries.forEach { p ->
        for (dark in listOf(false, true)) verify(colorSchemeFor(p, dark, greySurfaces = true), dark, "${p.name} grey")
    }

    @Test fun customSeedsMeetAa() = SampleSeeds.forEach { seed ->
        for (dark in listOf(false, true)) for (grey in listOf(false, true)) {
            verify(seedColorScheme(seed, dark, greySurfaces = grey), dark, "seed $seed${if (grey) " grey" else ""}")
        }
    }
}
