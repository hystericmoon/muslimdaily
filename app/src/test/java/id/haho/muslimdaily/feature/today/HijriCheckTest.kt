package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.domain.prayer.HijriCalendar
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HijriCheckTest {
    private fun data(at: LocalDateTime, confirmed: String = "") =
        (TodayPreviewData.ready(at = at, settings = TodayPreviewData.settings.copy(hijriConfirmedFor = confirmed)) as TodayUiState.Ready).data

    /** The calculated 29 Syakban 1448. */
    private val eve: LocalDate = generateSequence(LocalDate.of(2027, 1, 20)) { it.plusDays(1) }
        .first { HijriCalendar.of(it, 0).let { h -> h.month == 8 && h.day == 29 } }

    @Test
    fun `not asked before Maghrib on the 29th`() = assertNull(data(LocalDateTime.of(eve, LocalTime.NOON)).hijriCheck)

    @Test
    fun `asked after Maghrib on the 29th and through the next day`() {
        val evening = assertNotNull(data(LocalDateTime.of(eve, LocalTime.of(19, 30))).hijriCheck)
        assertEquals(8, evening.month)
        assertEquals(9, evening.nextMonth)
        assertTrue(evening.onEve)
        val nextDay = assertNotNull(data(LocalDateTime.of(eve.plusDays(1), LocalTime.of(9, 0))).hijriCheck)
        assertFalse(nextDay.onEve)
        assertNull(data(LocalDateTime.of(eve.plusDays(2), LocalTime.of(9, 0))).hijriCheck)
    }

    @Test
    fun `not asked again once answered`() {
        val check = assertNotNull(data(LocalDateTime.of(eve, LocalTime.of(19, 30))).hijriCheck)
        assertNull(data(LocalDateTime.of(eve, LocalTime.of(19, 30)), confirmed = check.key).hijriCheck)
    }

    @Test
    fun `answers move the calendar only when they differ from the calculation`() {
        val calcNew = HijriCheck("k", 8, 1448, calculatedNew = true, onEve = true)
        assertEquals(0, calcNew.delta(startsNext = true))
        assertEquals(-1, calcNew.delta(startsNext = false))
        val calc30 = HijriCheck("k", 8, 1448, calculatedNew = false, onEve = true)
        assertEquals(1, calc30.delta(startsNext = true))
        assertEquals(0, calc30.delta(startsNext = false))
    }

    @Test
    fun `other months are not asked`() {
        val rajab29 = generateSequence(LocalDate.of(2026, 12, 1)) { it.plusDays(1) }
            .first { HijriCalendar.of(it, 0).let { h -> h.month == 7 && h.day == 29 } }
        assertNull(data(LocalDateTime.of(rajab29, LocalTime.of(19, 30))).hijriCheck)
    }
}
