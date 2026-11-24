package id.haho.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.haho.design.glass.glassColors
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.Motion
import id.haho.design.theme.colorSchemeFor
import id.haho.design.theme.hahoTypography
import id.haho.design.theme.scaled
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TokensTest {

    @Test fun enterStaggerCapsAtEightSiblings() {
        assertEquals(0, Motion.enterDelayMillis(0))
        assertEquals(40, Motion.enterDelayMillis(1))
        assertEquals(320, Motion.enterDelayMillis(8))
        assertEquals(320, Motion.enterDelayMillis(50))
        assertEquals(0, Motion.enterDelayMillis(-3))
    }

    @Test fun scaledTypographyScalesSizeAndLineHeight() {
        val base = hahoTypography(FontFamily.SansSerif)
        val big = base.scaled(1.5f)
        assertEquals(base.bodyLarge.fontSize * 1.5f, big.bodyLarge.fontSize)
        assertEquals(base.bodyLarge.lineHeight * 1.5f, big.bodyLarge.lineHeight)
        assertEquals(base.titleLarge.fontWeight, big.titleLarge.fontWeight)
        assertSame(base, base.scaled(1f))
    }

    @Test fun typeScaleKeepsM3SizesWithSemiBoldTitles() {
        val m3 = Typography()
        val t = hahoTypography(FontFamily.SansSerif)
        assertEquals(m3.bodyLarge.fontSize, t.bodyLarge.fontSize)
        assertEquals(16.sp, t.bodyLarge.fontSize)
        assertEquals(FontWeight.SemiBold, t.titleMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, t.labelLarge.fontWeight)
    }

    @Test fun glassOffIsFullyOpaque() {
        for (p in HahoPalette.entries) for (dark in listOf(false, true)) {
            val g = glassColors(colorSchemeFor(p, dark), dark, enabled = false)
            for (c in listOf(g.base, g.card, g.chrome, g.sheet, g.tinted, g.alert, g.primary, g.tonal)) {
                assertEquals("${p.name} dark=$dark", 1f, c.alpha)
            }
            assertTrue(g.glows.isEmpty())
            assertEquals(0f, g.sheen.alpha)
            assertEquals(0f, g.rimTop.alpha)
        }
    }

    @Test fun glassOnHasThreeGlowsAndATranslucentCard() {
        val g = glassColors(colorSchemeFor(HahoPalette.Default, false), dark = false, enabled = true)
        assertEquals(3, g.glows.size)
        assertTrue(g.card.alpha < 1f)
        assertEquals(HahoPalette.TEAL, HahoPalette.Default)
    }
}
