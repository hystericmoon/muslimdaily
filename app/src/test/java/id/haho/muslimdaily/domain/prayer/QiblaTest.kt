package id.haho.muslimdaily.domain.prayer

import org.junit.Test
import kotlin.test.assertEquals

class QiblaTest {
    @Test
    fun `Jakarta faces about 295 degrees`() = assertEquals(295.15, Qibla.bearing(-6.2088, 106.8456), 0.2)

    @Test
    fun `Makassar and Medan are close to Jakarta's direction`() {
        assertEquals(292.5, Qibla.bearing(-5.1477, 119.4327), 1.0)
        assertEquals(293.0, Qibla.bearing(3.5952, 98.6722), 1.0)
    }

    @Test
    fun `turn takes the short way round`() {
        assertEquals(10.0, Qibla.turn(350.0, 0.0), 1e-9)
        assertEquals(-20.0, Qibla.turn(10.0, 350.0), 1e-9)
        assertEquals(0.0, Qibla.turn(295.0, 295.0), 1e-9)
    }
}
