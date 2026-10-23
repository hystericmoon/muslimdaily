package id.haho.muslimdaily.widget

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.pow
import kotlin.test.assertTrue

/**
 * The widget puts white text anywhere on the sky, so every painted pixel must give white text
 * at least 4.5:1 (WCAG AA), at every moment of the day. Checks the real bitmaps, every 10 minutes
 * over a day, in a wide and a tall shape. Stars are left out: they are sub-pixel texture.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyContrastTest {

    private fun luminance(c: Int): Double {
        fun ch(v: Int) = (v / 255.0).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * ch(Color.red(c)) + 0.7152 * ch(Color.green(c)) + 0.0722 * ch(Color.blue(c))
    }

    @Test
    fun `white text passes AA on the sky all day`() {
        val l = TodayPreviewData.settings.location!!
        val zone = ZoneId.of(l.timeZoneId)
        val day = PrayerCalculator().schedule(LocalDate.of(2026, 10, 23), l.latitude, l.longitude, zone, TodayPreviewData.settings.calculation, l.elevation)
        val start = LocalDate.of(2026, 10, 23).atStartOfDay(zone).toInstant()
        var worst = Double.MAX_VALUE
        var worstAt = ""
        for ((w, h) in listOf(170 to 85, 85 to 170)) {
            var t = start
            while (t.isBefore(start.plus(Duration.ofDays(1)))) {
                val bmp = SkyPainter.paint(w, h, 1f, Sky.at(t, day), cornerPx = 0f, stars = false)
                // Skip the 1 px rim along the edge; it is decoration.
                for (y in 2 until h - 2 step 2) for (x in 2 until w - 2 step 2) {
                    val ratio = 1.05 / (luminance(bmp.getPixel(x, y)) + 0.05)
                    if (ratio < worst) {
                        worst = ratio
                        worstAt = "$t ($w×$h at $x,$y)"
                    }
                }
                t = t.plus(Duration.ofMinutes(10))
            }
        }
        assertTrue(worst >= 4.5, "white text on the sky is %.2f:1 at %s".format(worst, worstAt))
    }
}
