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
 * Calibrated against the official schedule: within one minute for five cities across the year
 * (see KemenagReferenceTest).
 */
class PrayerCalculator @Inject constructor() {

    fun schedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        settings: CalculationSettings,
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
        fun adjust(t: kotlinx.datetime.Instant, ihtiyat: Int, offset: Int, roundDown: Boolean = false): Instant {
            val shifted = Instant.ofEpochMilli(t.toEpochMilliseconds()).plus((ihtiyat + offset).toLong(), ChronoUnit.MINUTES)
            return spec.rounding.apply(shifted, roundDown)
        }
        return PrayerSchedule(
            date = date,
            zone = zone,
            fajr = adjust(times.fajr, ih.fajr, user.fajr),
            sunrise = adjust(times.sunrise, ih.sunrise, user.sunrise, roundDown = true),
            dhuhr = adjust(times.dhuhr, ih.dhuhr, user.dhuhr),
            asr = adjust(times.asr, ih.asr, user.asr),
            maghrib = adjust(times.maghrib, ih.maghrib, user.maghrib),
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
    )

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
