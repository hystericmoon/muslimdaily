package id.haho.muslimdaily.widget

import android.content.Context
import android.text.format.DateFormat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Hands the next week of prayer times to a paired Wear OS watch (the 'wear' module's tile).
 * The watch never calculates; it shows what the phone computed with the user's own method,
 * offsets and location. Best effort: without Google Play services or a watch, nothing happens.
 */
@Singleton
class WearSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calculator: PrayerCalculator,
) {
    suspend fun push(settings: AppSettings, now: Instant) {
        val location = settings.location ?: return
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val today = now.atZone(zone).toLocalDate()
        val times = (0L until DAYS).flatMap { d ->
            val s = calculator.schedule(today.plusDays(d), location.latitude, location.longitude, zone, settings.calculation)
            PrayerTimeSlot.entries.map { s.time(it).toEpochMilli() }
        }
        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putString("place", location.name)
            dataMap.putString("zone", zone.id)
            dataMap.putLong("firstDay", today.toEpochDay())
            dataMap.putLongArray("times", times.toLongArray())
            dataMap.putBoolean("is24", DateFormat.is24HourFormat(context))
            dataMap.putLong("updated", now.toEpochMilli())
        }.asPutDataRequest().setUrgent()
        runCatching {
            withTimeoutOrNull(5_000) {
                suspendCancellableCoroutine { cont ->
                    Wearable.getDataClient(context).putDataItem(request).addOnCompleteListener { cont.resume(Unit) }
                }
            }
        }
    }

    companion object {
        const val PATH = "/prayer"
        const val DAYS = 7L
    }
}
