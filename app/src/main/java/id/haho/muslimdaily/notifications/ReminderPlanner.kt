package id.haho.muslimdaily.notifications

import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.domain.fasting.FastingCalendar
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSchedule
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ReminderType(val requestCode: Int) {
    ADHAN_FAJR(1), ADHAN_DHUHR(2), ADHAN_ASR(3), ADHAN_MAGHRIB(4), ADHAN_ISHA(5),
    MORNING_ADHKAR(10), EVENING_ADHKAR(11), BEFORE_SLEEP(12), FRIDAY(13), REVIEW(14),

    /** Minutes before each prayer: time to head to the mosque. */
    PRE_PRAYER(20),

    /** The evening before a sunnah fast. */
    FASTING(22),

    /** Not a notification: redraws widgets and the lock-screen countdown when the next prayer changes. */
    REFRESH(21),
    ;

    val prayer: Prayer?
        get() = when (this) {
            ADHAN_FAJR -> Prayer.FAJR
            ADHAN_DHUHR -> Prayer.DHUHR
            ADHAN_ASR -> Prayer.ASR
            ADHAN_MAGHRIB -> Prayer.MAGHRIB
            ADHAN_ISHA -> Prayer.ISHA
            else -> null
        }

    companion object {
        fun adhanFor(prayer: Prayer): ReminderType = when (prayer) {
            Prayer.FAJR -> ADHAN_FAJR
            Prayer.DHUHR, Prayer.JUMUAH -> ADHAN_DHUHR
            Prayer.ASR -> ADHAN_ASR
            Prayer.MAGHRIB -> ADHAN_MAGHRIB
            Prayer.ISHA -> ADHAN_ISHA
        }
    }
}

data class PlannedReminder(val type: ReminderType, val at: Instant, val silent: Boolean = false)

/**
 * Next firing time for every enabled reminder. Everything is off by default; prayer-relative
 * reminders follow the current location and calculation settings.
 */
object ReminderPlanner {

    fun plan(now: Instant, settings: AppSettings, calculator: PrayerCalculator): List<PlannedReminder> {
        val r = settings.reminders
        val location = settings.location
        val zone = location?.timeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        val schedules: List<PrayerSchedule> = location?.let { loc ->
            (0L..7L).map { calculator.schedule(today.plusDays(it), loc.latitude, loc.longitude, zone, settings.calculation) }
        }.orEmpty()

        fun firstAfter(pick: (PrayerSchedule) -> Instant): Instant? = schedules.map(pick).firstOrNull { it.isAfter(now) }

        val result = mutableListOf<PlannedReminder>()
        if (schedules.isNotEmpty()) {
            FIVE_PRAYERS.forEach { prayer ->
                val mode = r.adhan[prayer] ?: AdhanMode.OFF
                if (mode != AdhanMode.OFF) {
                    firstAfter { it.time(prayer) }?.let { result += PlannedReminder(ReminderType.adhanFor(prayer), it, mode == AdhanMode.SILENT) }
                }
            }
            if (r.morningAdhkar) {
                firstAfter { it.fajr.plus(Duration.ofMinutes(r.morningOffsetMinutes.toLong())) }
                    ?.let { result += PlannedReminder(ReminderType.MORNING_ADHKAR, it) }
            }
            if (r.eveningAdhkar) {
                firstAfter { it.asr.plus(Duration.ofMinutes(r.eveningOffsetMinutes.toLong())) }
                    ?.let { result += PlannedReminder(ReminderType.EVENING_ADHKAR, it) }
            }
            if (r.mosqueBefore) {
                val lead = Duration.ofMinutes(r.mosqueBeforeMinutes.toLong())
                schedules.flatMap { s -> FIVE_PRAYERS.map { s.time(it).minus(lead) } }
                    .filter { it.isAfter(now) }
                    .minOrNull()
                    ?.let { result += PlannedReminder(ReminderType.PRE_PRAYER, it) }
            }
            schedules.flatMap { s -> PrayerTimeSlot.entries.map { s.time(it) } + s.date.plusDays(1).atStartOfDay(zone).toInstant() }
                .filter { it.isAfter(now) }
                .minOrNull()
                ?.let { result += PlannedReminder(ReminderType.REFRESH, it) }
        }
        if (r.fasting) {
            // The first evening (at the chosen time) whose next day is a sunnah fast.
            val first = nextDaily(now, zone, r.fastingTime)
            generateSequence(first) { it.plusSeconds(86_400) }.take(60).firstOrNull { evening ->
                val tomorrow = evening.atZone(zone).toLocalDate().plusDays(1)
                FastingCalendar.day(tomorrow, settings.hijriAdjustment).sunnah(r.fastingWeekly).isNotEmpty()
            }?.let { result += PlannedReminder(ReminderType.FASTING, it) }
        }
        if (r.beforeSleep) result += PlannedReminder(ReminderType.BEFORE_SLEEP, nextDaily(now, zone, r.beforeSleepTime))
        if (r.reviewQueue) result += PlannedReminder(ReminderType.REVIEW, nextDaily(now, zone, r.reviewTime))
        if (r.friday) result += PlannedReminder(ReminderType.FRIDAY, nextFriday(now, zone, r.fridayTime))
        return result
    }

    fun nextDaily(now: Instant, zone: ZoneId, time: LocalTime): Instant {
        val today = now.atZone(zone).toLocalDate().atTime(time).atZone(zone).toInstant()
        return if (today.isAfter(now)) today else now.atZone(zone).toLocalDate().plusDays(1).atTime(time).atZone(zone).toInstant()
    }

    fun nextFriday(now: Instant, zone: ZoneId, time: LocalTime): Instant {
        val date = now.atZone(zone).toLocalDate()
        val thisFriday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).atTime(time).atZone(zone).toInstant()
        return if (thisFriday.isAfter(now)) {
            thisFriday
        } else {
            date.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).atTime(time).atZone(zone).toInstant()
        }
    }
}
