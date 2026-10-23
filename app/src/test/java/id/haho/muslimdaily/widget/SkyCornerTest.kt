package id.haho.muslimdaily.widget

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Before Android 12 launchers draw widgets edge to edge, so the sky bitmap itself must have
 * round corners: transparent outside the rounded rectangle, opaque inside.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyCornerTest {

    @Test
    fun `the sky is cut to round corners`() {
        val density = 2f
        val corner = 24 * density
        val sky = SkyPainter.paint(680, 328, density, Sky.NIGHT, corner)
        for ((x, y) in listOf(0 to 0, sky.width - 1 to 0, 0 to sky.height - 1, sky.width - 1 to sky.height - 1)) {
            assertEquals(0, Color.alpha(sky.getPixel(x, y)), "corner ($x, $y) should be transparent")
        }
        assertEquals(255, Color.alpha(sky.getPixel(sky.width / 2, sky.height / 2)))
        // Just inside the curve, on the diagonal, is sky again.
        val inside = (corner * 0.4f).toInt()
        assertTrue(Color.alpha(sky.getPixel(inside, inside)) > 200)
    }
}
