package id.haho.muslimdaily.widget

import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.domain.fasting.FastKind
import id.haho.muslimdaily.domain.fasting.FastingCalendar
import id.haho.muslimdaily.domain.prayer.HijriCalendar
import id.haho.muslimdaily.domain.prayer.HijriDate
import id.haho.muslimdaily.domain.prayer.NextPrayer
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.domain.prayer.SlotTime
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the prayer-times widget shows at one moment. Pure, so it is unit-tested. */
data class WidgetModel(
    val locationName: String,
    val zone: ZoneId,
    /** The day whose times are listed: today, or tomorrow once Isya has begun. */
    val day: LocalDate,
    val isTomorrow: Boolean,
    val hijri: HijriDate,
    /** The listed day's six times, in order. */
    val slots: List<SlotTime>,
    val next: NextPrayer,
    /** The slot in [slots] that [next] refers to. */
    val nextSlot: PrayerTimeSlot,
    /** What matters right now, for the larger sizes. */
    val now: NowLine,
    /** A sunnah fast tomorrow, if any. */
    val fastTomorrow: FastKind?,
    val sky: SkyLook,
) {
    /** On Fridays Dhuhr is listed as Jumu'ah. */
    val friday: Boolean get() = day.dayOfWeek == DayOfWeek.FRIDAY

    /** The five prayers, without sunrise. */
    val prayers: List<SlotTime> get() = slots.filter { it.slot != PrayerTimeSlot.SUNRISE }
}

/** The "now" line. [until] is left out when it is the next prayer's time, which the widget already shows. */
data class NowLine(val kind: NowKind, val until: Instant?)

enum class NowKind { FAJR_ENDS, PROHIBITED, DUHA, DHUHR, EVENING_ADHKAR, MAGHRIB, WITR }

object WidgetModels {

    /** Null when no location is set. */
    fun at(now: Instant, settings: AppSettings, calculator: PrayerCalculator): WidgetModel? {
        val location = settings.location ?: return null
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val date = now.atZone(zone).toLocalDate()
        fun schedule(d: LocalDate) = calculator.schedule(d, location.latitude, location.longitude, zone, settings.calculation, location.elevation)
        val today = schedule(date)
        val tomorrow = schedule(date.plusDays(1))
        val next = PrayerSnapshots.nextPrayer(now, listOf(today, tomorrow), settings)
        // Once Isya has begun, today's list is over: show tomorrow's, with its Fajr next.
        val isTomorrow = !now.isBefore(today.isha)
        val shown = if (isTomorrow) tomorrow else today
        val slots = PrayerTimeSlot.entries.map { SlotTime(it, shown.time(it)) }

        val calc = settings.calculation
        val risen = today.sunrise.plus(Duration.ofMinutes(calc.sunriseRiseMinutes.toLong()))
        val zawal = today.dhuhr.minus(Duration.ofMinutes(calc.zawalMinutes.toLong()))
        val line = when {
            now.isBefore(today.fajr) -> NowLine(NowKind.WITR, null)
            now.isBefore(today.sunrise) -> NowLine(NowKind.FAJR_ENDS, today.sunrise)
            now.isBefore(risen) -> NowLine(NowKind.PROHIBITED, risen)
            now.isBefore(zawal) -> NowLine(NowKind.DUHA, zawal)
            now.isBefore(today.dhuhr) -> NowLine(NowKind.PROHIBITED, null)
            now.isBefore(today.asr) -> NowLine(NowKind.DHUHR, null)
            now.isBefore(today.maghrib) -> NowLine(NowKind.EVENING_ADHKAR, null)
            now.isBefore(today.isha) -> NowLine(NowKind.MAGHRIB, null)
            else -> NowLine(NowKind.WITR, null)
        }

        return WidgetModel(
            locationName = location.name,
            zone = zone,
            day = shown.date,
            isTomorrow = isTomorrow,
            hijri = HijriCalendar.of(shown.date, settings.hijriAdjustment),
            slots = slots,
            next = next,
            nextSlot = slots.first { it.time == next.time }.slot,
            now = line,
            fastTomorrow = FastingCalendar.day(date.plusDays(1), settings.hijriAdjustment)
                .sunnah(settings.reminders.fastingWeekly).firstOrNull(),
            sky = Sky.at(now, today),
        )
    }
}

/**
 * Which layout fits the widget: 1–5 columns of content and 1–3 rows, from its size in dp.
 * Larger system font sizes count as a smaller widget, so text is never squeezed.
 */
data class WidgetGrid(val columns: Int, val rows: Int, val tight: Boolean, val scale: Float) {
    companion object {
        private val BASE_WIDTH = floatArrayOf(76f, 164f, 252f, 340f, 428f)
        private val BASE_HEIGHT = floatArrayOf(76f, 164f, 252f)

        fun of(widthDp: Float, heightDp: Float, fontScale: Float = 1f): WidgetGrid {
            val f = fontScale.coerceAtLeast(1f)
            val w = widthDp / f
            val h = heightDp / f
            val columns = when {
                w < 110f -> 1
                w < 195f -> 2
                w < 290f -> 3
                w < 400f -> 4
                else -> 5
            }
            val rows = when {
                h < 150f -> 1
                // Wide widgets fill a tall 2-row cell (Samsung's grid) with the next prayer large
                // instead of an empty panel around the times.
                h < (if (columns >= 4) 215f else 250f) -> 2
                else -> 3
            }
            // Bigger widgets (tablets, large launchers) get bigger type instead of empty space.
            val scale = minOf(w / BASE_WIDTH[columns - 1], h / BASE_HEIGHT[rows - 1]).coerceIn(1f, 1.35f)
            return WidgetGrid(columns, rows, tight = rows == 1 && h < 70f, scale = scale)
        }
    }
}
