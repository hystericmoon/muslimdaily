package id.haho.muslimdaily.data.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

/** A compass reading: heading from true north and how much to trust it. */
data class Heading(val degrees: Double, val reliable: Boolean)

/**
 * Device heading from the rotation-vector sensor (gyro-stabilised where the phone has one),
 * corrected from magnetic to true north for the given place.
 */
@Singleton
class CompassRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val sensors = context.getSystemService(SensorManager::class.java)

    private val sensor: Sensor?
        get() = sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    fun available(): Boolean = sensor != null

    fun headings(latitude: Double, longitude: Double): Flow<Heading> = callbackFlow {
        val s = sensor ?: run { close(); return@callbackFlow }
        val declination = GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val magnetic = Math.toDegrees(orientation[0].toDouble())
                val trueNorth = (magnetic + declination + 360.0) % 360.0
                trySend(Heading(trueNorth, accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
            }

            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                accuracy = value
            }
        }
        sensors!!.registerListener(listener, s, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }.conflate()
}
