package id.haho.muslimdaily.domain.prayer

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrayerSnapshotTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val calculator = PrayerCalculator()
    private fun at(d: Int, h: Int, m: Int) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant()

    @Test
    fun `next prayer at noon is Asr, with today's six times`() {
        val s = PrayerSnapshots.at(at(21, 12, 0), TodayPreviewData.settings, calculator)!!
        assertEquals(Prayer.ASR, s.next.prayer)
        assertEquals(PrayerTimeSlot.ASR, s.nextSlot)
        assertEquals(PrayerTimeSlot.entries, s.today.map { it.slot })
    }

    @Test
    fun `Dhuhr on Friday is Jumuah`() {
        val s = PrayerSnapshots.at(at(23, 9, 0), TodayPreviewData.settings, calculator)!!
        assertEquals(Prayer.JUMUAH, s.next.prayer)
    }

    @Test
    fun `after Isha the next prayer is tomorrow's Fajr, not one of today's slots`() {
        val s = PrayerSnapshots.at(at(21, 23, 0), TodayPreviewData.settings, calculator)!!
        assertEquals(Prayer.FAJR, s.next.prayer)
        assertEquals(22, s.next.time.atZone(zone).dayOfMonth)
        assertNull(s.nextSlot)
    }

    @Test
    fun `refresh after Isha is midnight`() {
        val now = at(21, 23, 0)
        val s = PrayerSnapshots.at(now, TodayPreviewData.settings, calculator)!!
        assertEquals(LocalDateTime.of(2026, 10, 22, 0, 0), PrayerSnapshots.nextRefresh(now, s).atZone(zone).toLocalDateTime())
    }

    @Test
    fun `no snapshot without a location`() {
        assertNull(PrayerSnapshots.at(at(21, 12, 0), TodayPreviewData.settings.copy(location = null), calculator))
    }
}
