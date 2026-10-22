package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.domain.prayer.HijriCalendar
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RamadanCardTest {
    private fun data(at: LocalDateTime) = (TodayPreviewData.ready(at = at) as TodayUiState.Ready).data

    @Test
    fun `no card outside Ramadan`() = assertNull(data(LocalDateTime.of(2026, 10, 21, 12, 0)).ramadan)

    @Test
    fun `during a Ramadan day the card counts to iftar`() {
        val d = data(LocalDateTime.of(2027, 2, 21, 15, 0))
        val r = assertNotNull(d.ramadan)
        assertEquals(HijriCalendar.of(LocalDate.of(2027, 2, 21), 0).day, r.day)
        assertTrue(!r.suhurNow(d.now))
        assertTrue(r.iftar.isAfter(d.now))
    }

    @Test
    fun `after Maghrib the card is about tomorrow's fast`() {
        val d = data(LocalDateTime.of(2027, 2, 21, 20, 0))
        val r = assertNotNull(d.ramadan)
        assertEquals(HijriCalendar.of(LocalDate.of(2027, 2, 22), 0).day, r.day)
        assertTrue(r.suhurNow(d.now))
    }
}
