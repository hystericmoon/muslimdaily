package id.haho.muslimdaily.domain.prayer

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class HijriCalendarTest {
    @Test
    fun `adjustment shifts by one day and is clamped`() {
        val date = LocalDate.of(2026, 10, 21)
        val base = HijriCalendar.of(date, 0)
        val plus = HijriCalendar.of(date, 1)
        assertEquals(HijriCalendar.of(date.plusDays(1), 0), plus)
        assertEquals(HijriCalendar.of(date, 5), plus)
        assertEquals(HijriCalendar.of(date.minusDays(1), 0), HijriCalendar.of(date, -1))
        assert(base.year > 1440)
    }
}
