package id.haho.design

import androidx.compose.material3.ColorScheme
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.seedColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/** The opaque pairings checked by tools/color/generate_scheme.mjs, for every scheme the library can make. */
class PaletteContrastTest {

    private fun verify(s: ColorScheme, label: String) {
        val checks = listOf(
            Triple("onSurface/surface", ratio(s.onSurface, s.surface), 4.5),
            Triple("onSurfaceVariant/surface", ratio(s.onSurfaceVariant, s.surface), 4.5),
            Triple("onSurfaceVariant/surfaceContainerHighest", ratio(s.onSurfaceVariant, s.surfaceContainerHighest), 4.5),
            Triple("onSurface/surfaceContainerLow", ratio(s.onSurface, s.surfaceContainerLow), 4.5),
            Triple("primary/surface", ratio(s.primary, s.surface), 4.5),
            Triple("onPrimary/primary", ratio(s.onPrimary, s.primary), 4.5),
            Triple("onPrimaryContainer/primaryContainer", ratio(s.onPrimaryContainer, s.primaryContainer), 4.5),
            Triple("onSecondaryContainer/secondaryContainer", ratio(s.onSecondaryContainer, s.secondaryContainer), 4.5),
            Triple("onTertiaryContainer/tertiaryContainer", ratio(s.onTertiaryContainer, s.tertiaryContainer), 4.5),
            Triple("onErrorContainer/errorContainer", ratio(s.onErrorContainer, s.errorContainer), 4.5),
            Triple("outline/surface", ratio(s.outline, s.surface), 3.0),
        )
        for ((name, r, min) in checks) assertTrue("$label $name: %.2f:1 < $min".format(r), r >= min)
    }

    @Test fun palettesMeetAa() = HahoPalette.entries.forEach { p ->
        for (dark in listOf(false, true)) for (grey in listOf(false, true)) {
            verify(colorSchemeFor(p, dark, grey), "${p.name} dark=$dark grey=$grey")
        }
    }

    @Test fun customSeedsMeetAa() = SampleSeeds.forEach { seed ->
        for (dark in listOf(false, true)) for (grey in listOf(false, true)) {
            verify(seedColorScheme(seed, dark, grey), "seed $seed dark=$dark grey=$grey")
        }
    }
}
