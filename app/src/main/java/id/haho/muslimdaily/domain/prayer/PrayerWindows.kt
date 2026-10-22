package id.haho.muslimdaily.domain.prayer

import java.time.Duration
import java.time.Instant

enum class WindowType {
    MORNING_ADHKAR,
    EVENING_ADHKAR,
    /** After praying Fajr until the sun has fully risen. */
    PROHIBITED_AFTER_FAJR,
    /** Short window at zawal, just before Dhuhr. */
    PROHIBITED_ZAWAL,
    /** After praying Asr until Maghrib. */
    PROHIBITED_AFTER_ASR,
    DUHA,
    WITR,
    ;

    val isProhibited: Boolean get() = this == PROHIBITED_AFTER_FAJR || this == PROHIBITED_ZAWAL || this == PROHIBITED_AFTER_ASR
}

data class TimeWindow(val type: WindowType, val start: Instant, val end: Instant) {
    operator fun contains(t: Instant): Boolean = !t.isBefore(start) && t.isBefore(end)
}

/**
 * Derives the day's windows from prayer times:
 *  - Morning adhkar: Fajr → sunrise. Evening adhkar: Asr → Maghrib.
 *  - Prohibited: Fajr → sunrise + rise minutes; Dhuhr − zawal minutes → Dhuhr; Asr → Maghrib.
 *  - Duha: end of the sunrise prohibition → start of the zawal window.
 *  - Witr: Isha → next day's Fajr.
 * The after-Fajr and after-Asr prohibitions begin once that prayer has been prayed; the UI says so
 * in words because the app cannot know when the user prayed.
 */
object PrayerWindows {

    fun forDay(
        today: PrayerSchedule,
        tomorrowFajr: Instant,
        sunriseRiseMinutes: Int,
        zawalMinutes: Int,
    ): List<TimeWindow> {
        val risen = today.sunrise.plus(Duration.ofMinutes(sunriseRiseMinutes.toLong()))
        val zawalStart = today.dhuhr.minus(Duration.ofMinutes(zawalMinutes.toLong()))
        return listOf(
            TimeWindow(WindowType.MORNING_ADHKAR, today.fajr, today.sunrise),
            TimeWindow(WindowType.PROHIBITED_AFTER_FAJR, today.fajr, risen),
            TimeWindow(WindowType.DUHA, risen, zawalStart),
            TimeWindow(WindowType.PROHIBITED_ZAWAL, zawalStart, today.dhuhr),
            TimeWindow(WindowType.EVENING_ADHKAR, today.asr, today.maghrib),
            TimeWindow(WindowType.PROHIBITED_AFTER_ASR, today.asr, today.maghrib),
            TimeWindow(WindowType.WITR, today.isha, tomorrowFajr),
        ).filter { it.end.isAfter(it.start) }
    }

    /**
     * Windows active at [now], most relevant first. Pass yesterday's windows too so that the witr
     * window running from last night's Isha to this morning's Fajr is found after midnight.
     */
    fun active(now: Instant, windows: List<TimeWindow>): List<TimeWindow> =
        windows.filter { now in it }.sortedBy { PRIORITY.indexOf(it.type) }

    private val PRIORITY = listOf(
        WindowType.PROHIBITED_ZAWAL,
        WindowType.MORNING_ADHKAR,
        WindowType.EVENING_ADHKAR,
        WindowType.PROHIBITED_AFTER_FAJR,
        WindowType.PROHIBITED_AFTER_ASR,
        WindowType.DUHA,
        WindowType.WITR,
    )
}
