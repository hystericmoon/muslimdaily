package id.haho.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import id.haho.design.glass.glassColors
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.seedColorScheme
import id.haho.design.theme.statusColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 AA (4.5:1) for the status colours as text on every surface they can sit on: the opaque
 * surface steps, glass cards over each backdrop extreme, the bare backdrop, and sheets over black
 * and white. Every palette, light and dark, glass on and off, grey surfaces and the sample seeds.
 */
class StatusContrastTest {

    private val failures = mutableListOf<String>()

    private fun verify(scheme: ColorScheme, dark: Boolean, label: String) {
        val status = statusColors(dark)
        for (glass in listOf(true, false)) {
            val g = glassColors(scheme, dark, glass)
            val mode = "$label ${if (dark) "dark" else "light"}${if (glass) "" else " glass-off"}"
            val surfaces = buildList {
                add("surface" to scheme.surface)
                add("containerLow" to scheme.surfaceContainerLow)
                add("container" to scheme.surfaceContainer)
                add("containerHigh" to scheme.surfaceContainerHigh)
                g.backdropExtremes().forEachIndexed { i, bg ->
                    add("backdrop#$i" to bg)
                    add("card#$i" to g.sheen.compositeOver(g.card.compositeOver(bg)))
                    add("chrome#$i" to g.chrome.compositeOver(bg))
                }
                for (behind in listOf(Color.Black, Color.White)) add("sheet/$behind" to g.sheet.compositeOver(behind))
            }
            for ((s, bg) in surfaces) {
                for ((name, fg) in listOf("success" to status.success, "warning" to status.warning, "error" to scheme.error)) {
                    val r = ratio(fg, bg)
                    if (r < 4.5) failures += "$mode $name on $s: %.2f".format(r)
                }
            }
        }
    }

    private fun assertNone() =
        assertTrue(failures.joinToString("\n", prefix = "${failures.size} failing pairs:\n"), failures.isEmpty())

    @Test fun palettesMeetAa() {
        for (p in HahoPalette.entries) for (dark in listOf(false, true)) for (grey in listOf(false, true)) {
            verify(colorSchemeFor(p, dark, greySurfaces = grey), dark, p.name + if (grey) " grey" else "")
        }
        assertNone()
    }

    @Test fun customSeedsMeetAa() {
        for (seed in SampleSeeds) for (dark in listOf(false, true)) {
            verify(seedColorScheme(seed, dark), dark, "seed $seed")
        }
        assertNone()
    }
}
