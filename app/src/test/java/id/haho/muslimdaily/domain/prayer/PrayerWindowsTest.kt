package id.haho.muslimdaily.domain.prayer

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrayerWindowsTest {
    private val day = LocalDate.of(2026, 10, 21)
    private fun at(h: Int, m: Int, d: LocalDate = day): Instant = d.atTime(h, m).toInstant(ZoneOffset.UTC)

    private val today = PrayerSchedule(
        date = day, zone = ZoneOffset.UTC,
        fajr = at(4, 10), sunrise = at(5, 30), dhuhr = at(11, 50),
        asr = at(15, 10), maghrib = at(17, 55), isha = at(19, 5),
    )
    private val yesterday = today.copy(
        date = day.minusDays(1),
        fajr = at(4, 11, day.minusDays(1)), sunrise = at(5, 31, day.minusDays(1)), dhuhr = at(11, 50, day.minusDays(1)),
        asr = at(15, 10, day.minusDays(1)), maghrib = at(17, 55, day.minusDays(1)), isha = at(19, 6, day.minusDays(1)),
    )
    private val tomorrowFajr = at(4, 9, day.plusDays(1))
    private val windows = PrayerWindows.forDay(today, tomorrowFajr, sunriseRiseMinutes = 15, zawalMinutes = 10) +
        PrayerWindows.forDay(yesterday, today.fajr, 15, 10)

    private fun activeAt(h: Int, m: Int) = PrayerWindows.active(at(h, m), windows).map { it.type }

    @Test
    fun `morning adhkar runs from fajr to sunrise`() {
        assertTrue(WindowType.MORNING_ADHKAR in activeAt(4, 10))
        assertTrue(WindowType.MORNING_ADHKAR in activeAt(5, 29))
        assertFalse(WindowType.MORNING_ADHKAR in activeAt(5, 30))
        assertFalse(WindowType.MORNING_ADHKAR in activeAt(4, 9))
    }

    @Test
    fun `evening adhkar runs from asr to maghrib`() {
        assertTrue(WindowType.EVENING_ADHKAR in activeAt(15, 10))
        assertTrue(WindowType.EVENING_ADHKAR in activeAt(17, 54))
        assertFalse(WindowType.EVENING_ADHKAR in activeAt(17, 55))
    }

    @Test
    fun `prohibited after fajr lasts until the sun has risen`() {
        assertTrue(WindowType.PROHIBITED_AFTER_FAJR in activeAt(5, 40))
        assertFalse(WindowType.PROHIBITED_AFTER_FAJR in activeAt(5, 45))
    }

    @Test
    fun `zawal window sits just before dhuhr`() {
        assertEquals(WindowType.PROHIBITED_ZAWAL, activeAt(11, 45).first())
        assertFalse(WindowType.PROHIBITED_ZAWAL in activeAt(11, 39))
        assertFalse(WindowType.PROHIBITED_ZAWAL in activeAt(11, 50))
    }

    @Test
    fun `prohibited after asr lasts until maghrib`() {
        assertTrue(WindowType.PROHIBITED_AFTER_ASR in activeAt(17, 0))
        assertFalse(WindowType.PROHIBITED_AFTER_ASR in activeAt(17, 55))
    }

    @Test
    fun `duha runs between the sunrise prohibition and zawal`() {
        assertFalse(WindowType.DUHA in activeAt(5, 44))
        assertTrue(WindowType.DUHA in activeAt(5, 45))
        assertTrue(WindowType.DUHA in activeAt(11, 39))
        assertFalse(WindowType.DUHA in activeAt(11, 40))
    }

    @Test
    fun `witr runs from isha until the next fajr, across midnight`() {
        assertTrue(WindowType.WITR in activeAt(19, 5))
        assertTrue(WindowType.WITR in activeAt(23, 59))
        assertTrue(WindowType.WITR in activeAt(2, 0)) // yesterday's window
        assertFalse(WindowType.WITR in activeAt(4, 10))
        assertFalse(WindowType.WITR in activeAt(19, 4))
    }

    @Test
    fun `morning adhkar takes priority over the prohibition label`() {
        assertEquals(listOf(WindowType.MORNING_ADHKAR, WindowType.PROHIBITED_AFTER_FAJR), activeAt(4, 30))
    }

    @Test
    fun `no window at midday outside zawal`() {
        assertEquals(emptyList(), activeAt(13, 0))
    }
}
