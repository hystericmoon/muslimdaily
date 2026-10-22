package id.haho.muslimdaily.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume

/** The week of prayer times the phone app sent (see WearSync on the phone). */
class PrayerData(
    val place: String,
    val zone: ZoneId,
    val firstDay: LocalDate,
    /** Six times per day (Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya), day after day. */
    val times: List<Instant>,
    val is24: Boolean,
) {
    data class Slot(val index: Int, val time: Instant, val day: LocalDate) {
        /** 0 Subuh, 1 Terbit, 2 Dzuhur, 3 Ashar, 4 Maghrib, 5 Isya. */
        val slot: Int get() = index % 6
        val isPrayer: Boolean get() = slot != 1
        val isFridayDhuhr: Boolean get() = slot == 2 && day.dayOfWeek == DayOfWeek.FRIDAY
    }

    val slots: List<Slot> = times.mapIndexed { i, t -> Slot(i, t, firstDay.plusDays((i / 6).toLong())) }

    /** The prayers (not sunrise) from [now] on, in order. */
    fun upcoming(now: Instant): List<Slot> = slots.filter { it.isPrayer && it.time.isAfter(now) }

    companion object {
        suspend fun load(context: Context): PrayerData? {
            val buffer: DataItemBuffer = suspendCancellableCoroutine<DataItemBuffer?> { cont ->
                Wearable.getDataClient(context)
                    .getDataItems(Uri.parse("wear://*/prayer"))
                    .addOnCompleteListener { cont.resume(if (it.isSuccessful) it.result else null) }
            } ?: return null
            return try {
                buffer.firstOrNull()?.let { item: com.google.android.gms.wearable.DataItem ->
                    val map = DataMapItem.fromDataItem(item).dataMap
                    PrayerData(
                        place = map.getString("place") ?: "",
                        zone = runCatching { ZoneId.of(map.getString("zone")) }.getOrElse { ZoneId.systemDefault() },
                        firstDay = LocalDate.ofEpochDay(map.getLong("firstDay")),
                        times = map.getLongArray("times")?.map(Instant::ofEpochMilli).orEmpty(),
                        is24 = map.getBoolean("is24", true),
                    )
                }
            } finally {
                buffer.release()
            }
        }
    }
}
