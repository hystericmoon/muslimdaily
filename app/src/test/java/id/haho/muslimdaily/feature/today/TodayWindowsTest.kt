package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSchedule
import id.haho.muslimdaily.domain.prayer.WindowType
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Today sections that appear only while their time window is in progress (Jakarta, Wed 21 Oct 2026). */
class TodayWindowsTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val settings = TodayPreviewData.settings

    private fun data(at: LocalDateTime) =
        (TodayPreviewData.ready(at = at, checked = emptySet(), progress = emptyMap()) as TodayUiState.Ready).data

    private fun schedule(date: LocalDate): PrayerSchedule {
        val l = settings.location!!
        return PrayerCalculator().schedule(date, l.latitude, l.longitude, zone, settings.calculation, l.elevation)
    }

    private val day = LocalDate.of(2026, 10, 21)
    private val today = schedule(day)
    private fun at(instant: java.time.Instant, plusMinutes: Long = 0) =
        LocalDateTime.ofInstant(instant.plus(Duration.ofMinutes(plusMinutes)), zone)

    @Test
    fun `before Subuh only last night's witr runs`() {
        val d = data(at(today.fajr, -30))
        assertNull(d.adhkar)
        assertNull(d.duha)
        assertTrue(d.prohibited.isEmpty())
        val witr = assertNotNull(d.witr)
        assertEquals(today.fajr, witr.end)
    }

    @Test
    fun `after Subuh the morning adhkar and the after-Subuh prohibition show`() {
        val d = data(at(today.fajr, 10))
        val adhkar = assertNotNull(d.adhkar)
        assertEquals(TodayStateBuilder.MORNING, adhkar.eventId)
        assertEquals(today.sunrise, adhkar.end)
        assertEquals(listOf(WindowType.PROHIBITED_AFTER_FAJR), d.prohibited.map { it.type })
        assertNull(d.duha)
        assertNull(d.witr)
    }

    @Test
    fun `the morning adhkar ends at sunrise while the prohibition runs until the sun has risen`() {
        val d = data(at(today.sunrise, 5))
        assertNull(d.adhkar)
        assertEquals(listOf(WindowType.PROHIBITED_AFTER_FAJR), d.prohibited.map { it.type })
        assertNull(d.duha)
    }

    @Test
    fun `mid-morning only Dhuha shows`() {
        val d = data(at(today.sunrise, 120))
        val duha = assertNotNull(d.duha)
        assertEquals(today.sunrise.plus(Duration.ofMinutes(settings.calculation.sunriseRiseMinutes.toLong())), duha.start)
        assertNull(d.adhkar)
        assertNull(d.witr)
        assertTrue(d.prohibited.isEmpty())
    }

    @Test
    fun `at zawal Dhuha has ended and the zawal prohibition shows`() {
        val d = data(at(today.dhuhr, -3))
        assertNull(d.duha)
        assertEquals(listOf(WindowType.PROHIBITED_ZAWAL), d.prohibited.map { it.type })
    }

    @Test
    fun `after Dhuhr nothing timed shows`() {
        val d = data(at(today.dhuhr, 30))
        assertNull(d.adhkar)
        assertNull(d.duha)
        assertNull(d.witr)
        assertTrue(d.prohibited.isEmpty())
    }

    @Test
    fun `after Asr the evening adhkar and the after-Asr prohibition show`() {
        val d = data(at(today.asr, 15))
        val adhkar = assertNotNull(d.adhkar)
        assertEquals(TodayStateBuilder.EVENING, adhkar.eventId)
        assertEquals(today.maghrib, adhkar.end)
        assertEquals(listOf(WindowType.PROHIBITED_AFTER_ASR), d.prohibited.map { it.type })
    }

    @Test
    fun `between Maghrib and Isya nothing timed shows`() {
        val d = data(at(today.maghrib, 10))
        assertNull(d.adhkar)
        assertNull(d.witr)
        assertTrue(d.prohibited.isEmpty())
    }

    @Test
    fun `after Isya witr runs until tomorrow's Subuh`() {
        val d = data(at(today.isha, 30))
        val witr = assertNotNull(d.witr)
        assertEquals(today.isha, witr.start)
        assertEquals(schedule(day.plusDays(1)).fajr, witr.end)
        assertNull(d.adhkar)
    }

    @Test
    fun `adhkar progress belongs to the running window`() {
        val now = at(today.asr, 15)
        val d = (TodayPreviewData.ready(at = now, progress = mapOf(TodayStateBuilder.EVENING to RunProgress(3, 10, false))) as TodayUiState.Ready).data
        assertEquals(RunProgress(3, 10, false), d.adhkar?.progress)
    }

    @Test
    fun `Isya ends are a third and half of the night from Maghrib, and the next Subuh`() {
        val d = data(at(today.dhuhr, 30))
        val nextFajr = schedule(day.plusDays(1)).fajr
        val night = Duration.between(today.maghrib, nextFajr)
        assertEquals(today.maghrib.plus(night.dividedBy(3)), d.ishaEnds.thirdOfNight)
        assertEquals(today.maghrib.plus(night.dividedBy(2)), d.ishaEnds.midnight)
        assertEquals(nextFajr, d.ishaEnds.fajr)
        assertTrue(d.ishaEnds.thirdOfNight.isAfter(today.isha))
    }

    @Test
    fun `the next prayer's row is marked so it can show the time left`() {
        val d = data(at(today.dhuhr, 30))
        assertEquals(listOf(PrayerTimeSlot.ASR), d.rows.filter { it.isNext }.map { it.slot })
        assertEquals(Duration.between(d.now, today.asr), d.next.remaining)
    }

    @Test
    fun `after Isya no row is next because the next Subuh is tomorrow`() {
        val d = data(at(today.isha, 30))
        assertTrue(d.rows.none { it.isNext })
        assertTrue(d.next.isTomorrow)
    }
}
