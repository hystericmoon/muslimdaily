package id.haho.muslimdaily.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import id.haho.muslimdaily.core.designsystem.glass.GlassColors
import id.haho.muslimdaily.core.designsystem.glass.glassColors
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.core.designsystem.theme.colorSchemeFor
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.2 AA (4.5:1) for text on every glass role, composited over the worst-case point of the
 * backdrop (plain base, each glow at its centre, and all glows stacked).
 */
class GlassContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (max(la, lb) / min(la, lb)).toDouble()
    }

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

    private fun verify(scheme: ColorScheme, dark: Boolean, palette: AppPalette) {
        val g = glassColors(scheme, dark, enabled = true)
        val mode = "$palette " + if (dark) "dark" else "light"
        for ((label, text) in listOf("onSurface" to scheme.onSurface, "onSurfaceVariant" to scheme.onSurfaceVariant, "primary" to scheme.primary)) {
            check("$mode card/$label", text, g.card, g, g.sheen)
            check("$mode chrome/$label", text, g.chrome, g, g.sheen)
            check("$mode backdrop/$label", text, Color.Transparent, g)
        }
        check("$mode tinted/onTinted", g.onTinted, g.tinted, g, g.sheen)
        check("$mode current row", scheme.onSurface, g.card, g, g.tinted)
        // The forbidden-time banner sits inside the tinted hero card; the badge sits on a plain card.
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
                val r = ratio(text, s)
                assertTrue("$mode sheet over $behind: %.2f:1".format(r), r >= 4.5)
            }
        }
    }

    @Test fun lightGlassMeetsAa() = AppPalette.entries.forEach { verify(colorSchemeFor(it, dark = false), dark = false, it) }

    @Test fun darkGlassMeetsAa() = AppPalette.entries.forEach { verify(colorSchemeFor(it, dark = true), dark = true, it) }
}
