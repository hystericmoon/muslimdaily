package id.haho.muslimdaily.domain.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.batoulapps.adhan2.model.Rounding
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AsrMethod
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.data.settings.CalculationSettings
import id.haho.muslimdaily.data.settings.PrayerOffsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One day's prayer times at a location. There is deliberately no Imsak. */
data class PrayerSchedule(
    val date: LocalDate,
    val zone: ZoneId,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
) {
    fun time(slot: PrayerTimeSlot): Instant = when (slot) {
        PrayerTimeSlot.FAJR -> fajr
        PrayerTimeSlot.SUNRISE -> sunrise
        PrayerTimeSlot.DHUHR -> dhuhr
        PrayerTimeSlot.ASR -> asr
        PrayerTimeSlot.MAGHRIB -> maghrib
        PrayerTimeSlot.ISHA -> isha
    }

    fun time(prayer: Prayer): Instant = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR, Prayer.JUMUAH -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

/**
 * Calculates prayer times with adhan-kotlin, then applies the method's precautionary minutes
 * (ihtiyat), the user's per-prayer offsets and the method's rounding.
 *
 * Kemenag RI is not an adhan preset: Fajr 20°, Isha 18°, ihtiyat +2 minutes on Fajr, Asr and
 * Isha, +3 on Dhuhr and Maghrib (the extra minute stands in for the semi-diameter and horizon
 * corrections Kemenag applies), −3 on sunrise; rounded up to the minute (sunrise down).
 * Kemenag also lowers the horizon for the observer's height (dip = 1.76′·√h), which makes
 * sunrise earlier and Maghrib later by about 3–4 minutes in a highland city like Bandung; the
 * preset applies the same correction when the location has an elevation.
 * Calibrated against the official schedule: within one minute for 16 cities in all three time
 * zones across the year (see KemenagReferenceTest).
 */
class PrayerCalculator @Inject constructor() {
    private companion object {
        const val DIP_BASELINE_M = 100
    }

    fun schedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        settings: CalculationSettings,
        /** Metres above sea level; only the Kemenag preset uses it. */
        elevation: Int = 0,
    ): PrayerSchedule {
        val coordinates = Coordinates(latitude, longitude)
        val spec = specFor(settings)
        val params = spec.base.copy(
            fajrAngle = spec.fajrAngle ?: spec.base.fajrAngle,
            ishaAngle = spec.ishaAngle ?: spec.base.ishaAngle,
            madhab = if (settings.asr == AsrMethod.HANAFI) Madhab.HANAFI else Madhab.SHAFI,
            highLatitudeRule = HighLatitudeRule.recommendedFor(coordinates),
            rounding = Rounding.NONE,
        )
        val times = PrayerTimes(coordinates, DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
        val user = settings.offsets
        val ih = spec.ihtiyat
        val dip = if (spec.horizonDip) dipSeconds(date, latitude, times, elevation) else 0L
        fun adjust(t: kotlinx.datetime.Instant, ihtiyat: Int, offset: Int, roundDown: Boolean = false, dipSign: Int = 0): Instant {
            val shifted = Instant.ofEpochMilli(t.toEpochMilliseconds())
                .plus((ihtiyat + offset).toLong(), ChronoUnit.MINUTES)
                .plusSeconds(dipSign * dip)
            return spec.rounding.apply(shifted, roundDown)
        }
        return PrayerSchedule(
            date = date,
            zone = zone,
            fajr = adjust(times.fajr, ih.fajr, user.fajr),
            sunrise = adjust(times.sunrise, ih.sunrise, user.sunrise, roundDown = true, dipSign = -1),
            dhuhr = adjust(times.dhuhr, ih.dhuhr, user.dhuhr),
            asr = adjust(times.asr, ih.asr, user.asr),
            maghrib = adjust(times.maghrib, ih.maghrib, user.maghrib, dipSign = 1),
            isha = adjust(times.isha, ih.isha, user.isha),
        )
    }

    private fun specFor(settings: CalculationSettings): MethodSpec = when (settings.method) {
        CalcMethod.KEMENAG -> MethodSpec(
            base = CalculationMethod.OTHER.parameters,
            fajrAngle = 20.0,
            ishaAngle = 18.0,
            ihtiyat = PrayerOffsets(fajr = 2, sunrise = -3, dhuhr = 3, asr = 2, maghrib = 3, isha = 2),
            rounding = MinuteRounding.UP,
            horizonDip = true,
        )
        CalcMethod.MWL -> MethodSpec(base = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters)
        CalcMethod.UMM_AL_QURA -> MethodSpec(base = CalculationMethod.UMM_AL_QURA.parameters)
        CalcMethod.CUSTOM -> MethodSpec(
            base = CalculationMethod.OTHER.parameters,
            fajrAngle = settings.customFajrAngle,
            ishaAngle = settings.customIshaAngle,
        )
    }

    private data class MethodSpec(
        val base: CalculationParameters,
        val fajrAngle: Double? = null,
        val ishaAngle: Double? = null,
        val ihtiyat: PrayerOffsets = PrayerOffsets(),
        val rounding: MinuteRounding = MinuteRounding.NEAREST,
        val horizonDip: Boolean = false,
    )

    /**
     * How much earlier the sun rises (and later it sets) for an observer [elevation] metres up:
     * the extra depression of the horizon divided by the sun's vertical speed at the horizon.
     */
    private fun dipSeconds(date: LocalDate, latitude: Double, times: PrayerTimes, elevation: Int): Long {
        // Kemenag doesn't publish the heights it uses per city. Its tables show the correction in
        // highland cities (Bandung, ~700 m) but not in low ones (Yogyakarta, ~110 m), and the
        // sea-level ihtiyat already absorbs the first metres; so only the height above
        // DIP_BASELINE_M counts. Calibrated in KemenagReferenceTest.
        val above = elevation - DIP_BASELINE_M
        if (above <= 0) return 0
        val dipDeg = 1.76 / 60.0 * sqrt(above.toDouble())
        // Hour angle at sunset, from the computed times (4 minutes of time per degree).
        val halfDayMinutes = (times.maghrib.toEpochMilliseconds() - times.dhuhr.toEpochMilliseconds()) / 60_000.0
        val hourAngle = Math.toRadians(halfDayMinutes / 4.0)
        val declination = Math.toRadians(23.44 * sin(Math.toRadians(360.0 / 365.0 * (date.dayOfYear - 81))))
        val rate = cos(Math.toRadians(latitude)) * cos(declination) * sin(hourAngle) // degrees of altitude per degree of hour angle
        if (rate < 0.05) return 0
        return (dipDeg / rate * 4 * 60).toLong()
    }

    private enum class MinuteRounding {
        NEAREST, UP;

        fun apply(t: Instant, roundDown: Boolean): Instant {
            val floor = t.truncatedTo(ChronoUnit.MINUTES)
            if (floor == t) return t
            return when {
                roundDown && this == UP -> floor
                this == UP -> floor.plus(1, ChronoUnit.MINUTES)
                t.epochSecond % 60 >= 30 -> floor.plus(1, ChronoUnit.MINUTES)
                else -> floor
            }
        }
    }
}
