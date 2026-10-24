package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.AsrMethod
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.domain.prayer.HijriCalendar
import id.haho.muslimdaily.domain.prayer.HijriDate
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSchedule
import id.haho.muslimdaily.domain.prayer.PrayerWindows
import id.haho.muslimdaily.domain.prayer.TimeWindow
import id.haho.muslimdaily.domain.prayer.WindowType
import id.haho.muslimdaily.domain.rawatib.PrayerPlan
import id.haho.muslimdaily.domain.rawatib.RawatibPlanner
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data object NoLocation : TodayUiState
    data class Ready(val data: TodayData) : TodayUiState
}

data class TodayData(
    val date: LocalDate,
    val hijri: HijriDate,
    val zone: ZoneId,
    val now: Instant,
    val locationName: String,
    val travelMode: Boolean,
    val method: CalcMethod,
    val asr: AsrMethod,
    val next: NextPrayer,
    /** The five prayers; sunrise is not a row of its own but Subuh's end, [sunrise]. */
    val rows: List<PrayerRow>,
    val sunrise: Instant,
    /** Today's Isya ends, shown inside its row. */
    val ishaEnds: IshaEnds,
    /** Witr's window, checked inside the Isya row: last night's until Subuh, then tonight's. */
    val witr: TimeWindow?,
    // The sections below are set only while their window is in progress.
    val duha: TimeWindow?,
    val prohibited: List<TimeWindow>,
    val adhkar: AdhkarCard?,
    val checked: Set<String>,
    val travelSuggestionKm: Int?,
    /** Set during Ramadan, and on its eve after Maghrib. */
    val ramadan: RamadanCard? = null,
    /** Asks, around the government's month-start announcement, which day the new month begins. */
    val hijriCheck: HijriCheck? = null,
)

/**
 * Indonesia starts Ramadan, Syawal and Zulhijah on the sidang isbat announcement, made on the
 * evening of the 29th, so the calculated calendar can be a day off. From Maghrib on the 29th
 * until the end of the following day the app asks once which way it went.
 *
 * [month] is the month ending; [calculatedNew] is whether the calculation already starts the
 * next month on the day after the 29th.
 */
data class HijriCheck(
    val key: String,
    val month: Int,
    val year: Int,
    val calculatedNew: Boolean,
    /** Asked on the evening of the 29th itself (true) or on the following day. */
    val onEve: Boolean,
) {
    val nextMonth: Int get() = month % 12 + 1

    /** The change to the Hijri adjustment for an answer ("the new month starts the day after the 29th"). */
    fun delta(startsNext: Boolean): Int = when {
        startsNext && !calculatedNew -> 1
        !startsNext && calculatedNew -> -1
        else -> 0
    }
}

/**
 * The fasting day in Ramadan: today until Maghrib, then tomorrow. Suhur ends at Subuh (the app
 * shows no imsak); the fast is broken at Maghrib.
 */
data class RamadanCard(val day: Int, val suhurEnds: Instant, val iftar: Instant) {
    fun suhurNow(now: Instant): Boolean = now.isBefore(suhurEnds)
}

data class NextPrayer(
    val prayer: Prayer,
    val time: Instant,
    val iqamah: Instant?,
    val remaining: Duration,
    val isTomorrow: Boolean,
)

data class PrayerRow(
    val slot: PrayerTimeSlot,
    val plan: PrayerPlan?,
    val time: Instant,
    val iqamah: Instant?,
    val isCurrent: Boolean,
    val isNext: Boolean,
)

/**
 * The ends of Isya the screen lists side by side, without ranking them: a third and the middle of
 * the night, both measured from Maghrib to the next Subuh, and that Subuh.
 */
data class IshaEnds(val thirdOfNight: Instant, val midnight: Instant, val fajr: Instant) {
    companion object {
        fun of(maghrib: Instant, nextFajr: Instant): IshaEnds {
            val night = Duration.between(maghrib, nextFajr)
            return IshaEnds(maghrib.plus(night.dividedBy(3)), maghrib.plus(night.dividedBy(2)), nextFajr)
        }
    }
}

data class RunProgress(val done: Int, val total: Int, val completed: Boolean)

/** The morning or evening adhkar whose window is in progress. */
data class AdhkarCard(
    val eventId: String,
    val end: Instant,
    val progress: RunProgress?,
)

/** Pure construction of the Today screen state, so it can be unit-tested with fixed inputs. */
object TodayStateBuilder {

    const val MORNING = "morning"
    const val EVENING = "evening"

    fun build(
        now: Instant,
        settings: AppSettings,
        rawatib: List<Rawatib>,
        fard: List<Fard>,
        checked: Set<String>,
        calculator: PrayerCalculator,
        runProgress: Map<String, RunProgress> = emptyMap(),
        travelSuggestionKm: Int? = null,
    ): TodayUiState {
        val location = settings.location ?: return TodayUiState.NoLocation
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val date = now.atZone(zone).toLocalDate()
        val calc = settings.calculation
        fun day(d: LocalDate) = calculator.schedule(d, location.latitude, location.longitude, zone, calc, location.elevation)
        val yesterday = day(date.minusDays(1))
        val today = day(date)
        val tomorrow = day(date.plusDays(1))

        val windows = PrayerWindows.forDay(today, tomorrow.fajr, calc.sunriseRiseMinutes, calc.zawalMinutes)
        val yesterdayWindows = PrayerWindows.forDay(yesterday, today.fajr, calc.sunriseRiseMinutes, calc.zawalMinutes)
        val active = PrayerWindows.active(now, windows + yesterdayWindows)

        val isFriday = date.dayOfWeek == DayOfWeek.FRIDAY
        fun plan(p: Prayer) = RawatibPlanner.plan(p, isFriday, settings.travelMode, settings.dhuhrQabliyahRakah, rawatib, fard)
        fun iqamah(p: Prayer, time: Instant): Instant? =
            settings.iqamahOffsets[p]?.takeIf { it > 0 }?.let { time.plus(Duration.ofMinutes(it.toLong())) }

        val next = nextPrayer(now, today, tomorrow, isFriday)
        val current = currentSlot(now, today)
        val rows = PrayerTimeSlot.entries.filter { it != PrayerTimeSlot.SUNRISE }.map { slot ->
            val prayer = slot.toPrayer()
            val p = prayer?.let { plan(it) }
            val time = today.time(slot)
            PrayerRow(
                slot = slot,
                plan = p,
                time = time,
                iqamah = p?.let { iqamah(it.prayer, time) },
                isCurrent = slot == current,
                isNext = !next.isTomorrow && prayer != null && today.time(prayer) == next.time,
            )
        }

        return TodayUiState.Ready(
            TodayData(
                date = date,
                hijri = HijriCalendar.of(date, settings.hijriAdjustment),
                zone = zone,
                now = now,
                locationName = location.name,
                travelMode = settings.travelMode,
                method = calc.method,
                asr = calc.asr,
                next = next.copy(iqamah = iqamah(next.prayer, next.time)),
                rows = rows,
                sunrise = today.sunrise,
                ishaEnds = IshaEnds.of(today.maghrib, tomorrow.fajr),
                // After midnight this is still last night's window, which runs to this morning's Subuh.
                witr = active.firstOrNull { it.type == WindowType.WITR } ?: windows.firstOrNull { it.type == WindowType.WITR },
                duha = active.firstOrNull { it.type == WindowType.DUHA },
                prohibited = active.filter { it.type.isProhibited }.sortedBy { it.start },
                adhkar = adhkarCard(active, runProgress),
                checked = checked,
                travelSuggestionKm = travelSuggestionKm,
                ramadan = ramadanCard(now, date, today, tomorrow, settings.hijriAdjustment),
                hijriCheck = hijriCheck(now, date, today, settings),
            ),
        )
    }

    /** The months whose start Indonesia sets by isbat: Ramadan, Syawal and Zulhijah. */
    private val ISBAT_EVES = setOf(8, 9, 11)

    private fun hijriCheck(now: Instant, date: LocalDate, today: PrayerSchedule, settings: AppSettings): HijriCheck? {
        val adj = settings.hijriAdjustment
        fun eve(d: LocalDate) = HijriCalendar.of(d, adj).takeIf { it.day == 29 && it.month in ISBAT_EVES }
        val eveDate = when {
            !now.isBefore(today.maghrib) && eve(date) != null -> date
            eve(date.minusDays(1)) != null -> date.minusDays(1)
            else -> return null
        }
        val h = eve(eveDate)!!
        val key = "${h.year}-${h.month}"
        if (settings.hijriConfirmedFor == key) return null
        return HijriCheck(key, h.month, h.year, calculatedNew = HijriCalendar.of(eveDate.plusDays(1), adj).day == 1, onEve = eveDate == date)
    }

    private fun ramadanCard(now: Instant, date: LocalDate, today: PrayerSchedule, tomorrow: PrayerSchedule, hijriAdjustment: Int): RamadanCard? {
        val (day, schedule) = if (now.isBefore(today.maghrib)) date to today else date.plusDays(1) to tomorrow
        val hijri = HijriCalendar.of(day, hijriAdjustment)
        if (hijri.month != 9) return null
        return RamadanCard(hijri.day, schedule.fajr, schedule.maghrib)
    }

    private fun PrayerTimeSlot.toPrayer(): Prayer? = when (this) {
        PrayerTimeSlot.FAJR -> Prayer.FAJR
        PrayerTimeSlot.SUNRISE -> null
        PrayerTimeSlot.DHUHR -> Prayer.DHUHR
        PrayerTimeSlot.ASR -> Prayer.ASR
        PrayerTimeSlot.MAGHRIB -> Prayer.MAGHRIB
        PrayerTimeSlot.ISHA -> Prayer.ISHA
    }

    private fun nextPrayer(now: Instant, today: PrayerSchedule, tomorrow: PrayerSchedule, isFriday: Boolean): NextPrayer {
        val upcoming = FIVE_PRAYERS.firstOrNull { today.time(it).isAfter(now) }
        val (prayer, time, isTomorrow) = if (upcoming != null) {
            Triple(upcoming, today.time(upcoming), false)
        } else {
            Triple(Prayer.FAJR, tomorrow.fajr, true)
        }
        val shown = if (prayer == Prayer.DHUHR && isFriday && !isTomorrow) Prayer.JUMUAH else prayer
        return NextPrayer(shown, time, null, Duration.between(now, time), isTomorrow)
    }

    /** The prayer whose time is running: Fajr until sunrise, then nothing until Dhuhr, and so on. */
    private fun currentSlot(now: Instant, s: PrayerSchedule): PrayerTimeSlot? = when {
        now.isBefore(s.fajr) -> null
        now.isBefore(s.sunrise) -> PrayerTimeSlot.FAJR
        now.isBefore(s.dhuhr) -> null
        now.isBefore(s.asr) -> PrayerTimeSlot.DHUHR
        now.isBefore(s.maghrib) -> PrayerTimeSlot.ASR
        now.isBefore(s.isha) -> PrayerTimeSlot.MAGHRIB
        else -> PrayerTimeSlot.ISHA
    }

    private fun adhkarCard(active: List<TimeWindow>, progress: Map<String, RunProgress>): AdhkarCard? {
        val window = active.firstOrNull { it.type == WindowType.MORNING_ADHKAR || it.type == WindowType.EVENING_ADHKAR } ?: return null
        val id = if (window.type == WindowType.MORNING_ADHKAR) MORNING else EVENING
        return AdhkarCard(id, window.end, progress[id])
    }
}
