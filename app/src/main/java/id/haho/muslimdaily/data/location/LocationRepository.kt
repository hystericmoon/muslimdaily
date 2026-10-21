package id.haho.muslimdaily.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
        val city = cities.nearest(fix.latitude, fix.longitude)
        val saved = SavedLocation(
            name = city?.name ?: "%.3f, %.3f".format(fix.latitude, fix.longitude),
            latitude = fix.latitude,
            longitude = fix.longitude,
            timeZoneId = ZoneId.systemDefault().id,
            source = LocationSource.GPS,
            updatedAtMillis = System.currentTimeMillis(),
        )
        settings.update { setLocation(saved) }
        return FixResult.Success(saved)
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
    }
}
