package id.haho.muslimdaily.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The glass is translucent, so the wallpaper decides how light it gets. Over the worst case, a
 * pure white wallpaper, muted text (65% white) must still pass AA everywhere on it, sheen
 * included. The 1 px edge is decoration and left out.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassContrastTest {

    private fun luminance(c: Int): Double {
        fun ch(v: Int) = (v / 255.0).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * ch(Color.red(c)) + 0.7152 * ch(Color.green(c)) + 0.0722 * ch(Color.blue(c))
    }

    private fun over(bg: Int, fg: Int): Int {
        val a = Color.alpha(fg) / 255f
        fun ch(b: Int, f: Int) = (f * a + b * (1 - a)).toInt()
        return Color.rgb(ch(Color.red(bg), Color.red(fg)), ch(Color.green(bg), Color.green(fg)), ch(Color.blue(bg), Color.blue(fg)))
    }

    @Test
    fun `muted text passes AA on the glass over a white wallpaper`() {
        var worst = Double.MAX_VALUE
        for ((w, h) in listOf(340 to 164, 164 to 340)) {
            val glass = GlassPainter.glass(w, h, 1f, cornerPx = 0f)
            val shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(shot).apply {
                drawColor(Color.WHITE)
                drawBitmap(glass, 0f, 0f, null)
            }
            for (y in 2 until h - 2 step 2) for (x in 2 until w - 2 step 2) {
                val bg = shot.getPixel(x, y)
                val ratio = (luminance(over(bg, Muted)) + 0.05) / (luminance(bg) + 0.05)
                if (ratio < worst) worst = ratio
            }
        }
        assertTrue(worst >= 4.5, "muted text on the glass over white is %.2f:1".format(worst))
    }

    @Test
    fun `the glass is cut to round corners`() {
        val glass = GlassPainter.glass(680, 328, 2f, cornerPx = 48f)
        assertEquals(0, Color.alpha(glass.getPixel(0, 0)))
        assertTrue(Color.alpha(glass.getPixel(340, 164)) > 150)
    }

    @Test
    fun `the moon is dark where it is unlit`() {
        val size = 200
        val waxing = GlassPainter.moon(ApplicationProvider.getApplicationContext<android.content.Context>().resources, size, 0.25f)
        // First quarter: the right half lit, the left half in shadow.
        val lit = luminance(waxing.getPixel(size / 2 + 25, size / 2))
        val dark = luminance(waxing.getPixel(size / 2 - 25, size / 2))
        assertTrue(lit > dark * 4, "lit $lit, dark $dark")
        val full = GlassPainter.moon(ApplicationProvider.getApplicationContext<android.content.Context>().resources, size, 0.5f)
        assertTrue(luminance(full.getPixel(size / 2 - 25, size / 2)) > dark * 4)
    }
}
