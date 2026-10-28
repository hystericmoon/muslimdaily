package id.haho.muslimdaily.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.muslimdaily.core.designsystem.theme.extendedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Adwam's gold roles (active-phrase highlight, count-complete disc) in every palette, light and
 * dark: text at 4.5:1, the underline indicator at 3:1 (WCAG 2.2 AA). The palettes' own roles and
 * the glass are tested in haho-design.
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
        }
    }

    /** The values the palette generator used to emit, before the move to haho-design. */
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
