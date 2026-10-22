package id.haho.muslimdaily.domain.prayer

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Direction of the Ka'bah, as an initial great-circle bearing from true north. */
object Qibla {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    /** Degrees clockwise from true north, 0 until 360. */
    fun bearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LON - longitude)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Signed turn from [heading] to [target], in -180 until 180 (positive = turn right). */
    fun turn(heading: Double, target: Double): Double = ((target - heading + 540.0) % 360.0) - 180.0
}
