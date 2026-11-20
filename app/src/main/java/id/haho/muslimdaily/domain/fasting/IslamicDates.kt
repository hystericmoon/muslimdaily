package id.haho.muslimdaily.domain.fasting

import id.haho.muslimdaily.domain.prayer.HijriCalendar
import id.haho.muslimdaily.domain.prayer.HijriDate
import java.time.LocalDate

/**
 * The Islamic dates the app names, by Hijri month and day. Names and dates only: no ruling and no
 * virtue is attached here. Calculated like the fasting calendar, so the same Hijri adjustment
 * applies, and Indonesia's isbat can move a date by a day (the screen says so).
 */
enum class IslamicOccasion(val month: Int, val day: Int) {
    NEW_YEAR(1, 1),
    ASHURA(1, 10),
    RAMADAN(9, 1),
    EID_FITR(10, 1),
    ARAFAH(12, 9),
    EID_ADHA(12, 10),
}

data class IslamicDay(val occasion: IslamicOccasion, val date: LocalDate, val hijri: HijriDate)

object IslamicDates {
    /** The occasion that falls on [date], if any. */
    fun on(date: LocalDate, hijriAdjustment: Int): IslamicDay? {
        val h = HijriCalendar.of(date, hijriAdjustment)
        val occasion = IslamicOccasion.entries.firstOrNull { it.month == h.month && it.day == h.day } ?: return null
        return IslamicDay(occasion, date, h)
    }

    /** The next [limit] occasions from [from] (inclusive), soonest first. */
    fun upcoming(from: LocalDate, hijriAdjustment: Int, limit: Int, horizonDays: Long = 400): List<IslamicDay> =
        generateSequence(from) { it.plusDays(1) }
            .take(horizonDays.toInt())
            .mapNotNull { on(it, hijriAdjustment) }
            .take(limit)
            .toList()
}
