package id.haho.muslimdaily.domain.fasting

import id.haho.muslimdaily.domain.prayer.HijriCalendar
import id.haho.muslimdaily.domain.prayer.HijriDate
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What a day is for fasting. Each kind carries its evidence in the UI (FastingStrings); only
 * days with a sahih or hasan basis are listed. A forbidden day overrides everything else.
 */
enum class FastKind(val forbidden: Boolean = false, val obligatory: Boolean = false, val weekly: Boolean = false) {
    RAMADAN(obligatory = true),
    ARAFAH,
    TASUA,
    ASHURA,
    WHITE_DAYS,
    SHAWWAL,
    MONDAY(weekly = true),
    THURSDAY(weekly = true),
    EID_FITR(forbidden = true),
    EID_ADHA(forbidden = true),
    TASHRIQ(forbidden = true),
}

data class FastDay(val date: LocalDate, val hijri: HijriDate, val kinds: List<FastKind>) {
    val forbidden: Boolean get() = kinds.any { it.forbidden }
    val ramadan: Boolean get() = FastKind.RAMADAN in kinds

    /** Sunnah fasts only (not Ramadan, not forbidden). */
    fun sunnah(includeWeekly: Boolean = true): List<FastKind> =
        kinds.filter { !it.forbidden && !it.obligatory && (includeWeekly || !it.weekly) }
}

object FastingCalendar {

    fun day(date: LocalDate, hijriAdjustment: Int): FastDay {
        val h = HijriCalendar.of(date, hijriAdjustment)
        return FastDay(date, h, kinds(date, h))
    }

    internal fun kinds(date: LocalDate, h: HijriDate): List<FastKind> {
        val forbidden = when {
            h.month == 10 && h.day == 1 -> FastKind.EID_FITR
            h.month == 12 && h.day == 10 -> FastKind.EID_ADHA
            h.month == 12 && h.day in 11..13 -> FastKind.TASHRIQ
            else -> null
        }
        if (forbidden != null) return listOf(forbidden)
        if (h.month == 9) return listOf(FastKind.RAMADAN)
        return buildList {
            if (h.month == 12 && h.day == 9) add(FastKind.ARAFAH)
            if (h.month == 1 && h.day == 9) add(FastKind.TASUA)
            if (h.month == 1 && h.day == 10) add(FastKind.ASHURA)
            if (h.day in 13..15) add(FastKind.WHITE_DAYS)
            if (h.month == 10 && h.day >= 2) add(FastKind.SHAWWAL)
            if (date.dayOfWeek == DayOfWeek.MONDAY) add(FastKind.MONDAY)
            if (date.dayOfWeek == DayOfWeek.THURSDAY) add(FastKind.THURSDAY)
        }
    }

    /** Every Gregorian day of the Hijri month that [anyDay] falls in, in order. */
    fun month(anyDay: LocalDate, hijriAdjustment: Int): List<FastDay> {
        var start = anyDay
        while (HijriCalendar.of(start, hijriAdjustment).day != 1) start = start.minusDays(1)
        val month = HijriCalendar.of(start, hijriAdjustment).month
        return generateSequence(start) { it.plusDays(1) }
            .map { day(it, hijriAdjustment) }
            .takeWhile { it.hijri.month == month }
            .toList()
    }

    /** The next days from [from] (inclusive) that are a sunnah fast or a forbidden day. */
    fun upcoming(from: LocalDate, hijriAdjustment: Int, includeWeekly: Boolean, limit: Int, horizonDays: Long = 400): List<FastDay> =
        generateSequence(from) { it.plusDays(1) }
            .take(horizonDays.toInt())
            .map { day(it, hijriAdjustment) }
            .filter { it.forbidden || it.sunnah(includeWeekly).isNotEmpty() }
            .take(limit)
            .toList()
}
