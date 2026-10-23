package id.haho.muslimdaily.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One-shot, coarse location. Coordinates are cached in settings; the app never tracks location in
 * the background. If the device appears to have moved, the Today screen *suggests* updating the
 * location and enabling travel mode; nothing changes automatically.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val cities: CityRepository,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPrecisePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Where the device is right now, for the nearby-mosque list. Not saved anywhere. */
    data class Fix(val latitude: Double, val longitude: Double, val accuracyMeters: Float?, val precise: Boolean)

    /**
     * One current fix: high accuracy when precise location is granted, otherwise approximate.
     * Null without permission or when no fix arrives in time.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentFix(): Fix? {
        if (!hasPermission()) return null
        val precise = hasPrecisePermission()
        val source = CancellationTokenSource()
        val priority = if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            client.getCurrentLocation(priority, source.token).awaitOrNull()
        } ?: run {
            source.cancel()
            // A cached fix may be from somewhere else on a trip; only trust a recent one.
            client.lastLocation.awaitOrNull()?.takeIf {
                SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < MAX_CACHED_AGE_NANOS
            }
        } ?: return null
        return Fix(fix.latitude, fix.longitude, fix.accuracy.takeIf { fix.hasAccuracy() }, precise)
    }

    sealed interface FixResult {
        data class Success(val location: SavedLocation) : FixResult
        data object NoPermission : FixResult
        data object Unavailable : FixResult
    }

    /** Takes one fix, names it after the nearest city and saves it. */
    @SuppressLint("MissingPermission")
    suspend fun updateFromGps(): FixResult {
        if (!hasPermission()) return FixResult.NoPermission
        val source = CancellationTokenSource()
        val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, source.token).awaitOrNull()
        } ?: run {
            source.cancel()
            client.lastLocation.awaitOrNull()
        } ?: return FixResult.Unavailable
        // Naming the fix is a nicety; fall back to coordinates if the city list is unavailable.
        val city = runCatching { cities.nearest(fix.latitude, fix.longitude) }.getOrNull()
        val saved = SavedLocation(
            name = city?.name ?: "%.3f, %.3f".format(fix.latitude, fix.longitude),
            latitude = fix.latitude,
            longitude = fix.longitude,
            timeZoneId = ZoneId.systemDefault().id,
            source = LocationSource.GPS,
            updatedAtMillis = System.currentTimeMillis(),
            // The town's terrain height is steadier than a phone's GPS altitude (which is also
            // above the ellipsoid, not sea level); the fix's altitude is the fallback.
            elevation = city?.takeIf { distanceKm(fix.latitude, fix.longitude, it.latitude, it.longitude) < 15 }?.elevation
                ?: fix.altitude.takeIf { fix.hasAltitude() }?.toInt()?.coerceAtLeast(0) ?: 0,
        )
        settings.update { setLocation(saved) }
        return FixResult.Success(saved)
    }

    /**
     * Locations saved before 0.5.0 have no elevation. Takes it from the nearest town once, so a
     * highland user's Maghrib moves to match Kemenag without picking the city again.
     */
    suspend fun backfillElevation() {
        val s = settings.current()
        val loc = s.location ?: return
        if (loc.elevation != 0 || s.elevationChecked) return
        val city = cities.nearest(loc.latitude, loc.longitude)
            ?.takeIf { distanceKm(loc.latitude, loc.longitude, it.latitude, it.longitude) < 15 }
        settings.update {
            elevationChecked = true
            if (city != null && city.elevation > 0) setLocation(loc.copy(elevation = city.elevation))
        }
    }

    suspend fun saveCity(city: City) {
        settings.update {
            setLocation(
                SavedLocation(
                    name = city.name,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timeZoneId = city.timeZone,
                    source = LocationSource.MANUAL,
                    updatedAtMillis = System.currentTimeMillis(),
                    elevation = city.elevation,
                ),
            )
        }
    }

    /**
     * Distance in km between the saved location and the device's last known position, if the
     * permission is granted and a cached fix exists. Never requests a fresh fix.
     */
    @SuppressLint("MissingPermission")
    suspend fun distanceFromSavedKm(saved: SavedLocation): Double? {
        if (!hasPermission()) return null
        val last = client.lastLocation.awaitOrNull() ?: return null
        return distanceKm(saved.latitude, saved.longitude, last.latitude, last.longitude)
    }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task -> cont.resume(if (task.isSuccessful) task.result else null) }
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 15_000L
        const val MAX_CACHED_AGE_NANOS = 3 * 60 * 1_000_000_000L
    }
}
