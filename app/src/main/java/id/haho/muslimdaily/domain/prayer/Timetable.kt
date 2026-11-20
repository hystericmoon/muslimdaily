package id.haho.muslimdaily.domain.prayer

import java.time.LocalDate
import java.time.YearMonth

/** One line of a timetable: a day, its Hijri date and its prayer times. */
data class TimetableRow(val schedule: PrayerSchedule, val hijri: HijriDate) {
    val date: LocalDate get() = schedule.date
}

/**
 * The monthly timetable and the Ramadan Imsakiyah: the same per-day schedule Today shows, laid
 * out for a month. Pure and offline; [schedule] is the calculator bound to the saved location,
 * so a table can never disagree with Today. There is deliberately no Imsak column.
 */
object Timetable {
    fun month(month: YearMonth, hijriAdjustment: Int, schedule: (LocalDate) -> PrayerSchedule): List<TimetableRow> =
        rows(month.atDay(1), month.lengthOfMonth(), hijriAdjustment, schedule)

    /**
     * The days of the Ramadan [from] falls in, or of the next one when [from] is outside it, in
     * order (29 or 30 days). Null if none starts within about a year, which cannot happen.
     */
    fun ramadan(from: LocalDate, hijriAdjustment: Int, schedule: (LocalDate) -> PrayerSchedule): List<TimetableRow>? {
        var start = from
        if (HijriCalendar.of(start, hijriAdjustment).month == RAMADAN) {
            while (HijriCalendar.of(start.minusDays(1), hijriAdjustment).month == RAMADAN) start = start.minusDays(1)
        } else {
            var scanned = 0
            while (HijriCalendar.of(start, hijriAdjustment).month != RAMADAN) {
                start = start.plusDays(1)
                if (++scanned > SCAN_DAYS) return null
            }
        }
        val days = generateSequence(start) { it.plusDays(1) }
            .takeWhile { HijriCalendar.of(it, hijriAdjustment).month == RAMADAN }
            .count()
        return rows(start, days, hijriAdjustment, schedule)
    }

    private fun rows(first: LocalDate, count: Int, hijriAdjustment: Int, schedule: (LocalDate) -> PrayerSchedule) =
        List(count) { i ->
            val date = first.plusDays(i.toLong())
            TimetableRow(schedule(date), HijriCalendar.of(date, hijriAdjustment))
        }

    private const val RAMADAN = 9
    private const val SCAN_DAYS = 400
}
