package id.haho.muslimdaily.domain.fasting

import id.haho.muslimdaily.domain.prayer.HijriDate
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FastingCalendarTest {
    private val wednesday = LocalDate.of(2026, 10, 21)
    private val monday = LocalDate.of(2026, 10, 19)

    @Test
    fun `eid and tashriq override everything`() {
        assertEquals(listOf(FastKind.EID_FITR), FastingCalendar.kinds(monday, HijriDate(1, 10, 1448)))
        assertEquals(listOf(FastKind.EID_ADHA), FastingCalendar.kinds(wednesday, HijriDate(10, 12, 1448)))
        // 13 Zulhijah is a white day by date but a tashriq day by rule.
        assertEquals(listOf(FastKind.TASHRIQ), FastingCalendar.kinds(monday, HijriDate(13, 12, 1448)))
    }

    @Test
    fun `Ramadan days are only Ramadan`() {
        assertEquals(listOf(FastKind.RAMADAN), FastingCalendar.kinds(monday, HijriDate(14, 9, 1448)))
    }

    @Test
    fun `sunnah days combine`() {
        assertEquals(listOf(FastKind.ASHURA, FastKind.MONDAY), FastingCalendar.kinds(monday, HijriDate(10, 1, 1449)))
        assertEquals(listOf(FastKind.ARAFAH), FastingCalendar.kinds(wednesday, HijriDate(9, 12, 1448)))
        assertEquals(listOf(FastKind.WHITE_DAYS), FastingCalendar.kinds(wednesday, HijriDate(14, 5, 1448)))
        assertEquals(listOf(FastKind.SHAWWAL), FastingCalendar.kinds(wednesday, HijriDate(2, 10, 1448)))
        assertEquals(emptyList(), FastingCalendar.kinds(wednesday, HijriDate(20, 5, 1448)))
    }

    @Test
    fun `a Hijri month is 29 or 30 consecutive days starting on the 1st`() {
        val m = FastingCalendar.month(wednesday, 0)
        assertTrue(m.size in 29..30)
        assertEquals(1, m.first().hijri.day)
        assertTrue(m.zipWithNext().all { (a, b) -> b.date == a.date.plusDays(1) && b.hijri.day == a.hijri.day + 1 })
        assertTrue(m.any { it.date == wednesday })
    }

    @Test
    fun `upcoming can skip the weekly fasts`() {
        val all = FastingCalendar.upcoming(wednesday, 0, includeWeekly = true, limit = 10)
        val special = FastingCalendar.upcoming(wednesday, 0, includeWeekly = false, limit = 10)
        assertEquals(10, all.size)
        assertTrue(special.all { d -> d.forbidden || d.kinds.any { !it.weekly } })
        assertTrue(all.first().date <= special.first().date)
    }
}
