package id.haho.muslimdaily.widget

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.domain.fasting.FastKind
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSchedule
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetModelTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val settings = TodayPreviewData.settings
    private val calculator = PrayerCalculator()

    /** Friday 23 October 2026, 12 Jumadilawal 1448: tomorrow is the first of the white days. */
    private val friday = LocalDate.of(2026, 10, 23)

    private fun day(d: LocalDate = friday): PrayerSchedule {
        val l = settings.location!!
        return calculator.schedule(d, l.latitude, l.longitude, zone, settings.calculation, l.elevation)
    }

    private fun at(h: Int, m: Int, d: LocalDate = friday): Instant = LocalDateTime.of(d, java.time.LocalTime.of(h, m)).atZone(zone).toInstant()

    private fun model(now: Instant, s: AppSettings = settings) = WidgetModels.at(now, s, calculator)!!

    @Test
    fun `no location means no model`() {
        assertNull(WidgetModels.at(at(9, 0), settings.copy(location = null), calculator))
    }

    @Test
    fun `the now line follows the day and never repeats the next prayer's time`() {
        val d = day()
        fun line(t: Instant) = model(t).now
        assertEquals(NowLine(NowKind.WITR, null), line(d.fajr.minusSeconds(600)))
        assertEquals(NowLine(NowKind.FAJR_ENDS, d.sunrise), line(d.fajr.plusSeconds(600)))
        assertEquals(NowLine(NowKind.PROHIBITED, d.sunrise.plus(Duration.ofMinutes(15))), line(d.sunrise.plusSeconds(300)))
        assertEquals(NowLine(NowKind.DUHA, d.dhuhr.minus(Duration.ofMinutes(10))), line(at(8, 30)))
        assertEquals(NowLine(NowKind.PROHIBITED, null), line(d.dhuhr.minusSeconds(300)))
        assertEquals(NowLine(NowKind.DHUHR, null), line(d.dhuhr.plusSeconds(600)))
        assertEquals(NowLine(NowKind.EVENING_ADHKAR, null), line(d.asr.plusSeconds(600)))
        assertEquals(NowLine(NowKind.MAGHRIB, null), line(d.maghrib.plusSeconds(600)))
        assertEquals(NowLine(NowKind.WITR, null), line(d.isha.plusSeconds(600)))
    }

    @Test
    fun `on Friday morning the next prayer is Jumuah`() {
        val m = model(at(8, 30))
        assertEquals(Prayer.JUMUAH, m.next.prayer)
        assertEquals(PrayerTimeSlot.DHUHR, m.nextSlot)
        assertTrue(m.friday)
        assertFalse(m.isTomorrow)
        assertEquals(5, m.prayers.size)
        assertEquals(6, m.slots.size)
    }

    @Test
    fun `once Isya begins the widget lists tomorrow with Fajr next`() {
        val d = day()
        val m = model(d.isha.plusSeconds(60))
        val tomorrow = day(friday.plusDays(1))
        assertTrue(m.isTomorrow)
        assertEquals(friday.plusDays(1), m.day)
        assertEquals(PrayerTimeSlot.FAJR, m.nextSlot)
        assertEquals(tomorrow.fajr, m.next.time)
        assertEquals(tomorrow.dhuhr, m.slots.first { it.slot == PrayerTimeSlot.DHUHR }.time)
        assertFalse(m.friday, "Saturday's Dhuhr is not Jumu'ah")
        // Just before Isya, today is still listed.
        assertFalse(model(d.isha.minusSeconds(60)).isTomorrow)
    }

    @Test
    fun `a sunnah fast tomorrow is named, and weekly fasts follow the setting`() {
        assertEquals(FastKind.WHITE_DAYS, model(at(9, 0)).fastTomorrow)
        val wednesday = LocalDate.of(2026, 10, 28)
        assertEquals(FastKind.THURSDAY, model(at(9, 0, wednesday)).fastTomorrow)
        val noWeekly = settings.copy(reminders = settings.reminders.copy(fastingWeekly = false))
        assertNull(model(at(9, 0, wednesday), noWeekly).fastTomorrow)
    }

    @Test
    fun `the sun shows from sunrise to Maghrib, the moon otherwise`() {
        val d = day()
        assertFalse(model(d.fajr).daytime)
        assertTrue(model(d.sunrise).daytime)
        assertTrue(model(d.maghrib.minusSeconds(60)).daytime)
        assertFalse(model(d.maghrib).daytime)
    }

    @Test
    fun `the moon's phase follows the synodic month`() {
        // Full moon 26 October 2026 04:12 UTC, new moon 9 November 2026 07:02 UTC.
        assertEquals(0.5f, Moon.phase(Instant.parse("2026-10-26T04:12:00Z")), 0.03f)
        val newMoon = Moon.phase(Instant.parse("2026-11-09T07:02:00Z"))
        assertTrue(newMoon < 0.03f || newMoon > 0.97f, "new moon was $newMoon")
    }

    @Test
    fun `the widget's size picks its layout`() {
        // Android's reference sizes for a 5-column grid, portrait (cells: 73n − 16 by 118n − 16 dp).
        assertEquals(1 to 1, WidgetGrid.of(57f, 102f).let { it.columns to it.rows })
        assertEquals(2 to 1, WidgetGrid.of(130f, 102f).let { it.columns to it.rows })
        assertEquals(3 to 2, WidgetGrid.of(203f, 220f).let { it.columns to it.rows })
        assertEquals(3 to 2, WidgetGrid.of(276f, 220f).let { it.columns to it.rows })
        assertEquals(4 to 3, WidgetGrid.of(349f, 337f).let { it.columns to it.rows })
        // The design sizes.
        assertEquals(4 to 2, WidgetGrid.of(340f, 164f).let { it.columns to it.rows })
        assertEquals(5 to 3, WidgetGrid.of(428f, 252f).let { it.columns to it.rows })
        // A landscape row is short: drop the secondary line.
        assertTrue(WidgetGrid.of(340f, 51f).tight)
        assertFalse(WidgetGrid.of(340f, 76f).tight)
    }

    @Test
    fun `large system text counts as a smaller widget, and big widgets get bigger type`() {
        val big = WidgetGrid.of(340f, 164f, fontScale = 2f)
        assertEquals(2 to 1, big.columns to big.rows)
        assertEquals(1f, WidgetGrid.of(340f, 164f).scale)
        assertEquals(1.35f, WidgetGrid.of(800f, 600f).scale)
    }

    @Test
    fun `the bar runs through the day's prayers, an equal length between each two`() {
        val d = day()
        // Halfway from Dhuhr to Asr: one gap of four done, plus half of the next.
        val half = d.dhuhr.plus(Duration.between(d.dhuhr, d.asr).dividedBy(2))
        val track = model(half).track
        assertEquals(listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).size, track.stops.size)
        assertEquals(1.5f / 4, track.progress, 0.001f)
        assertEquals(0f, model(d.fajr).track.progress, 0.001f)
    }

    @Test
    fun `at night the bar runs from Isya to Subuh, before and after midnight`() {
        val after = model(at(21, 0)).track
        assertEquals(listOf(PrayerTimeSlot.ISHA, PrayerTimeSlot.FAJR), after.stops.map { it.slot })
        assertEquals(day().isha, after.stops.first().time)
        assertEquals(day(friday.plusDays(1)).fajr, after.stops.last().time)
        val before = model(at(2, 0)).track
        assertEquals(day(friday.minusDays(1)).isha, before.stops.first().time)
        assertEquals(day().fajr, before.stops.last().time)
        assertTrue(before.progress > after.progress)
    }

    @Test
    fun `12-hour times keep their digits and shrink only the marker`() {
        assertEquals(listOf("11:40" to 16f, " AM" to 16f * 0.7f), clockParts("11:40 AM", 16f).toList())
        assertEquals(listOf("11.40" to 16f), clockParts("11.40", 16f).toList())
    }
}
