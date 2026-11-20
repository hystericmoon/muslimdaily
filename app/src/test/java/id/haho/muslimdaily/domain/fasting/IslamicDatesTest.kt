package id.haho.muslimdaily.domain.fasting

import id.haho.muslimdaily.domain.prayer.HijriCalendar
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslamicDatesTest {
    private val from = LocalDate.of(2026, 11, 20)

    @Test
    fun `every date it names falls on the Hijri month and day of its occasion`() {
        IslamicDates.upcoming(from, 0, limit = 12).forEach { d ->
            val h = HijriCalendar.of(d.date, 0)
            assertEquals(d.occasion.month, h.month)
            assertEquals(d.occasion.day, h.day)
            assertEquals(h, d.hijri)
        }
    }

    @Test
    fun `a year ahead holds each of the six dates once, soonest first`() {
        val next = IslamicDates.upcoming(from, 0, limit = 6)
        assertEquals(IslamicOccasion.entries.toSet(), next.map { it.occasion }.toSet())
        assertEquals(next.sortedBy { it.date }, next)
        assertEquals(6, next.map { it.date }.distinct().size)
    }

    @Test
    fun `an ordinary day names nothing, and the day itself counts as upcoming`() {
        val first = IslamicDates.upcoming(from, 0, limit = 1).single()
        assertEquals(first, IslamicDates.on(first.date, 0))
        assertEquals(first, IslamicDates.upcoming(first.date, 0, limit = 1).single())
        assertNull(IslamicDates.on(first.date.minusDays(1), 0)?.takeIf { it.occasion == first.occasion })
    }
}
