package id.haho.muslimdaily.domain.prayer

import id.haho.muslimdaily.data.settings.AsrMethod
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.data.settings.CalculationSettings
import id.haho.muslimdaily.data.settings.PrayerOffsets
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerCalculatorTest {
    private val calculator = PrayerCalculator()
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val date = LocalDate.of(2026, 10, 21)

    private fun jakarta(settings: CalculationSettings = CalculationSettings()) =
        calculator.schedule(date, -6.2146, 106.8451, jakarta, settings)

    private fun local(i: java.time.Instant): LocalTime = i.atZone(jakarta).toLocalTime()

    @Test
    fun `times are in order and within plausible ranges for Jakarta`() {
        val s = jakarta()
        val times = listOf(s.fajr, s.sunrise, s.dhuhr, s.asr, s.maghrib, s.isha)
        assertEquals(times.sorted(), times)
        assertTrue(local(s.fajr) in LocalTime.of(3, 50)..LocalTime.of(4, 30), "fajr ${local(s.fajr)}")
        assertTrue(local(s.dhuhr) in LocalTime.of(11, 30)..LocalTime.of(12, 10), "dhuhr ${local(s.dhuhr)}")
        assertTrue(local(s.maghrib) in LocalTime.of(17, 40)..LocalTime.of(18, 20), "maghrib ${local(s.maghrib)}")
    }

    @Test
    fun `kemenag times are whole minutes`() {
        val s = jakarta()
        listOf(s.fajr, s.sunrise, s.dhuhr, s.asr, s.maghrib, s.isha).forEach {
            assertEquals(0, it.epochSecond % 60, "not rounded: $it")
        }
    }

    @Test
    fun `kemenag differs from MWL by its angles and ihtiyat`() {
        val kemenag = jakarta()
        val mwl = jakarta(CalculationSettings(method = CalcMethod.MWL))
        // Fajr at 20 degrees is earlier than at 18 degrees, even after +2 minutes ihtiyat.
        assertTrue(kemenag.fajr.isBefore(mwl.fajr))
        // Maghrib: same astronomy, Kemenag adds 3 minutes and rounds up.
        val diff = Duration.between(mwl.maghrib, kemenag.maghrib).toMinutes()
        assertTrue(diff in 2..4, "maghrib diff $diff")
    }

    @Test
    fun `user offsets shift each prayer`() {
        val base = jakarta()
        val shifted = jakarta(CalculationSettings(offsets = PrayerOffsets(fajr = 3, isha = -2)))
        assertEquals(3, Duration.between(base.fajr, shifted.fajr).toMinutes())
        assertEquals(-2, Duration.between(base.isha, shifted.isha).toMinutes())
        assertEquals(base.dhuhr, shifted.dhuhr)
    }

    @Test
    fun `hanafi asr is later than standard`() {
        val standard = jakarta()
        val hanafi = jakarta(CalculationSettings(asr = AsrMethod.HANAFI))
        assertTrue(hanafi.asr.isAfter(standard.asr))
        assertEquals(standard.dhuhr, hanafi.dhuhr)
    }

    @Test
    fun `custom angles are used`() {
        val a = jakarta(CalculationSettings(method = CalcMethod.CUSTOM, customFajrAngle = 20.0, customIshaAngle = 18.0))
        val b = jakarta(CalculationSettings(method = CalcMethod.CUSTOM, customFajrAngle = 15.0, customIshaAngle = 15.0))
        assertTrue(a.fajr.isBefore(b.fajr))
        assertTrue(a.isha.isAfter(b.isha))
    }
}
