package id.haho.muslimdaily.domain.prayer

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val day: Int, val month: Int, val year: Int)

/**
 * Hijri date from the Umm al-Qura calendar (java.time HijrahDate), shifted by the user's
 * adjustment (−1, 0, +1 days) to match local sighting.
 */
object HijriCalendar {
    fun of(date: LocalDate, adjustmentDays: Int): HijriDate {
        val h = HijrahDate.from(date.plusDays(adjustmentDays.coerceIn(-1, 1).toLong()))
        return HijriDate(
            day = h.get(ChronoField.DAY_OF_MONTH),
            month = h.get(ChronoField.MONTH_OF_YEAR),
            year = h.get(ChronoField.YEAR),
        )
    }
}
