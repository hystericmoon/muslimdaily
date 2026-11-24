package id.haho.muslimdaily.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import id.haho.design.glass.glassColors
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.muslimdaily.core.designsystem.theme.extendedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Adwam's gold roles (active-phrase highlight, count-complete disc) and the third accent in every
 * palette, light and dark: text at 4.5:1, the underline indicator at 3:1 (WCAG 2.2 AA). The
 * palettes' own roles and the glass are tested in `:design`.
 */
class ExtendedColorsContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (max(la, lb) / min(la, lb)).toDouble()
    }

    private fun assertRatio(name: String, fg: Color, bg: Color, needs: Double) {
        val r = ratio(fg, bg)
        assertTrue("$name: %.2f:1 (needs $needs:1)".format(r), r >= needs)
    }

    @Test fun goldRolesMeetAa() {
        for (palette in HahoPalette.entries) for (dark in listOf(false, true)) {
            val c = extendedColors(palette, dark)
            val surface = colorSchemeFor(palette, dark).surface
            val mode = "$palette " + if (dark) "dark" else "light"
            assertRatio("$mode onHighlightFill/highlightFill", c.onHighlightFill, c.highlightFill, 4.5)
            assertRatio("$mode highlightIndicator/surface", c.highlightIndicator, surface, 3.0)
            assertRatio("$mode highlightIndicator/highlightFill", c.highlightIndicator, c.highlightFill, 3.0)
            assertRatio("$mode onCompleteFill/completeFill", c.onCompleteFill, c.completeFill, 4.5)
            // The small gold marks (fasting dots, crescent) sit on the page and on cards.
            val scheme = colorSchemeFor(palette, dark)
            for ((name, bg) in listOf(
                "surface" to scheme.surface,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer,
                "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            )) assertRatio("$mode goldMark/$name", c.goldMark, bg, 3.0)
        }
    }

    /**
     * The accent carries text (the countdown, the Now chip), so it needs 4.5:1 on the page, on every
     * container and on the glass cards it sits on; the text on its own container needs 7:1.
     */
    @Test fun accentRolesMeetAa() {
        val misses = mutableListOf<String>()
        fun check(name: String, fg: Color, bg: Color, needs: Double) {
            val r = ratio(fg, bg)
            if (r < needs) misses += "$name: %.2f:1 (needs $needs:1)".format(r)
        }
        for (palette in HahoPalette.entries) for (dark in listOf(false, true)) {
            val c = extendedColors(palette, dark)
            val scheme = colorSchemeFor(palette, dark)
            val mode = "$palette " + if (dark) "dark" else "light"
            for ((name, bg) in listOf(
                "surface" to scheme.surface,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer,
                "surfaceContainerHigh" to scheme.surfaceContainerHigh,
                "surfaceContainerHighest" to scheme.surfaceContainerHighest,
            )) check("$mode accent/$name", c.accent, bg, 4.5)
            check("$mode onAccent/accent", c.onAccent, c.accent, 4.5)
            check("$mode onAccentContainer/accentContainer", c.onAccentContainer, c.accentContainer, 7.0)
            // Glass: the Next card (tinted, with the sheen) and a prayer row (card) over the worst backdrop.
            val g = glassColors(scheme, dark, enabled = true)
            for (bg in g.backdropExtremes()) {
                for ((name, fill) in listOf("tinted" to g.tinted, "card" to g.card)) {
                    val surface = g.sheen.compositeOver(fill.compositeOver(bg))
                    check("$mode accent/glass $name over $bg", c.accent, surface, 4.5)
                }
            }
        }
        assertTrue(misses.joinToString("\n"), misses.isEmpty())
    }

    /** The accent is its own colour: never the palette primary (the 30) or the gold fill. */
    @Test fun accentDiffersFromPrimaryAndGold() {
        for (palette in HahoPalette.entries) for (dark in listOf(false, true)) {
            val c = extendedColors(palette, dark)
            assertTrue("$palette dark=$dark accent equals the primary", c.accent != colorSchemeFor(palette, dark).primary)
            assertTrue("$palette dark=$dark accent equals gold", c.accent != c.completeFill)
        }
    }

    /** The values of docs/accent.md, one palette per row (spot check; the generator is tools/accent_palette.py). */
    @Test fun accentKeepsItsValues() {
        val tealLight = extendedColors(HahoPalette.TEAL, dark = false)
        assertEquals(Color(0xFF6D54A8), tealLight.accent)
        assertEquals(Color(0xFFE6E0FE), tealLight.accentContainer)
        val peachDark = extendedColors(HahoPalette.PEACH, dark = true)
        assertEquals(Color(0xFFA5C2FF), peachDark.accent)
        assertEquals(Color(0xFF000000), peachDark.onAccent)
        assertEquals(Color(0xFFAEC1E9), peachDark.onAccentContainer)
    }

    /** The book's comma (،) in the primary colour: text contrast on every surface it sits on. */
    @Test fun pauseCommaMeetsAa() {
        for (palette in HahoPalette.entries) for (dark in listOf(false, true)) {
            val scheme = colorSchemeFor(palette, dark)
            val mode = "$palette " + if (dark) "dark" else "light"
            for ((name, bg) in listOf(
                "surface" to scheme.surface,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer,
                "highlightFill" to extendedColors(palette, dark).highlightFill,
            )) assertRatio("$mode primary/$name", scheme.primary, bg, 4.5)
        }
    }

    /** The values the palette generator used to emit, before the move to the design module. */
    @Test fun goldRolesKeepTheirValues() {
        for (palette in HahoPalette.entries) {
            val light = extendedColors(palette, dark = false)
            assertEquals(Color(0xFFFFE08E), light.highlightFill)
            assertEquals(Color(0xFF241A00), light.onHighlightFill)
            assertEquals(colorSchemeFor(palette, dark = false).primary, light.highlightIndicator)
            val dark = extendedColors(palette, dark = true)
            assertEquals(Color(0xFF584400), dark.highlightFill)
            assertEquals(Color(0xFFFFE08E), dark.onHighlightFill)
            assertEquals(Color(0xFFC9A227), dark.highlightIndicator)
            for (c in listOf(light, dark)) {
                assertEquals(Color(0xFFC9A227), c.completeFill)
                assertEquals(Color(0xFF241A00), c.onCompleteFill)
            }
        }
    }
}
