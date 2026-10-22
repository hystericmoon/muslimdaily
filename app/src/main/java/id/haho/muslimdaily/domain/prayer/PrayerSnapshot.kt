package id.haho.muslimdaily.domain.prayer

import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AppSettings
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SlotTime(val slot: PrayerTimeSlot, val time: Instant)

/** The next obligatory prayer. On Fridays Dhuhr is shown as Jumu'ah. */
data class NextPrayer(val prayer: Prayer, val time: Instant, val iqamah: Instant?)

/** What glanceable surfaces (widgets, the lock-screen notification) show at a moment. */
data class PrayerSnapshot(
    val locationName: String,
    val zone: ZoneId,
    val date: LocalDate,
    /** Today's six times, in order. */
    val today: List<SlotTime>,
    val next: NextPrayer,
) {
    /** The slot in [today] that [next] refers to, or null when the next prayer is tomorrow's Fajr. */
    val nextSlot: PrayerTimeSlot?
        get() = today.firstOrNull { it.time == next.time }?.slot
}

object PrayerSnapshots {

    /** Null when no location is set. */
    fun at(now: Instant, settings: AppSettings, calculator: PrayerCalculator): PrayerSnapshot? {
        val location = settings.location ?: return null
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val date = now.atZone(zone).toLocalDate()
        fun schedule(d: LocalDate) = calculator.schedule(d, location.latitude, location.longitude, zone, settings.calculation)
        val today = schedule(date)
        val next = nextPrayer(now, listOf(today, schedule(date.plusDays(1))), settings)
        return PrayerSnapshot(
            locationName = location.name,
            zone = zone,
            date = date,
            today = PrayerTimeSlot.entries.map { SlotTime(it, today.time(it)) },
            next = next,
        )
    }

    /** The first of the five prayers strictly after [now], across the given consecutive days. */
    fun nextPrayer(now: Instant, days: List<PrayerSchedule>, settings: AppSettings): NextPrayer {
        for (day in days) {
            for (prayer in FIVE_PRAYERS) {
                val time = day.time(prayer)
                if (time.isAfter(now)) {
                    val shown = if (prayer == Prayer.DHUHR && day.date.dayOfWeek == DayOfWeek.FRIDAY) Prayer.JUMUAH else prayer
                    val iqamah = settings.iqamahOffsets[prayer]?.takeIf { it > 0 }?.let { time.plus(Duration.ofMinutes(it.toLong())) }
                    return NextPrayer(shown, time, iqamah)
                }
            }
        }
        // Callers always pass today and tomorrow, so Fajr tomorrow is always after now.
        val last = days.last()
        return NextPrayer(Prayer.FAJR, last.fajr, null)
    }

    /**
     * When the glanceable surfaces next need redrawing: the next of today's six times (the
     * highlighted slot moves), or the start of tomorrow.
     */
    fun nextRefresh(now: Instant, snapshot: PrayerSnapshot): Instant =
        snapshot.today.map { it.time }.firstOrNull { it.isAfter(now) }
            ?: snapshot.date.plusDays(1).atStartOfDay(snapshot.zone).toInstant()
}
